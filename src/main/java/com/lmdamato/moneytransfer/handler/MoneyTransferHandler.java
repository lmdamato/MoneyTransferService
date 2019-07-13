package com.lmdamato.moneytransfer.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmdamato.moneytransfer.exception.UserAlreadyExistsException;
import com.lmdamato.moneytransfer.model.Money;
import com.lmdamato.moneytransfer.storage.InMemoryMoneyTransferDao;
import com.lmdamato.moneytransfer.util.RequestParser;
import com.lmdamato.moneytransfer.exception.InsufficientFundsException;
import com.lmdamato.moneytransfer.exception.UserDoesNotExistException;
import com.lmdamato.moneytransfer.model.User;
import com.lmdamato.moneytransfer.storage.MoneyTransferDao;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.NonNull;

import javax.ws.rs.core.MediaType;
import java.nio.ByteBuffer;

public class MoneyTransferHandler {
    @NonNull
    private static final MoneyTransferDao dao = InMemoryMoneyTransferDao.getInstance();

    @NonNull
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void createUserHandler(@NonNull final HttpServerExchange exchange) {
        final Option<User> user = RequestParser.user(exchange);

        if (user.isDefined()) {
            dao.createUser(user.get())
                .map(u -> {
                    exchange.setStatusCode(StatusCodes.CREATED);
                    return null;
                })
                .getOrElseGet(error -> {
                    if (error instanceof UserAlreadyExistsException) {
                        exchange.setStatusCode(StatusCodes.NO_CONTENT);
                    } else {
                        exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
                        sendJson(exchange, "Unknown error");
                    }
                    return null;
                });
        } else {
            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
        }

        exchange.endExchange();
    }

    public static void getBalanceHandler(@NonNull final HttpServerExchange exchange) {
        final Option<User> user = RequestParser.user(exchange);

        if (user.isDefined()) {
            dao.getBalance(user.get())
                .map(m -> {
                    exchange.setStatusCode(StatusCodes.OK);
                    sendJson(exchange, m);
                    return null;
                })
                .getOrElseGet(error -> {
                    if (error instanceof UserDoesNotExistException) {
                        exchange.setStatusCode(StatusCodes.NOT_FOUND);
                        sendJson(exchange, "User not found");
                    }

                    return null;
                });
        } else {
            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
        }

        exchange.endExchange();
    }

    public static void depositHandler(@NonNull final HttpServerExchange exchange) {
        final Option<User> user = RequestParser.user(exchange);
        final Option<Money> amount = RequestParser.amount(exchange);

        if (user.isDefined() && amount.isDefined()) {
            dao.deposit(user.get(), amount.get())
                .map(ignore -> {
                    exchange.setStatusCode(StatusCodes.NO_CONTENT);
                    return null;
                })
                .getOrElseGet(error -> {
                    if (error instanceof UserDoesNotExistException) {
                        exchange.setStatusCode(StatusCodes.NOT_FOUND);
                        sendJson(exchange, "User not found");
                    } else {
                        exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
                        sendJson(exchange, "Unknown error");
                    }

                    return null;
                });
        } else {
            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
        }

        exchange.endExchange();
    }

    public static void withdrawHandler(@NonNull final HttpServerExchange exchange) {
        final Option<User> user = RequestParser.user(exchange);
        final Option<Money> amount = RequestParser.amount(exchange);

        if (user.isDefined() && amount.isDefined()) {
            dao.withdraw(user.get(), amount.get())
                .map(ignore -> {
                    exchange.setStatusCode(StatusCodes.NO_CONTENT);
                    return null;
                })
                .getOrElseGet(error -> {
                    mapError(error, exchange);
                    return null;
                });
        }  else {
            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
        }

        exchange.endExchange();
    }

    public static void transferHandler(@NonNull final HttpServerExchange exchange) {
        final Option<User> from = RequestParser.pathParam(exchange, "from").map(User::new);
        final Option<User> to = RequestParser.pathParam(exchange, "to").map(User::new);

        final Option<Money> amount = RequestParser.amount(exchange);

        if (from.isDefined() && to.isDefined() && amount.isDefined()) {
            dao.transfer(from.get(), to.get(), amount.get())
                .map(ignore -> {
                    exchange.setStatusCode(StatusCodes.NO_CONTENT);
                    return null;
                })
                .getOrElseGet(error -> {
                    mapError(error, exchange);
                    return null;
                });
        } else {
            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
        }

        exchange.endExchange();
    }

    private static Try<Void> sendJson(@NonNull final HttpServerExchange exchange, @NonNull final Object obj) {
        return Try.run(() -> {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            exchange.getResponseSender().send(
                ByteBuffer.wrap(mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(obj))
            );
        });
    }

    private static void mapError(@NonNull final Throwable e, @NonNull final HttpServerExchange exchange) {
        if (e instanceof UserDoesNotExistException) {
            exchange.setStatusCode(StatusCodes.NOT_FOUND);
            sendJson(exchange, "User not found");
        } else if (e instanceof InsufficientFundsException) {
            exchange.setStatusCode(StatusCodes.FORBIDDEN);
            sendJson(exchange, "Insufficient funds");
        } else {
            exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
            sendJson(exchange, "Unknown error");
        }
    }
}
