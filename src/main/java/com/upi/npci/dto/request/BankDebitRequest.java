package com.upi.npci.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class BankDebitRequest {

    @JsonProperty("transaction_id")
    private UUID transactionId;

    @JsonProperty("account_vpa")
    private String accountVpa;

    @JsonProperty("amount_paise")
    private Long amountPaise;

    @JsonProperty("upi_pin_hash")
    private String upiPinHash;

    @JsonProperty("hmac_signature")
    private String hmacSignature;
}
