package com.upi.npci.exception;

public class VpaNotFoundException extends RuntimeException {
    public VpaNotFoundException(String vpa) {
        super("VPA not found in registry: " + vpa);
    }
}
