import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.*;

public class ConcurrentBankExample {
    public static void main(String[] args) {
        ConcurrentBank bank = new ConcurrentBank();

        // Создание счетов
        BankAccount account1 = bank.createAccount(1000);
        BankAccount account2 = bank.createAccount(500);

        // Перевод между счетами в разных потоках
        Thread transferThread1 = new Thread(() -> bank.transfer(account1, account2, 200));
        Thread transferThread2 = new Thread(() -> bank.transfer(account1, account2, 100));

        transferThread1.start();
        transferThread2.start();

        try {
            transferThread1.join();
            transferThread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Вывод балансов
        System.out.println("Account 1 balance: " + account1.getBalance());
        System.out.println("Account 2 balance: " + account2.getBalance());
        System.out.println("Total balance: " + bank.getTotalBalance());
    }
}

class BankAccount {
    private final int ACC_NUMBER;
    private double balance;
    private final Lock LOCK = new ReentrantLock();

    public BankAccount(int ACC_NUMBER, double balance) {
        this.ACC_NUMBER = ACC_NUMBER;
        this.balance = balance;
    }

    public int getAccountNumber() {
        return ACC_NUMBER;
    }

    public void deposit(double amount) {
        LOCK.lock();
        try {
            if (amount>0)
                balance+=amount;
        } finally {
            LOCK.unlock();
        }
    }

    public boolean withdraw(double amount) {
        LOCK.lock();
        try {
            if (amount>0 && balance>=amount) {
                balance-=amount;
                return true;
            }
            return false;
        } finally {
            LOCK.unlock();
        }
    }

    public double getBalance() {
        LOCK.lock();
        try {
            return balance;
        } finally {
            LOCK.unlock();
        }
    }

    public Lock getLock() {
        return LOCK;
    }
}

class ConcurrentBank {
    private Map<Integer, BankAccount> accounts = new ConcurrentHashMap<>();
    private int accNumber = 1;
    private final Lock BANK_LOCK = new ReentrantLock();

    public BankAccount createAccount(double balance) {
        BANK_LOCK.lock();
        try {
            int acc = accNumber++;
            BankAccount bankAcc = new BankAccount(acc, balance);
            accounts.put(acc, bankAcc);
            return bankAcc;
        } finally {
            BANK_LOCK.unlock();
        }
    }

    public boolean transfer(BankAccount fromAcc, BankAccount toAcc, double amount) {

        if (fromAcc == null || toAcc == null || fromAcc.getAccountNumber() == toAcc.getAccountNumber())
            return false;

        BankAccount acc1lock = (fromAcc.getAccountNumber()<toAcc.getAccountNumber()) ? fromAcc : toAcc;
        BankAccount acc2lock = (fromAcc.getAccountNumber()<toAcc.getAccountNumber()) ? toAcc : fromAcc;

        acc1lock.getLock().lock();
        acc2lock.getLock().lock();
        try {
            if (fromAcc.withdraw(amount)) {
                toAcc.deposit(amount);
                return true;
            } else {
                return false;
            }
        } finally {
            acc1lock.getLock().unlock();
            acc2lock.getLock().unlock();
        }
    }

    public double getTotalBalance() {
        BANK_LOCK.lock();
        try {
            double total = 0;
            for (BankAccount acc : accounts.values()) {
                total+=acc.getBalance();
            }
            return total;
        } finally {
            BANK_LOCK.unlock();
        }
    }
}
