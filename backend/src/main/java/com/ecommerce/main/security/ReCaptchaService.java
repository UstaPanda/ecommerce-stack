package com.ecommerce.main.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class ReCaptchaService {

    @Value("${recaptcha.secret-key}")
    private String secretKey;

    @Value("${recaptcha.enabled:true}")
    private boolean enabled;

    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    private final RestTemplate restTemplate = new RestTemplate();

    private static final double MIN_SCORE = 0.5;

    public void verify(String token) {
        if (!enabled) return;

        String url = VERIFY_URL + "?secret=" + secretKey + "&response=" + token;

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(url, null, Map.class);

        if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
            throw new IllegalArgumentException("reCAPTCHA dogrulamasi basarisiz");
        }

        // Skor kontrolü (Sadece v3 için geçerlidir, v2'de null döner)
        Object scoreObj = response.get("score");
        if (scoreObj != null) {
            double score = ((Number) scoreObj).doubleValue();
            if (score < MIN_SCORE) {
                throw new IllegalArgumentException("reCAPTCHA skoru yetersiz (bot aktivitesi tespit edildi)");
            }
        }
    }
}
