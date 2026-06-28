package com.upi.npci.controller;

import com.upi.npci.dto.request.PaymentInitiateRequest;
import com.upi.npci.dto.response.PaymentInitiateResponse;
import com.upi.npci.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/switch")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate")
    public ResponseEntity<PaymentInitiateResponse> initiatePayment(@RequestBody @Valid PaymentInitiateRequest request) {
        log.info("Received transaction initiation request for ID: {}", request.getTransactionId());
        PaymentInitiateResponse response = paymentService.initiatePayment(request);
        return ResponseEntity.ok(response);
    }
}
