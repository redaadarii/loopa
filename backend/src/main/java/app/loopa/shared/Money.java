package app.loopa.shared;

import java.util.Currency;
import java.util.Objects;

/**
 * Montant en unités mineures (centimes) + devise ISO 4217. Jamais de double pour l'argent.
 */
public record Money(long amountMinor, Currency currency) {

    public Money {
        Objects.requireNonNull(currency, "currency");
    }

    public static Money of(long amountMinor, String currencyCode) {
        return new Money(amountMinor, Currency.getInstance(currencyCode));
    }

    public static Money zero(String currencyCode) {
        return of(0, currencyCode);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(amountMinor + other.amountMinor, currency);
    }

    public Money times(int quantity) {
        return new Money(amountMinor * quantity, currency);
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Devises différentes : " + currency + " / " + other.currency);
        }
    }
}
