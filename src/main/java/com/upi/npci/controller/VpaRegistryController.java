package com.upi.npci.controller;

import com.upi.npci.dto.request.VpaRegisterRequest;
import com.upi.npci.dto.response.VpaRegisterResponse;
import com.upi.npci.service.VpaRegistryService;
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
@RequestMapping("/switch/vpa")
@RequiredArgsConstructor
public class VpaRegistryController {

    private final VpaRegistryService vpaRegistryService;

    @PostMapping("/register")
    public ResponseEntity<VpaRegisterResponse> registerVpa(@RequestBody @Valid VpaRegisterRequest request) {
        log.info("Received VPA registration request: {}", request.getVpa());
        VpaRegisterResponse response = vpaRegistryService.registerVpa(request);
        return ResponseEntity.ok(response);
    }
}
