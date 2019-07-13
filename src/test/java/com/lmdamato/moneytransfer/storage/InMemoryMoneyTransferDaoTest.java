package com.lmdamato.moneytransfer.storage;

import com.lmdamato.moneytransfer.exception.InsufficientFundsException;
import com.lmdamato.moneytransfer.exception.UserAlreadyExistsException;
import com.lmdamato.moneytransfer.exception.UserDoesNotExistException;
import com.lmdamato.moneytransfer.model.Money;
import com.lmdamato.moneytransfer.model.User;
import io.vavr.control.Try;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InMemoryMoneyTransferDaoTest {
    private MoneyTransferDao dao = InMemoryMoneyTransferDao.getInstance();

    @Test
    public void testCreateUserSuccess() {
        final User newUser = new User("abc");
        final Try<User> u = dao.createUser(newUser);

        assertTrue(u.isSuccess());
        assertEquals(newUser, u.get());
    }

    @Test
    public void testCreateUserFailure_UserAlreadyExists() {
        final User newUser = new User("def");
        final Try<User> u1 = dao.createUser(newUser);
        final Try<User> u2 = dao.createUser(newUser);

        assertTrue(u2.isFailure());
        assertTrue(u2.getCause() instanceof UserAlreadyExistsException);
    }

    @Test
    public void testGetBalanceSuccess() {
        final User newUser = new User("ghi");
        final Try<User> u1 = dao.createUser(newUser);
        final Try<Money> b = dao.getBalance(u1.get());

        assertTrue(b.isSuccess());
        assertEquals(Money.ZERO, b.get());
    }

    @Test
    public void testGetBalanceFailure_UserDoesNotExist() {
        final Try<Money> b = dao.getBalance(new User("xxx"));

        assertTrue(b.isFailure());
        assertTrue(b.getCause() instanceof UserDoesNotExistException);
    }

    @Test
    public void testDepositSuccess() {
        final User newUser = new User("jkl");
        final Money amount = new Money(new BigDecimal("12.34"));

        final Try<User> u1 = dao.createUser(newUser);
        final Try<Void> d = dao.deposit(newUser, amount);

        assertTrue(d.isSuccess());
    }

    @Test
    public void testDepositFailure_UserDoesNotExist() {
        final User nonExistingUser = new User("xxx");
        final Money amount = new Money(new BigDecimal("12.34"));

        final Try<Void> d = dao.deposit(nonExistingUser, amount);

        assertTrue(d.isFailure());
        assertTrue(d.getCause() instanceof UserDoesNotExistException);
    }

    @Test
    public void testWithdrawSuccess() {
        final User newUser = new User("mno");
        final Money amountToDeposit = new Money(new BigDecimal("12.34"));
        final Money amountToWithdraw = new Money(new BigDecimal("9.99"));

        final Try<User> u1 = dao.createUser(newUser);
        final Try<Void> d = dao.deposit(newUser, amountToDeposit);
        final Try<Void> w = dao.withdraw(newUser, amountToWithdraw);

        assertTrue(w.isSuccess());
    }

    @Test
    public void testWithdrawFailure_UserDoesNotExist() {
        final User newUser = new User("xxx");
        final Money amountToWithdraw = new Money(new BigDecimal("9.99"));

        final Try<Void> w = dao.withdraw(newUser, amountToWithdraw);

        assertTrue(w.isFailure());
        assertTrue(w.getCause() instanceof UserDoesNotExistException);
    }

    @Test
    public void testWithdrawFailure_InsufficientFunds() {
        final User newUser = new User("pqr");
        final Money amountToDeposit = new Money(new BigDecimal("12.34"));
        final Money amountToWithdraw = new Money(new BigDecimal("100.99"));

        final Try<User> u1 = dao.createUser(newUser);
        final Try<Void> d = dao.deposit(newUser, amountToDeposit);
        final Try<Void> w = dao.withdraw(newUser, amountToWithdraw);

        assertTrue(w.isFailure());
        assertTrue(w.getCause() instanceof InsufficientFundsException);
    }

    @Test
    public void testTransferSuccess() {
        final User user1 = new User("sto");
        final User user2 = new User("pqr");

        final Money amountToDeposit = new Money(new BigDecimal("12.34"));
        final Money amountToTransfer = new Money(new BigDecimal("9.00"));

        final Try<User> u1 = dao.createUser(user1);
        final Try<User> u2 = dao.createUser(user2);

        final Try<Void> d = dao.deposit(user1, amountToDeposit);
        final Try<Void> t = dao.transfer(user1, user2, amountToTransfer);

        assertTrue(t.isSuccess());

        final Try<Money> b1 = dao.getBalance(user1);
        final Try<Money> b2 = dao.getBalance(user2);

        assertTrue(b1.isSuccess());
        assertEquals(amountToDeposit.minus(amountToTransfer), b1.get());

        assertTrue(b2.isSuccess());
        assertEquals(amountToTransfer, b2.get());
    }

    @Test
    public void testTransferFailure_SendingUserDoesNotExist() {
        final User user1 = new User("xxx");
        final User user2 = new User("stu");

        final Money amountToTransfer = new Money(new BigDecimal("9.00"));

        final Try<User> u2 = dao.createUser(user2);
        final Try<Void> t = dao.transfer(user1, user2, amountToTransfer);

        assertTrue(t.isFailure());
        assertTrue(t.getCause() instanceof UserDoesNotExistException);
    }

    @Test
    public void testTransferFailure_ReceivingUserDoesNotExist() {
        final User user1 = new User("vwx");
        final User user2 = new User("xxx");

        final Money amountToDeposit = new Money(new BigDecimal("12.34"));
        final Money amountToTransfer = new Money(new BigDecimal("9.00"));

        final Try<User> u1 = dao.createUser(user1);
        final Try<Void> d = dao.deposit(user1, amountToDeposit);
        final Try<Void> t = dao.transfer(user1, user2, amountToTransfer);

        assertTrue(t.isFailure());
        assertTrue(t.getCause() instanceof UserDoesNotExistException);
    }

    @Test
    public void testTransferFailure_InsufficientFunds() {
        final User user1 = new User("abc1");
        final User user2 = new User("def2");

        final Money amountToTransfer = new Money(new BigDecimal("9.00"));

        final Try<User> u1 = dao.createUser(user1);
        final Try<User> u2 = dao.createUser(user2);

        final Try<Void> t = dao.transfer(user1, user2, amountToTransfer);

        assertTrue(t.isFailure());
        assertTrue(t.getCause() instanceof InsufficientFundsException);
    }
}
