package com.dealchain.dealchain.domain.security;

public class ContractEncryptionException extends RuntimeException {
    public ContractEncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}