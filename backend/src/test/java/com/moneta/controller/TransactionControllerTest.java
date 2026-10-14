package com.moneta.controller;

import com.moneta.dto.MessageResponse;
import com.moneta.dto.TransactionListResponse;
import com.moneta.dto.TransactionRequest;
import com.moneta.dto.TransactionResponse;
import com.moneta.dto.UpdateTransactionRequest;
import com.moneta.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionController transactionController;

    @Test
    void createTransaction() {
        TransactionRequest req = new TransactionRequest(new BigDecimal("100.00"), LocalDate.of(2024, 1, 1), "Salary", "Desc");
        when(transactionService.createTransaction(req))
                .thenReturn(new TransactionResponse(1L, new BigDecimal("100.00"), LocalDate.of(2024, 1, 1), "Salary", "Desc", "INCOME"));

        ResponseEntity<TransactionResponse> res = transactionController.createTransaction(req);
        assertEquals(HttpStatus.CREATED, res.getStatusCode());
        assertEquals(1L, res.getBody().getId());
    }

    @Test
    void getTransactions() {
        when(transactionService.getTransactions(any(), any(), any(), any(), any()))
                .thenReturn(new TransactionListResponse(Collections.emptyList()));

        ResponseEntity<TransactionListResponse> res = transactionController.getTransactions(null, null, null, null, null);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void updateTransaction() {
        UpdateTransactionRequest req = new UpdateTransactionRequest(new BigDecimal("200.00"), "Updated", "Salary");
        when(transactionService.updateTransaction(eq(1L), any()))
                .thenReturn(new TransactionResponse(1L, new BigDecimal("200.00"), LocalDate.of(2024, 1, 1), "Salary", "Updated", "INCOME"));

        ResponseEntity<TransactionResponse> res = transactionController.updateTransaction(1L, req);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(new BigDecimal("200.00"), res.getBody().getAmount());
    }

    @Test
    void deleteTransaction() {
        when(transactionService.deleteTransaction(1L)).thenReturn(new MessageResponse("Transaction deleted successfully"));

        ResponseEntity<MessageResponse> res = transactionController.deleteTransaction(1L);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals("Transaction deleted successfully", res.getBody().getMessage());
    }
}
