package com.upi.npci.service;

import com.upi.npci.dto.request.VpaRegisterRequest;
import com.upi.npci.dto.response.VpaRegisterResponse;
import com.upi.npci.entity.VpaRegistry;
import com.upi.npci.exception.VpaNotFoundException;
import com.upi.npci.repository.VpaRegistryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VpaRegistryService {

    private final VpaRegistryRepository vpaRegistryRepository;

    @Transactional
    public VpaRegisterResponse registerVpa(VpaRegisterRequest request) {
        log.info("Registering VPA: {} for bank: {}, URL: {}", 
                request.getVpa(), request.getBankCode(), request.getBankApiUrl());

        VpaRegistry registry = VpaRegistry.builder()
                .vpa(request.getVpa().toLowerCase())
                .bankCode(request.getBankCode().toUpperCase())
                .bankApiUrl(request.getBankApiUrl())
                .accountNumber(request.getAccountNumber())
                .build();

        vpaRegistryRepository.save(registry);

        return VpaRegisterResponse.builder()
                .status("SUCCESS")
                .message("VPA registered successfully")
                .build();
    }

    @Transactional(readOnly = true)
    public VpaRegistry resolveVpa(String vpa) {
        log.info("Resolving routing info for VPA: {}", vpa);
        return vpaRegistryRepository.findById(vpa.toLowerCase())
                .orElseThrow(() -> new VpaNotFoundException(vpa));
    }
}
