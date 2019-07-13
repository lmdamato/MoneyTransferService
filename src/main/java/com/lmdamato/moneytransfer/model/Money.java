package com.lmdamato.moneytransfer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;

@EqualsAndHashCode
@ToString
public class Money implements Comparable<Money> {
    public static final Money ZERO = new Money(BigDecimal.ZERO);

    @JsonProperty("amount")
    @NonNull
    private final BigDecimal amount;

    public Money(@NonNull final BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be non-negative");
        }

        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public Money plus(@NonNull final Money other) {
        return new Money(this.amount.add(other.amount));
    }

    public Money minus(@NonNull final Money other) {
        return new Money(this.amount.subtract(other.amount));
    }

    @Override
    public int compareTo(@NonNull final Money o) {
        return this.amount.compareTo(o.amount);
    }
}
