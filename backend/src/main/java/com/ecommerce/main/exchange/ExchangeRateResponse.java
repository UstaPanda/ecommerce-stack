package com.ecommerce.main.exchange;

import java.util.Map;

public record ExchangeRateResponse(
        Map<String, Double> rates,
        String lastUpdated,
        String rateDate
) {}
