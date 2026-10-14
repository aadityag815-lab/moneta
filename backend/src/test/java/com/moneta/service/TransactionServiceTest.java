package com.moneta.service;

import com.moneta.dto.MessageResponse;
import com.moneta.dto.TransactionListResponse;
import com.moneta.dto.TransactionRequest;
import com.moneta.dto.TransactionResponse;
import com.moneta.dto.UpdateTransactionRequest;
import com.moneta.entity.Category;
import com.moneta.entity.CategoryType;
import com.moneta.entity.Transaction;
import com.moneta.entity.User;
import com.moneta.exception.BadRequestException;
import com.moneta.exception.ForbiddenException;
import com.moneta.exception.ResourceNotFoundException;
import com.moneta.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private AuthService authService;

    @InjectMocks
    private TransactionService transactionService;

    private User testUser;
    private User otherUser;
    private Category salaryCategory;

    @BeforeEach
    void setUp() {
        testUser = new User("user@example.com", "pass", "John Doe", "1234");
        testUser.setId(1L);

        otherUser = new User("other@example.com", "pass", "Other", "5678");
        otherUser.setId(2L);

        salaryCategory = new Category("Salary", CategoryType.INCOME, false, null);
        salaryCategory.setId(10L);
    }

    @Test
    void createTransaction_Success() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(testUser);
        when(categoryService.findAccessibleCategoryByName("Salary", testUser)).thenReturn(salaryCategory);

        TransactionRequest request = new TransactionRequest(
                new BigDecimal("50000.00"),
                LocalDate.of(2024, 1, 15),
                "Salary",
                "January Salary"
        );

        Transaction saved = new Transaction(
                request.getAmount(),
                request.getDate(),
                "Salary",
                CategoryType.INCOME,
                "January Salary",
                salaryCategory,
                testUser
        );
        saved.setId(1L);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);

        TransactionResponse response = transactionService.createTransaction(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(new BigDecimal("50000.00"), response.getAmount());
        assertEquals("INCOME", response.getType());
        assertEquals("Salary", response.getCategory());
    }

    @Test
    void createTransaction_NegativeAmount_ThrowsBadRequest() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(testUser);

        TransactionRequest request = new TransactionRequest(
                new BigDecimal("-50.00"),
                LocalDate.of(2024, 1, 15),
                "Salary",
                "Invalid"
        );

        assertThrows(BadRequestException.class, () -> transactionService.createTransaction(request));
    }

    @Test
    void createTransaction_FutureDate_ThrowsBadRequest() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(testUser);

        TransactionRequest request = new TransactionRequest(
                new BigDecimal("100.00"),
                LocalDate.now().plusDays(5),
                "Salary",
                "Future"
        );

        assertThrows(BadRequestException.class, () -> transactionService.createTransaction(request));
    }

    @Test
    void getTransactions_Success() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(testUser);

        Transaction transaction = new Transaction(
                new BigDecimal("1000.00"),
                LocalDate.of(2024, 1, 1),
                "Salary",
                CategoryType.INCOME,
                "Desc",
                salaryCategory,
                testUser
        );
        transaction.setId(1L);

        when(transactionRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(Collections.singletonList(transaction));

        TransactionListResponse response = transactionService.getTransactions(null, null, null, null, null);

        assertNotNull(response);
        assertEquals(1, response.getTransactions().size());
        assertEquals(1L, response.getTransactions().get(0).getId());
    }

    @Test
    void getTransactions_InvalidType_ThrowsBadRequest() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(testUser);

        assertThrows(BadRequestException.class, () ->
                transactionService.getTransactions(null, null, null, null, "INVALID"));
    }

    @Test
    void updateTransaction_Success() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(testUser);

        Transaction existing = new Transaction(
                new BigDecimal("50000.00"),
                LocalDate.of(2024, 1, 15),
                "Salary",
                CategoryType.INCOME,
                "Old Salary",
                salaryCategory,
                testUser
        );
        existing.setId(1L);

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(existing);

        UpdateTransactionRequest updateReq = new UpdateTransactionRequest();
        updateReq.setAmount(new BigDecimal("60000.00"));
        updateReq.setDescription("Updated Salary");

        TransactionResponse response = transactionService.updateTransaction(1L, updateReq);

        assertNotNull(response);
        assertEquals(new BigDecimal("60000.00"), existing.getAmount());
        assertEquals("Updated Salary", existing.getDescription());
    }

    @Test
    void updateTransaction_DateModificationAttempt_ThrowsBadRequest() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(testUser);

        Transaction existing = new Transaction(
                new BigDecimal("50000.00"),
                LocalDate.of(2024, 1, 15),
                "Salary",
                CategoryType.INCOME,
                "Old Salary",
                salaryCategory,
                testUser
        );
        existing.setId(1L);

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(existing));

        UpdateTransactionRequest updateReq = new UpdateTransactionRequest();
        updateReq.setDate(LocalDate.of(2024, 1, 20)); // Changed date!

        assertThrows(BadRequestException.class, () -> transactionService.updateTransaction(1L, updateReq));
    }

    @Test
    void updateTransaction_CrossUser_ThrowsForbidden() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(testUser);

        Transaction existing = new Transaction(
                new BigDecimal("50000.00"),
                LocalDate.of(2024, 1, 15),
                "Salary",
                CategoryType.INCOME,
                "Old Salary",
                salaryCategory,
                otherUser
        );
        existing.setId(1L);

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(existing));

        UpdateTransactionRequest updateReq = new UpdateTransactionRequest();
        updateReq.setAmount(new BigDecimal("60000.00"));

        assertThrows(ForbiddenException.class, () -> transactionService.updateTransaction(1L, updateReq));
    }

    @Test
    void deleteTransaction_Success() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(testUser);

        Transaction existing = new Transaction(
                new BigDecimal("50000.00"),
                LocalDate.of(2024, 1, 15),
                "Salary",
                CategoryType.INCOME,
                "Old Salary",
                salaryCategory,
                testUser
        );
        existing.setId(1L);

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(existing));

        MessageResponse response = transactionService.deleteTransaction(1L);

        assertEquals("Transaction deleted successfully", response.getMessage());
        verify(transactionRepository).delete(existing);
    }

    @Test
    void deleteTransaction_NotFound_ThrowsResourceNotFound() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(testUser);
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> transactionService.deleteTransaction(99L));
    }

    @Test
    void deleteTransaction_CrossUser_ThrowsForbidden() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(testUser);

        Transaction existing = new Transaction(
                new BigDecimal("50000.00"),
                LocalDate.of(2024, 1, 15),
                "Salary",
                CategoryType.INCOME,
                "Old Salary",
                salaryCategory,
                otherUser
        );
        existing.setId(1L);

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(ForbiddenException.class, () -> transactionService.deleteTransaction(1L));
        verify(transactionRepository, never()).delete(any(Transaction.class));
    }
}
