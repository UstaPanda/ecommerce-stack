package com.ecommerce.main.crypto;

import lombok.Getter;

/**
 * Result of verifying a crypto payment transaction on the blockchain.
 */
@Getter
public class CryptoVerificationResult {

    private final boolean success;
    private final String message;
    private final String txHash;
    private final String amountPaid;   // e.g. "0.0025 ETH"
    private final String usdEquivalent; // e.g. "$8.50"

    private CryptoVerificationResult(
            boolean success, String message,
            String txHash, String amountPaid, String usdEquivalent) {
        this.success = success;
        this.message = message;
        this.txHash = txHash;
        this.amountPaid = amountPaid;
        this.usdEquivalent = usdEquivalent;
    }

    public static CryptoVerificationResult success(String txHash, String amountPaid, String usdEquivalent) {
        return new CryptoVerificationResult(true, "Ödeme doğrulandı", txHash, amountPaid, usdEquivalent);
    }

    public static CryptoVerificationResult failure(String message) {
        return new CryptoVerificationResult(false, message, null, null, null);
    }
}
