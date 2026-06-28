package com.upi.npci.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Slf4j
@Service
public class HmacSigningService {

    private final ObjectMapper objectMapper;
    private final String hmacSecret;

    public HmacSigningService(ObjectMapper objectMapper, @Value("${hmac.secret}") String hmacSecret) {
        this.objectMapper = objectMapper;
        this.hmacSecret = hmacSecret;
    }

    public String generateSignature(UUID transactionId, String accountVpa, Long amountPaise, String upiPinHash) {
        try {
            // Sort keys alphabetically using TreeMap to match HmacAuthFilter requirements
            Map<String, Object> payloadMap = new TreeMap<>();
            payloadMap.put("transaction_id", transactionId.toString());
            payloadMap.put("account_vpa", accountVpa);
            payloadMap.put("amount_paise", amountPaise);
            if (upiPinHash != null) {
                payloadMap.put("upi_pin_hash", upiPinHash);
            }

            // Serialize to JSON string
            String payloadJson = objectMapper.writeValueAsString(payloadMap);
            log.info("Payload to sign: {}", payloadJson);

            // Compute HMAC-SHA256
            return calculateHmac(payloadJson);
        } catch (Exception e) {
            log.error("Error generating HMAC signature", e);
            throw new RuntimeException("HMAC generation failed", e);
        }
    }

    private String calculateHmac(String data) throws Exception {
        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256Hmac.init(secretKeySpec);
        byte[] hmacBytes = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hmacBytes);
    }
}
