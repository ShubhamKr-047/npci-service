package com.upi.npci.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankResponse {

    @JsonProperty("status")
    private String status;

    @JsonProperty("rrn")
    private String rrn;

    @JsonProperty("failure_reason")
    private String failureReason;

    @JsonProperty("account_vpa")
    private String accountVpa;
}
