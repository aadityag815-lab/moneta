package com.moneta.service;

import com.moneta.dto.GoalListResponse;
import com.moneta.dto.GoalRequest;
import com.moneta.dto.GoalResponse;
import com.moneta.dto.MessageResponse;
import com.moneta.dto.UpdateGoalRequest;
import com.moneta.entity.Category;
import com.moneta.entity.CategoryType;
import com.moneta.entity.SavingsGoal;
import com.moneta.entity.Transaction;
import com.moneta.entity.User;
import com.moneta.exception.BadRequestException;
import com.moneta.exception.ForbiddenException;
import com.moneta.exception.ResourceNotFoundException;
import com.moneta.repository.SavingsGoalRepository;
import com.moneta.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock
    private SavingsGoalRepository savingsGoalRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private GoalService goalService;

    private User testUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        testUser = new User("user@example.com", "pass", "John Doe", "1234");
        testUser.setId(1L);

        otherUser = new User("other@example.com", "pass", "Other", "5678");
        otherUser.setId(2L);
    }

    @Test
    void createGoal_Success_WithProgressCalculation() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(testUser);

        LocalDate futureTargetDate = LocalDate.now().plusYears(1);
        GoalRequest request = new GoalRequest(
                "Emergency Fund",
                new BigDecimal("5000.00"),
                futureTargetDate,
                LocalDate.of(2025, 1, 1)
        );

        SavingsGoal saved = new SavingsGoal(
                "Emergency Fund",
                new BigDecimal("5000.00"),
                futureTargetDate,
                LocalDate.of(2025, 1, 1),
                testUser
        );
        saved.setId(1L);

        when(savingsGoalRepository.save(any(SavingsGoal.class))).thenReturn(saved);

        // Transactions since start date: Income 2000, Expense 1000 => Net progress = 1000.00
        Category salaryCat = new Category("Salary", CategoryType.INCOME, false, null);
        Category foodCat = new Category("Food", CategoryType.EXPENSE, false, null);
        Transaction t1 = new Transaction(new BigDecimal("2000.00"), LocalDate.of(2025, 1, 10), "Salary", CategoryType.INCOME, "Salary", salaryCat, testUser);
        Transaction t2 = new Transaction(new BigDecimal("1000.00"), LocalDate.of(2025, 1, 15), "Food", CategoryType.EXPENSE, "Food", foodCat, testUser);

        when(transactionRepository.findByUserAndDateGreaterThanEqual(testUser, LocalDate.of(2025, 1, 1)))
                .thenReturn(Arrays.asList(t1, t2));

        GoalResponse response = goalService.createGoal(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Emergency Fund", response.getGoalName());
        assertEquals(new BigDecimal("5000.00"), response.getTargetAmount());
        assertEquals(new BigDecimal("1000.00"), response.getCurrentProgress());
        assertEquals(new BigDecimal("20.00"), response.getProgressPercentage());
        assertEquals(new BigDecimal("4000.00"), response.getRemainingAmount());
    }

    @Test
    void createGoal_InvalidTargetAmount_ThrowsBadRequest() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(testUser);

        GoalRequest request = new GoalRequest(
                "Emergency Fund",
                new BigDecimal("-500.00"),
                LocalDate.now().plusYears(1),
                null
        );

        assertThrows(BadRequestException.class, () -> goalService.createGoal(request));
    }

    @Test
    void createGoal_PastTargetDate_ThrowsBadRequest() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(testUser);

        GoalRequest request = new GoalRequest(
                "Emergency Fund",
                new BigDecimal("5000.00"),
                LocalDate.now().minusDays(1),
                null
        );

        assertThrows(BadRequestException.class, () -> goalService.createGoal(request));
    }

    @Test
    void getAllGoals_Success() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(testUser);

        SavingsGoal goal = new SavingsGoal("Vacation", new BigDecimal("3000.00"), LocalDate.now().plusMonths(6), LocalDate.of(2025, 1, 1), testUser);
        goal.setId(1L);

        when(savingsGoalRepository.findByUserOrderByIdAsc(testUser)).thenReturn(Collections.singletonList(goal));
        when(transactionRepository.findByUserAndDateGreaterThanEqual(testUser, LocalDate.of(2025, 1, 1))).thenReturn(Collections.emptyList());

        GoalListResponse response = goalService.getAllGoals();

        assertNotNull(response);
        assertEquals(1, response.getGoals().size());
        assertEquals(new BigDecimal("0.00"), response.getGoals().get(0).getCurrentProgress());
        assertEquals(new BigDecimal("3000.00"), response.getGoals().get(0).getRemainingAmount());
    }

    @Test
    void getGoalById_CrossUser_ThrowsForbidden() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(testUser);

        SavingsGoal goal = new SavingsGoal("Vacation", new BigDecimal("3000.00"), LocalDate.now().plusMonths(6), LocalDate.of(2025, 1, 1), otherUser);
        goal.setId(1L);

        when(savingsGoalRepository.findById(1L)).thenReturn(Optional.of(goal));

        assertThrows(ForbiddenException.class, () -> goalService.getGoalById(1L));
    }

    @Test
    void updateGoal_Success() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(testUser);

        SavingsGoal goal = new SavingsGoal("Vacation", new BigDecimal("3000.00"), LocalDate.now().plusMonths(6), LocalDate.of(2025, 1, 1), testUser);
        goal.setId(1L);

        when(savingsGoalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(savingsGoalRepository.save(any(SavingsGoal.class))).thenReturn(goal);
        when(transactionRepository.findByUserAndDateGreaterThanEqual(testUser, LocalDate.of(2025, 1, 1))).thenReturn(Collections.emptyList());

        LocalDate newTargetDate = LocalDate.now().plusMonths(12);
        UpdateGoalRequest updateReq = new UpdateGoalRequest(new BigDecimal("6000.00"), newTargetDate);

        GoalResponse response = goalService.updateGoal(1L, updateReq);

        assertNotNull(response);
        assertEquals(new BigDecimal("6000.00"), goal.getTargetAmount());
        assertEquals(newTargetDate, goal.getTargetDate());
    }

    @Test
    void deleteGoal_Success() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(testUser);

        SavingsGoal goal = new SavingsGoal("Vacation", new BigDecimal("3000.00"), LocalDate.of(2026, 6, 1), LocalDate.of(2025, 1, 1), testUser);
        goal.setId(1L);

        when(savingsGoalRepository.findById(1L)).thenReturn(Optional.of(goal));

        MessageResponse response = goalService.deleteGoal(1L);

        assertEquals("Goal deleted successfully", response.getMessage());
        verify(savingsGoalRepository).delete(goal);
    }
}
