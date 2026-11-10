package com.example.bankapp.controller;

import com.example.bankapp.dto.TransferRequest;
import com.example.bankapp.exception.BankException;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.service.BankService;
import com.example.bankapp.service.TransactionAuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
public class BankController {
    private final BankService bankService;
    private final TransactionAuditService auditService;

    @GetMapping("/")
    public String dashboard(@RequestParam(required = false) String keyword, Model model) {
        populateDashboard(keyword, model);
        return "bank";
    }

    @PostMapping("/customers")
    public String addCustomer(@Valid @ModelAttribute("customer") Customer customer,
                               BindingResult result, Model model,
                               RedirectAttributes redirect) {
        if (result.hasErrors()) {
            populateDashboard(null, model);
            return "bank";
        }
        try {
            bankService.createCustomer(customer);
            redirect.addFlashAttribute("success", "Customer added successfully");
        } catch (BankException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/transfer")
    public String transfer(@Valid @ModelAttribute("transferRequest") TransferRequest request,
                           BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            populateDashboard(null, model);
            return "bank";
        }
        try {
            bankService.transfer(request.getFromAccount(), request.getToAccount(),
                    request.getAmount(), request.isSimulateFailure());
            redirect.addFlashAttribute("success", "Transfer completed successfully");
        } catch (BankException ex) {
            auditService.recordFailure(request.getFromAccount(), request.getToAccount(),
                    request.getAmount(), ex.getMessage());
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/";
    }

    @GetMapping("/accounts/{accountNumber}/history")
    public String history(@PathVariable String accountNumber, Model model) {
        Customer customer = bankService.findCustomer(accountNumber)
                .orElseThrow(() -> new BankException("Account " + accountNumber + " was not found"));
        model.addAttribute("customer", customer);
        model.addAttribute("transactions", bankService.history(accountNumber));
        model.addAttribute("totalSent", bankService.totalSent(accountNumber));
        return "history";
    }

    @GetMapping("/large-transfers")
    public String largeTransfers(@RequestParam(defaultValue = "10000") BigDecimal min, Model model) {
        model.addAttribute("min", min);
        model.addAttribute("transfers", bankService.largeTransfers(min));
        return "large-transfers";
    }

    @GetMapping("/deposit")
    public String depositPage(Model model) {
        model.addAttribute("customers", bankService.listCustomers(null));
        return "deposit";
    }

    @PostMapping("/deposit")
    public String deposit(@RequestParam String accountNumber, @RequestParam BigDecimal amount,
                          RedirectAttributes redirect) {
        try {
            bankService.deposit(accountNumber, amount);
            redirect.addFlashAttribute("success", "Deposit completed successfully");
        } catch (BankException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/deposit";
    }

    @GetMapping("/withdraw")
    public String withdrawPage(Model model) {
        model.addAttribute("customers", bankService.listCustomers(null));
        return "withdraw";
    }

    @PostMapping("/withdraw")
    public String withdraw(@RequestParam String accountNumber, @RequestParam BigDecimal amount,
                           RedirectAttributes redirect) {
        try {
            bankService.withdraw(accountNumber, amount);
            redirect.addFlashAttribute("success", "Withdrawal completed successfully");
        } catch (BankException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/withdraw";
    }

    private void populateDashboard(String keyword, Model model) {
        model.addAttribute("customers", bankService.listCustomers(keyword));
        model.addAttribute("allCustomers", bankService.listCustomers(null));
        model.addAttribute("transactions", bankService.latestTransactions());
        model.addAttribute("totalCustomers", bankService.countCustomers());
        model.addAttribute("totalBalance", bankService.totalBalance());
        model.addAttribute("successCount", bankService.countTransactions(TransactionStatus.SUCCESS));
        model.addAttribute("failedCount", bankService.countTransactions(TransactionStatus.FAILED));
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        if (!model.containsAttribute("transferRequest")) {
            model.addAttribute("transferRequest", new TransferRequest());
        }
        if (!model.containsAttribute("customer")) {
            model.addAttribute("customer", new Customer());
        }
    }
}
