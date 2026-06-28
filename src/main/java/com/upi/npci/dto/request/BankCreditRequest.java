package com.upi.npci.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class BankCreditRequest {

    @JsonProperty("transaction_id")
    private UUID transactionId;

    @JsonProperty("account_vpa")
    private String accountVpa;

    @JsonProperty("amount_paise")
    private Long amountPaise;

    @JsonProperty("hmac_signature")
    private String hmacSignature;
}
