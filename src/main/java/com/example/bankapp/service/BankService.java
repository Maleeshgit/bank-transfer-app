package com.example.bankapp.service;

import com.example.bankapp.exception.AccountNotFoundException;
import com.example.bankapp.exception.BankException;
import com.example.bankapp.exception.InsufficientBalanceException;
import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;
import com.example.bankapp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)          // default for every method: read-only transaction
public class BankService {
    private final CustomerRepository customerRepository;
    private final BankTransactionRepository transactionRepository;

    public List<Customer> listCustomers(String keyword) {
        return keyword == null || keyword.isBlank()
                ? customerRepository.findAllOrdered()
                : customerRepository.searchByName(keyword.trim());
    }

    public Optional<Customer> findCustomer(String accountNumber) {
        return customerRepository.getByAccountNumber(accountNumber);
    }

    public BigDecimal totalBalance() {
        return Optional.ofNullable(customerRepository.getTotalBalance()).orElse(BigDecimal.ZERO);
    }

    public long countCustomers() {
        return customerRepository.countCustomersNative();
    }

    public List<BankTransaction> latestTransactions() {
        return transactionRepository.findAllLatest();
    }

    public long countTransactions(TransactionStatus status) {
        return transactionRepository.countByStatus(status);
    }

    @Transactional
    public Customer createCustomer(Customer customer) {
        if (customerRepository.countAccounts(customer.getAccountNumber()) > 0) {
            throw new BankException("Account number already exists");
        }
        if (customerRepository.countEmails(customer.getEmail()) > 0) {
            throw new BankException("Email already exists");
        }
        customer.setBalance(customer.getBalance().setScale(2, RoundingMode.HALF_UP));
        return customerRepository.save(customer);
    }

    @Transactional(rollbackFor = Exception.class)
    public BankTransaction transfer(String fromAccount, String toAccount, BigDecimal amount,
                                    boolean simulateFailure) {
        if (fromAccount.equals(toAccount)) {
            throw new BankException("Sender and receiver must be different accounts");
        }
        requireCustomer(fromAccount);
        requireCustomer(toAccount);
        BigDecimal normalizedAmount = normalize(amount);

        if (customerRepository.debit(fromAccount, normalizedAmount) == 0) {
            throw new InsufficientBalanceException(fromAccount);
        }
        if (simulateFailure) {
            throw new BankException("All changes were rolled back (simulated failure)");
        }
        if (customerRepository.credit(toAccount, normalizedAmount) == 0) {
            throw new AccountNotFoundException(toAccount);
        }

        return transactionRepository.save(new BankTransaction(
                fromAccount, toAccount, normalizedAmount, TransactionStatus.SUCCESS,
                "Transfer completed"));
    }

    @Transactional
    public BankTransaction deposit(String accountNumber, BigDecimal amount) {
        requireCustomer(accountNumber);
        BigDecimal normalizedAmount = normalize(amount);
        if (customerRepository.credit(accountNumber, normalizedAmount) == 0) {
            throw new AccountNotFoundException(accountNumber);
        }
        return transactionRepository.save(new BankTransaction(
                null, accountNumber, normalizedAmount, TransactionStatus.SUCCESS,
                "Deposit completed"));
    }

    @Transactional
    public BankTransaction withdraw(String accountNumber, BigDecimal amount) {
        requireCustomer(accountNumber);
        BigDecimal normalizedAmount = normalize(amount);
        if (customerRepository.debit(accountNumber, normalizedAmount) == 0) {
            throw new InsufficientBalanceException(accountNumber);
        }
        return transactionRepository.save(new BankTransaction(
                accountNumber, null, normalizedAmount, TransactionStatus.SUCCESS,
                "Withdrawal completed"));
    }

    public List<BankTransaction> history(String accountNumber) {
        return transactionRepository.findHistory(accountNumber);
    }

    public List<BankTransaction> largeTransfers(BigDecimal minimum) {
        return transactionRepository.findSuccessfulAbove(normalize(minimum));
    }

    public BigDecimal totalSent(String accountNumber) {
        return transactionRepository.totalSentByAccount(accountNumber);
    }

    private Customer requireCustomer(String accountNumber) {
        return customerRepository.getByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    private BigDecimal normalize(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BankException("Amount must be greater than zero");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
