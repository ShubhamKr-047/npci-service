package com.upi.npci.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInitiateResponse {
    private UUID transactionId;
    private String status;
    private String payerRrn;
    private String payeeRrn;
    private String failureReason;
}
