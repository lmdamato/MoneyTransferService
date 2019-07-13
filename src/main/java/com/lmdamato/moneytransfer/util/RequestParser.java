package com.lmdamato.moneytransfer.util;

import com.lmdamato.moneytransfer.model.Money;
import com.lmdamato.moneytransfer.model.User;
import io.undertow.server.HttpServerExchange;
import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.math.BigDecimal;
import java.util.Deque;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RequestParser {
    public static Option<User> user(@NonNull final HttpServerExchange exchange) {
        return pathParam(exchange, "userId")
            .map(User::new);
    }

    public static Option<Money> amount(@NonNull final HttpServerExchange exchange) {
        return pathParam(exchange, "amount")
            .map(BigDecimal::new)
            .map(Money::new);
    }

    public static Option<String> pathParam(
        @NonNull final HttpServerExchange exchange,
        @NonNull final String name
    ) {
        return Option
            .of(exchange.getQueryParameters().get(name))
            .map(Deque::getFirst);
    }
}
