package com.upi.npci.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class BankReversalRequest {

    @JsonProperty("original_txn_id")
    private UUID originalTxnId;

    @JsonProperty("reversal_txn_id")
    private UUID reversalTxnId;

    @JsonProperty("account_vpa")
    private String accountVpa;

    @JsonProperty("amount_paise")
    private Long amountPaise;
}
