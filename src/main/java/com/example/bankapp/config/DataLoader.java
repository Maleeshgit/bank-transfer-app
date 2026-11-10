package com.example.bankapp.config;

import com.example.bankapp.model.Customer;
import com.example.bankapp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {
    private final CustomerRepository customerRepository;

    @Override
    public void run(String... args) {
        if (customerRepository.count() == 0) {
            customerRepository.saveAll(List.of(
                    new Customer("100001", "Kamal Perera", "kamal@example.com", new BigDecimal("50000.00")),
                    new Customer("100002", "Nimali Silva", "nimali@example.com", new BigDecimal("25000.00")),
                    new Customer("100003", "Saman Fernando", "saman@example.com", new BigDecimal("10000.00"))
            ));
        }
    }
}
