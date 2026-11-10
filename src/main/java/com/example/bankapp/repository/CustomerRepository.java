package com.example.bankapp.repository;

import com.example.bankapp.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    @Query("select c from Customer c where c.accountNumber = :accountNumber")
    Optional<Customer> getByAccountNumber(@Param("accountNumber") String accountNumber);

    @Query("select count(c) from Customer c where c.accountNumber = :accountNumber")
    long countAccounts(@Param("accountNumber") String accountNumber);

    @Query("select count(c) from Customer c where lower(c.email) = lower(:email)")
    long countEmails(@Param("email") String email);

    @Query("select c from Customer c order by c.fullName asc")
    List<Customer> findAllOrdered();

    @Query("select c from Customer c where lower(c.fullName) like lower(concat('%', :keyword, '%')) order by c.fullName asc")
    List<Customer> searchByName(@Param("keyword") String keyword);

    @Query("select sum(c.balance) from Customer c")
    BigDecimal getTotalBalance();

    @Query(value = "select count(*) from customers", nativeQuery = true)
    long countCustomersNative();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update Customer c set c.balance = c.balance - :amount where c.accountNumber = :accountNumber and c.balance >= :amount")
    int debit(@Param("accountNumber") String accountNumber, @Param("amount") BigDecimal amount);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update Customer c set c.balance = c.balance + :amount where c.accountNumber = :accountNumber")
    int credit(@Param("accountNumber") String accountNumber, @Param("amount") BigDecimal amount);
}
