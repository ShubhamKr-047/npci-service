package com.upi.npci.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VpaRegisterRequest {

    @NotBlank(message = "vpa is required")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+$", message = "Invalid VPA format")
    private String vpa;

    @NotBlank(message = "bank_code is required")
    private String bankCode;

    @NotBlank(message = "bank_api_url is required")
    private String bankApiUrl;

    @NotBlank(message = "account_number is required")
    private String accountNumber;
}
