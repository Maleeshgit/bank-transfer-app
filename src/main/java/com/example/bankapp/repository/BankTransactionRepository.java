package com.example.bankapp.repository;

import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface BankTransactionRepository
        extends JpaRepository<BankTransaction, Long> {
    @Query("select t from BankTransaction t order by t.createdAt desc, t.id desc")
    List<BankTransaction> findAllLatest();

    @Query("select t from BankTransaction t where t.fromAccount = :account or t.toAccount = :account order by t.createdAt desc, t.id desc")
    List<BankTransaction> findHistory(@Param("account") String account);

    @Query("select t from BankTransaction t where t.status = :status order by t.createdAt desc, t.id desc")
    List<BankTransaction> findByStatusLatest(@Param("status") TransactionStatus status);

    @Query("select count(t) from BankTransaction t where t.status = :status")
    long countByStatus(@Param("status") TransactionStatus status);

    @Query("select t from BankTransaction t where t.status = com.example.bankapp.model.TransactionStatus.SUCCESS and t.amount > :minimum order by t.amount desc, t.createdAt desc")
    List<BankTransaction> findSuccessfulAbove(@Param("minimum") BigDecimal minimum);

    @Query("select coalesce(sum(t.amount), 0) from BankTransaction t where t.status = com.example.bankapp.model.TransactionStatus.SUCCESS and t.fromAccount = :account")
    BigDecimal totalSentByAccount(@Param("account") String account);
}