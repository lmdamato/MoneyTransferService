package com.lmdamato.moneytransfer.storage;

import com.lmdamato.moneytransfer.exception.InsufficientFundsException;
import com.lmdamato.moneytransfer.exception.UserAlreadyExistsException;
import com.lmdamato.moneytransfer.exception.UserDoesNotExistException;
import com.lmdamato.moneytransfer.model.Money;
import com.lmdamato.moneytransfer.model.User;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.NonNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryMoneyTransferDao implements MoneyTransferDao {
    @NonNull
    private final ConcurrentMap<User, Money> moneyMap;

    private static final MoneyTransferDao ourInstance = new InMemoryMoneyTransferDao();

    public static MoneyTransferDao getInstance() {
        return ourInstance;
    }

    private InMemoryMoneyTransferDao() {
        moneyMap = new ConcurrentHashMap<>();
    }

    @Override
    public Try<User> createUser(@NonNull final User user) {
        return Try
            .of(() -> {
                if (moneyMap.putIfAbsent(user, Money.ZERO) == null) {
                    return user;
                } else {
                    throw new UserAlreadyExistsException();
                }
            });
    }

    @Override
    public Try<Money> getBalance(@NonNull final User user) {
        return Option
            .of(moneyMap.get(user))
            .toTry(UserDoesNotExistException::new);
    }

    @Override
    public synchronized Try<Void> deposit(@NonNull final User user, @NonNull final Money amount) {
        // Only if user exists proceed to add funds
        return getBalance(user)
            .map(ignore -> moneyMap.merge(user, amount, Money::plus))
            .map(ignore -> null);
    }

    @Override
    public synchronized Try<Void> withdraw(@NonNull final User user, @NonNull final Money amountToWithdraw) {
        // Only if user exists and balance is enough proceed to withdraw funds
        return getBalance(user)
            .filter(
                fromAmount -> fromAmount.compareTo(amountToWithdraw) >= 0,
                InsufficientFundsException::new
            )
            .map(ignore -> moneyMap.merge(user, amountToWithdraw, Money::minus))
            .map(ignore -> null);
    }

    @Override
    public synchronized Try<Void> transfer(
        @NonNull final User from,
        @NonNull final User to,
        @NonNull final Money amountToTransfer
    ) {
        return getBalance(from)                     // Sending user exists
            .filter(                                // Sending user has enough funds
                fromAmount -> fromAmount.compareTo(amountToTransfer) >= 0,
                InsufficientFundsException::new
            )
            .flatMap(ignore -> getBalance(to))      // Receiving user exists
            .flatMap(ignore -> withdraw(from, amountToTransfer).flatMap(ignore2 -> deposit(to, amountToTransfer)));
    }
}
