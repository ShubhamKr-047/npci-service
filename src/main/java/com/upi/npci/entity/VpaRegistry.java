package com.upi.npci.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "vpa_registry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VpaRegistry {

    @Id
    @Column(name = "vpa", nullable = false, length = 100)
    private String vpa;

    @Column(name = "bank_code", nullable = false, length = 20)
    private String bankCode;

    @Column(name = "bank_api_url", nullable = false, length = 255)
    private String bankApiUrl;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;
}
