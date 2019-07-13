package com.lmdamato.moneytransfer.model;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class MoneyTest {
    @Test
    public void testMoneySuccess() {
        new Money(new BigDecimal("12.34"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMoneyFailure_NegativeAmount() {
        new Money(new BigDecimal("-12.34"));
    }

    @Test
    public void testPlus() {
        final BigDecimal bd1 = new BigDecimal("12.34");
        final BigDecimal bd2 = new BigDecimal("7.33");

        final Money m1 = new Money(bd1);
        final Money m2 = new Money(bd2);

        assertEquals(new Money(bd1.add(bd2)), m1.plus(m2));
    }

    @Test
    public void testMinusSuccess() {
        final BigDecimal bd1 = new BigDecimal("12.34");
        final BigDecimal bd2 = new BigDecimal("7.33");

        final Money m1 = new Money(bd1);
        final Money m2 = new Money(bd2);

        assertEquals(new Money(bd1.subtract(bd2)), m1.minus(m2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMinusFailure_NegativeResult() {
        final BigDecimal bd1 = new BigDecimal("12.34");
        final BigDecimal bd2 = new BigDecimal("27.33");

        final Money m1 = new Money(bd1);
        final Money m2 = new Money(bd2);

        m1.minus(m2);
    }
}
