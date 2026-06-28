package com.upi.npci.service;

import com.upi.npci.dto.request.BankCreditRequest;
import com.upi.npci.dto.request.BankDebitRequest;
import com.upi.npci.dto.request.BankReversalRequest;
import com.upi.npci.dto.request.PaymentInitiateRequest;
import com.upi.npci.dto.response.BankResponse;
import com.upi.npci.dto.response.PaymentInitiateResponse;
import com.upi.npci.entity.Transaction;
import com.upi.npci.entity.TransactionStatus;
import com.upi.npci.entity.VpaRegistry;
import com.upi.npci.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final VpaRegistryService vpaRegistryService;
    private final HmacSigningService hmacSigningService;
    private final WebClient webClient;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public PaymentInitiateResponse initiatePayment(PaymentInitiateRequest request) {
        log.info("Initiating payment transaction ID: {} from {} to {} of amount {} paise",
                request.getTransactionId(), request.getPayerVpa(), request.getPayeeVpa(), request.getAmountPaise());

        // Step 1: Idempotency Check
        Optional<Transaction> existingOpt = transactionRepository.findById(request.getTransactionId());
        if (existingOpt.isPresent()) {
            Transaction existing = existingOpt.get();
            log.info("Duplicate payment transaction detected for ID: {}. Returning current status: {}",
                    request.getTransactionId(), existing.getStatus());
            return PaymentInitiateResponse.builder()
                    .transactionId(existing.getTransactionId())
                    .status(existing.getStatus().name())
                    .payerRrn(existing.getPayerRrn())
                    .payeeRrn(existing.getPayeeRrn())
                    .failureReason(existing.getFailureReason())
                    .build();
        }

        // Step 2: Resolve VPAs to get Bank routing URLs
        VpaRegistry payerRegistry = vpaRegistryService.resolveVpa(request.getPayerVpa());
        VpaRegistry payeeRegistry = vpaRegistryService.resolveVpa(request.getPayeeVpa());

        // Step 3: Save transaction as INITIATED
        Transaction transaction = Transaction.builder()
                .transactionId(request.getTransactionId())
                .payerVpa(request.getPayerVpa())
                .payeeVpa(request.getPayeeVpa())
                .amountPaise(request.getAmountPaise())
                .status(TransactionStatus.INITIATED)
                .build();
        transaction = transactionRepository.save(transaction);

        // Step 4: Step A - Call Payer Bank Debit
        BankResponse debitResponse = executeDebitCall(payerRegistry.getBankApiUrl(), request);
        if (!"SUCCESS".equalsIgnoreCase(debitResponse.getStatus())) {
            log.warn("Debit failed for transaction ID: {}. Reason: {}", request.getTransactionId(), debitResponse.getFailureReason());
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(debitResponse.getFailureReason());
            transactionRepository.save(transaction);
            return PaymentInitiateResponse.builder()
                    .transactionId(request.getTransactionId())
                    .status(TransactionStatus.FAILED.name())
                    .failureReason(debitResponse.getFailureReason())
                    .build();
        }

        // Debit was successful
        log.info("Debit successful for transaction ID: {}. Payer RRN: {}", request.getTransactionId(), debitResponse.getRrn());
        transaction.setStatus(TransactionStatus.DEBIT_SUCCESS);
        transaction.setPayerRrn(debitResponse.getRrn());
        transaction = transactionRepository.save(transaction);

        // Step 5: Step B - Call Payee Bank Credit
        BankResponse creditResponse = executeCreditCall(payeeRegistry.getBankApiUrl(), request);
        if ("SUCCESS".equalsIgnoreCase(creditResponse.getStatus())) {
            log.info("Credit successful for transaction ID: {}. Payee RRN: {}", request.getTransactionId(), creditResponse.getRrn());
            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setPayeeRrn(creditResponse.getRrn());
            transactionRepository.save(transaction);
            return PaymentInitiateResponse.builder()
                    .transactionId(request.getTransactionId())
                    .status(TransactionStatus.SUCCESS.name())
                    .payerRrn(transaction.getPayerRrn())
                    .payeeRrn(creditResponse.getRrn())
                    .build();
        }

        // Credit failed or timed out. Trigger Auto-Reversal
        log.warn("Credit failed/timed out for transaction ID: {}. Triggering auto-reversal. Reason: {}",
                request.getTransactionId(), creditResponse.getFailureReason());
        
        transaction.setStatus(TransactionStatus.CREDIT_PENDING);
        transaction.setFailureReason(creditResponse.getFailureReason());
        transaction = transactionRepository.save(transaction);

        executeReversalCall(payerRegistry.getBankApiUrl(), transaction);

        return PaymentInitiateResponse.builder()
                .transactionId(request.getTransactionId())
                .status(transaction.getStatus().name())
                .payerRrn(transaction.getPayerRrn())
                .failureReason(transaction.getFailureReason())
                .build();
    }

    private BankResponse executeDebitCall(String bankApiUrl, PaymentInitiateRequest request) {
        String hmacSignature = hmacSigningService.generateSignature(
                request.getTransactionId(), request.getPayerVpa(), request.getAmountPaise(), request.getUpiPinHash());

        BankDebitRequest debitRequest = BankDebitRequest.builder()
                .transactionId(request.getTransactionId())
                .accountVpa(request.getPayerVpa())
                .amountPaise(request.getAmountPaise())
                .upiPinHash(request.getUpiPinHash())
                .hmacSignature(hmacSignature)
                .build();

        log.info("Sending debit request to bank: {}/bank/debit", bankApiUrl);
        return callBankEndpoint(bankApiUrl + "/bank/debit", debitRequest);
    }

    private BankResponse executeCreditCall(String bankApiUrl, PaymentInitiateRequest request) {
        String hmacSignature = hmacSigningService.generateSignature(
                request.getTransactionId(), request.getPayeeVpa(), request.getAmountPaise(), null);

        BankCreditRequest creditRequest = BankCreditRequest.builder()
                .transactionId(request.getTransactionId())
                .accountVpa(request.getPayeeVpa())
                .amountPaise(request.getAmountPaise())
                .hmacSignature(hmacSignature)
                .build();

        log.info("Sending credit request to bank: {}/bank/credit", bankApiUrl);
        // Timeout is set to 3 seconds. Simulated timeouts in bank service sleep for 6 seconds.
        return callBankEndpointWithTimeout(bankApiUrl + "/bank/credit", creditRequest, Duration.ofSeconds(3));
    }

    private void executeReversalCall(String bankApiUrl, Transaction transaction) {
        UUID reversalTxnId = UUID.randomUUID();
        BankReversalRequest reversalRequest = BankReversalRequest.builder()
                .originalTxnId(transaction.getTransactionId())
                .reversalTxnId(reversalTxnId)
                .accountVpa(transaction.getPayerVpa())
                .amountPaise(transaction.getAmountPaise())
                .build();

        log.info("Sending reversal request to bank: {}/bank/reversal, Reversal ID: {}", bankApiUrl, reversalTxnId);
        try {
            BankResponse response = webClient.post()
                    .uri(bankApiUrl + "/bank/reversal")
                    .bodyValue(reversalRequest)
                    .retrieve()
                    .bodyToMono(BankResponse.class)
                    .block();

            if (response != null && "SUCCESS".equalsIgnoreCase(response.getStatus())) {
                log.info("Reversal successful for original transaction ID: {}. Refund RRN: {}",
                        transaction.getTransactionId(), response.getRrn());
                transaction.setStatus(TransactionStatus.REVERSED);
                transactionRepository.save(transaction);
            } else {
                log.error("Reversal failed or returned non-success for original transaction ID: {}", transaction.getTransactionId());
            }
        } catch (Exception e) {
            log.error("Failed to execute reversal call for transaction ID: {}", transaction.getTransactionId(), e);
        }
    }

    private BankResponse callBankEndpoint(String url, Object requestBody) {
        return callBankEndpointWithTimeout(url, requestBody, Duration.ofSeconds(10));
    }

    private BankResponse callBankEndpointWithTimeout(String url, Object requestBody, Duration timeout) {
        try {
            return webClient.post()
                    .uri(url)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(BankResponse.class)
                    .timeout(timeout)
                    .block();
        } catch (WebClientResponseException ex) {
            log.warn("Bank HTTP endpoint returned error code: {}. Body: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            try {
                BankResponse response = ex.getResponseBodyAs(BankResponse.class);
                if (response != null) {
                    return response;
                }
            } catch (Exception parseEx) {
                log.error("Error parsing bank error response body", parseEx);
            }
            return BankResponse.builder()
                    .status("FAILURE")
                    .failureReason(ex.getStatusCode().toString())
                    .build();
        } catch (Exception ex) {
            log.error("Exception calling bank endpoint: {}", url, ex);
            String reason = "GATEWAY_TIMEOUT";
            if (ex instanceof java.util.concurrent.TimeoutException || ex.getCause() instanceof java.util.concurrent.TimeoutException) {
                reason = "TIMEOUT";
            }
            return BankResponse.builder()
                    .status("FAILURE")
                    .failureReason(reason)
                    .build();
        }
    }
}
