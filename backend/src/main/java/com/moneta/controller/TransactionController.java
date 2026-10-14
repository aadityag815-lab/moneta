package com.moneta.controller;

import com.moneta.dto.MessageResponse;
import com.moneta.dto.TransactionListResponse;
import com.moneta.dto.TransactionRequest;
import com.moneta.dto.TransactionResponse;
import com.moneta.dto.UpdateTransactionRequest;
import com.moneta.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(@Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.createTransaction(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<TransactionListResponse> getTransactions(
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "type", required = false) String type) {

        TransactionListResponse response = transactionService.getTransactions(startDate, endDate, category, categoryId, type);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateTransactionRequest request) {

        TransactionResponse response = transactionService.updateTransaction(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteTransaction(@PathVariable("id") Long id) {
        MessageResponse response = transactionService.deleteTransaction(id);
        return ResponseEntity.ok(response);
    }
}
