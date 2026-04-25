package com.ecommerce.main.crypto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * Verifies crypto payment transactions on-chain via public JSON-RPC endpoints.
 * No external library needed — plain HTTP calls to the blockchain node.
 */
@Service
public class BlockchainVerificationService {

    @Value("${crypto.merchant.wallet}")
    private String merchantWallet;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Free public RPC endpoints (no API key required)
    private static final Map<Integer, String> RPC_URLS = Map.of(
        1,        "https://ethereum-rpc.publicnode.com",
        137,      "https://polygon-rpc.com",
        56,       "https://bsc-dataseed.binance.org",
        43114,    "https://api.avax.network/ext/bc/C/rpc",
        42161,    "https://arb1.arbitrum.io/rpc",
        10,       "https://mainnet.optimism.io",
        8453,     "https://mainnet.base.org",
        11155111, "https://ethereum-sepolia-rpc.publicnode.com"  // Sepolia testnet
    );

    // CoinGecko coin IDs for each chain's native token
    private static final Map<Integer, String> COINGECKO_IDS = Map.of(
        1,        "ethereum",
        137,      "matic-network",
        56,       "binancecoin",
        43114,    "avalanche-2",
        42161,    "ethereum",
        10,       "ethereum",
        8453,     "ethereum",
        11155111, "ethereum"
    );

    private static final Map<Integer, String> NATIVE_SYMBOLS = Map.of(
        1,        "ETH",
        137,      "MATIC",
        56,       "BNB",
        43114,    "AVAX",
        42161,    "ETH",
        10,       "ETH",
        8453,     "ETH",
        11155111, "ETH"
    );

    /**
     * Verifies that {@code txHash} on {@code chainId}:
     * <ol>
     *   <li>Is mined and successful (status = 0x1)</li>
     *   <li>Is sent to the merchant wallet</li>
     *   <li>Carries at least {@code expectedUsdAmount} USD worth of native token (2% tolerance)</li>
     * </ol>
     */
    public CryptoVerificationResult verify(String txHash, int chainId, double expectedUsdAmount) {
        String rpcUrl = RPC_URLS.get(chainId);
        if (rpcUrl == null) {
            return CryptoVerificationResult.failure("Desteklenmeyen zincir ID: " + chainId);
        }

        try {
            // 1. Transaction receipt — proves the tx is mined and not reverted
            JsonNode receipt = rpcCall(rpcUrl, "eth_getTransactionReceipt", List.of(txHash));
            if (receipt == null || receipt.isNull()) {
                return CryptoVerificationResult.failure(
                    "İşlem henüz onaylanmamış veya zincirde bulunamadı. Lütfen biraz bekleyin.");
            }
            String status = receipt.path("status").asText("0x0");
            if (!"0x1".equals(status)) {
                return CryptoVerificationResult.failure("Blockchain işlemi başarısız (reverted).");
            }

            // 2. Transaction details — verify to-address and value
            JsonNode tx = rpcCall(rpcUrl, "eth_getTransactionByHash", List.of(txHash));
            if (tx == null || tx.isNull()) {
                return CryptoVerificationResult.failure("İşlem detayları alınamadı.");
            }

            String toAddress = tx.path("to").asText("");
            if (!merchantWallet.equalsIgnoreCase(toAddress)) {
                return CryptoVerificationResult.failure(
                    "Ödeme yanlış adrese gönderilmiş. Beklenen: " + merchantWallet);
            }

            // 3. Parse value (hex wei → ETH)
            String valueHex = tx.path("value").asText("0x0");
            BigInteger weiAmount = new BigInteger(valueHex.replaceFirst("^0x", ""), 16);
            BigDecimal ethAmount = new BigDecimal(weiAmount)
                .divide(BigDecimal.TEN.pow(18), 18, RoundingMode.HALF_UP);

            String symbol = NATIVE_SYMBOLS.getOrDefault(chainId, "TOKEN");

            // Sepolia testnet: skip USD value check (test money has no value)
            if (chainId == 11155111) {
                return CryptoVerificationResult.success(
                    txHash,
                    ethAmount.stripTrailingZeros().toPlainString() + " " + symbol + " (Testnet)",
                    "Testnet - fiyat kontrolü yok");
            }

            // 4. Get native token price from CoinGecko (free, no API key)
            double tokenPriceUsd = fetchTokenPriceUsd(chainId);
            if (tokenPriceUsd <= 0) {
                return CryptoVerificationResult.failure(
                    "Token fiyatı alınamadı (CoinGecko). Lütfen tekrar deneyin.");
            }

            double paidUsd = ethAmount.doubleValue() * tokenPriceUsd;

            // Allow 2% price-fluctuation tolerance
            if (paidUsd < expectedUsdAmount * 0.98) {
                return CryptoVerificationResult.failure(
                    String.format("Yetersiz ödeme: %.6f %s (≈$%.2f), gereken: $%.2f",
                        ethAmount.doubleValue(), symbol, paidUsd, expectedUsdAmount));
            }

            return CryptoVerificationResult.success(
                txHash,
                ethAmount.stripTrailingZeros().toPlainString() + " " + symbol,
                String.format("$%.2f", paidUsd));

        } catch (Exception e) {
            return CryptoVerificationResult.failure("Doğrulama hatası: " + e.getMessage());
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private JsonNode rpcCall(String rpcUrl, String method, List<Object> params) throws Exception {
        Map<String, Object> payload = Map.of(
            "jsonrpc", "2.0",
            "method",  method,
            "params",  params,
            "id",      1
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(rpcUrl, entity, String.class);
        JsonNode root = objectMapper.readTree(response.getBody());
        return root.path("result");
    }

    private double fetchTokenPriceUsd(int chainId) {
        try {
            String coinId = COINGECKO_IDS.getOrDefault(chainId, "ethereum");
            String url = "https://api.coingecko.com/api/v3/simple/price"
                       + "?ids=" + coinId + "&vs_currencies=usd";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path(coinId).path("usd").asDouble(0);
        } catch (Exception e) {
            return 0;
        }
    }
}
