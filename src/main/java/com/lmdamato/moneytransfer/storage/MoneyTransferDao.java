package com.lmdamato.moneytransfer.storage;

import com.lmdamato.moneytransfer.model.Money;
import com.lmdamato.moneytransfer.model.User;
import io.vavr.control.Try;

public interface MoneyTransferDao {
    Try<User> createUser(User user);
    Try<Money> getBalance(User user);
    Try<Void> deposit(User user, Money amount);
    Try<Void> withdraw(User user, Money amount);
    Try<Void> transfer(User from, User to, Money amount);
}
