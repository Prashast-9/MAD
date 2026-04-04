package com.example.currencyconvertor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Static exchange rates: units of each currency per 1 USD (e.g. 1 USD = 83 INR).
 */
public final class ExchangeRates {

    /**
     * Maps ISO-style currency code to how many units of that currency equal one US dollar.
     */
    private static final Map<String, Double> UNITS_PER_USD;

    static {
        Map<String, Double> m = new HashMap<>();
        m.put("USD", 1.0);
        m.put("INR", 83.0);
        m.put("EUR", 0.92);
        m.put("JPY", 150.0);
        UNITS_PER_USD = Collections.unmodifiableMap(m);
    }

    private ExchangeRates() {
    }

    static String normalizeCode(String currencyCode) {
        if (currencyCode == null) {
            return "";
        }
        return currencyCode.trim().toUpperCase(Locale.US);
    }

    static double unitsPerUsd(String normalizedCode) {
        Double rate = UNITS_PER_USD.get(normalizedCode);
        if (rate == null) {
            throw new IllegalArgumentException("Unknown currency: " + normalizedCode);
        }
        return rate;
    }

    static boolean isSupported(String currencyCode) {
        String code = normalizeCode(currencyCode);
        return !code.isEmpty() && UNITS_PER_USD.containsKey(code);
    }
}
