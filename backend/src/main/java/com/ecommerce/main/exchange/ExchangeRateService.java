package com.ecommerce.main.exchange;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ExchangeRateService {

    private static final String TCMB_URL = "https://www.tcmb.gov.tr/kurlar/today.xml";

    // TRY per 1 unit of each currency (e.g., USD -> 38.12 means 1 USD = 38.12 TRY)
    private final Map<String, Double> tryRates = new ConcurrentHashMap<>();
    private String lastUpdated = "";
    private String rateDate = "";

    @PostConstruct
    @Scheduled(fixedRate = 1_800_000) // her 30 dakikada bir
    public void fetchRates() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String xml = restTemplate.getForObject(TCMB_URL, String.class);
            parseXml(xml);
            log.info("TCMB exchange rates updated at {}", lastUpdated);
        } catch (Exception e) {
            log.warn("TCMB rate fetch failed: {}", e.getMessage());
            if (tryRates.isEmpty()) {
                // fallback rates
                tryRates.put("USD", 38.50);
                tryRates.put("EUR", 42.50);
                tryRates.put("GBP", 49.20);
                tryRates.put("JPY", 0.255);
                tryRates.put("CHF", 43.10);
                tryRates.put("CAD", 27.80);
                tryRates.put("AUD", 24.50);
                tryRates.put("SAR", 10.25);
                tryRates.put("AED", 10.48);
            }
        }
    }

    private void parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));

        // tarih bilgisi
        Element root = doc.getDocumentElement();
        rateDate = root.getAttribute("Date");

        NodeList currencies = doc.getElementsByTagName("Currency");
        Map<String, Double> updated = new HashMap<>();

        for (int i = 0; i < currencies.getLength(); i++) {
            Element el = (Element) currencies.item(i);
            String code = el.getAttribute("CurrencyCode");
            String sellingStr = getTagText(el, "ForexSelling");
            String unitStr = getTagText(el, "Unit");

            if (sellingStr == null || sellingStr.isBlank()) continue;

            try {
                double selling = Double.parseDouble(sellingStr);
                int unit = unitStr != null ? Integer.parseInt(unitStr.trim()) : 1;
                updated.put(code, selling / unit); // TRY per 1 unit
            } catch (NumberFormatException ignored) {}
        }

        if (!updated.isEmpty()) {
            tryRates.clear();
            tryRates.putAll(updated);
            lastUpdated = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        }
    }

    private String getTagText(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() == 0) return null;
        return list.item(0).getTextContent();
    }

    /**
     * USD bazlı conversion rate döner.
     * Örnek: rates["TRY"] = 38.50 → 1 USD = 38.50 TRY
     *        rates["EUR"] = 0.892 → 1 USD = 0.892 EUR
     */
    public ExchangeRateResponse getRatesFromUsd() {
        // tryRates: "1 birim yabancı para = X TRY"
        // usdInTry: 1 USD kaç TRY
        double usdInTry = tryRates.getOrDefault("USD", 38.50);

        Map<String, Double> fromUsd = new HashMap<>();
        fromUsd.put("USD", 1.0);
        // TRY: TCMB XML'inde base currency olarak yer almaz, elle ekliyoruz
        // 1 USD = usdInTry TRY
        fromUsd.put("TRY", usdInTry);

        for (Map.Entry<String, Double> e : tryRates.entrySet()) {
            String code = e.getKey();
            double xInTry = e.getValue(); // 1 CODE = xInTry TRY
            if (xInTry > 0 && !code.equals("USD")) {
                // 1 USD = usdInTry TRY → 1 CODE = xInTry TRY → 1 USD = usdInTry/xInTry CODE
                fromUsd.put(code, usdInTry / xInTry);
            }
        }

        return new ExchangeRateResponse(fromUsd, lastUpdated, rateDate);
    }
}
