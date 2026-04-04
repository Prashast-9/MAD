package com.example.currencyconvertor;

/**
 * Converts amounts between supported currencies using USD as the pivot (no network).
 */
public final class CurrencyConverter {

    private CurrencyConverter() {
    }

    /**
     * Converts {@code amount} from {@code fromCurrency} to {@code toCurrency}.
     * <p>
     * Rates are defined as units per USD: first convert to USD, then to the target.
     * </p>
     *
     * @param amount         non-negative amount in the source currency
     * @param fromCurrency   source currency code (e.g. INR)
     * @param toCurrency     target currency code (e.g. USD)
     * @return converted amount in the target currency
     * @throws IllegalArgumentException if a currency is unknown or amount is negative
     */
    public static double convert(double amount, String fromCurrency, String toCurrency) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        String from = ExchangeRates.normalizeCode(fromCurrency);
        String to = ExchangeRates.normalizeCode(toCurrency);
        if (from.isEmpty() || to.isEmpty()) {
            throw new IllegalArgumentException("Currency code cannot be empty");
        }
        if (!ExchangeRates.isSupported(from) || !ExchangeRates.isSupported(to)) {
            throw new IllegalArgumentException("Unsupported currency");
        }
        if (from.equals(to)) {
            return amount;
        }
        double rateFrom = ExchangeRates.unitsPerUsd(from);
        double rateTo = ExchangeRates.unitsPerUsd(to);
        double amountInUsd = amount / rateFrom;
        return amountInUsd * rateTo;
    }
}
