package com.moneta.service;

import com.moneta.dto.MonthlyReportResponse;
import com.moneta.dto.YearlyReportResponse;
import com.moneta.entity.Category;
import com.moneta.entity.CategoryType;
import com.moneta.entity.Transaction;
import com.moneta.entity.User;
import com.moneta.exception.BadRequestException;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private ReportService reportService;

    private User testUser;
    private Category salaryCategory;
    private Category freelanceCategory;
    private Category foodCategory;
    private Category rentCategory;

    @BeforeEach
    void setUp() {
        testUser = new User("user@example.com", "pass", "John Doe", "1234");
        testUser.setId(1L);

        salaryCategory = new Category("Salary", CategoryType.INCOME, false, null);
        freelanceCategory = new Category("Freelance", CategoryType.INCOME, true, testUser);
        foodCategory = new Category("Food", CategoryType.EXPENSE, false, null);
        rentCategory = new Category("Rent", CategoryType.EXPENSE, false, null);
    }

    @Test
    void getMonthlyReport_Success() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(testUser);

        Transaction t1 = new Transaction(new BigDecimal("3000.00"), LocalDate.of(2024, 1, 10), "Salary", CategoryType.INCOME, "Salary", salaryCategory, testUser);
        Transaction t2 = new Transaction(new BigDecimal("500.00"), LocalDate.of(2024, 1, 15), "Freelance", CategoryType.INCOME, "Freelance", freelanceCategory, testUser);
        Transaction t3 = new Transaction(new BigDecimal("400.00"), LocalDate.of(2024, 1, 12), "Food", CategoryType.EXPENSE, "Food", foodCategory, testUser);
        Transaction t4 = new Transaction(new BigDecimal("1200.00"), LocalDate.of(2024, 1, 1), "Rent", CategoryType.EXPENSE, "Rent", rentCategory, testUser);

        when(transactionRepository.findByUserAndDateBetween(eq(testUser), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Arrays.asList(t1, t2, t3, t4));

        MonthlyReportResponse report = reportService.getMonthlyReport(2024, 1);

        assertNotNull(report);
        assertEquals(1, report.getMonth());
        assertEquals(2024, report.getYear());

        assertEquals(new BigDecimal("3000.00"), report.getTotalIncome().get("Salary"));
        assertEquals(new BigDecimal("500.00"), report.getTotalIncome().get("Freelance"));
        assertEquals(new BigDecimal("400.00"), report.getTotalExpenses().get("Food"));
        assertEquals(new BigDecimal("1200.00"), report.getTotalExpenses().get("Rent"));

        // Net savings = 3500 - 1600 = 1900.00
        assertEquals(new BigDecimal("1900.00"), report.getNetSavings());
    }

    @Test
    void getMonthlyReport_InvalidMonth_ThrowsBadRequest() {
        assertThrows(BadRequestException.class, () -> reportService.getMonthlyReport(2024, 13));
        assertThrows(BadRequestException.class, () -> reportService.getMonthlyReport(2024, 0));
    }

    @Test
    void getYearlyReport_Success() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(testUser);

        Transaction t1 = new Transaction(new BigDecimal("36000.00"), LocalDate.of(2024, 5, 1), "Salary", CategoryType.INCOME, "Salary", salaryCategory, testUser);
        Transaction t2 = new Transaction(new BigDecimal("6000.00"), LocalDate.of(2024, 6, 1), "Freelance", CategoryType.INCOME, "Freelance", freelanceCategory, testUser);
        Transaction t3 = new Transaction(new BigDecimal("4800.00"), LocalDate.of(2024, 2, 1), "Food", CategoryType.EXPENSE, "Food", foodCategory, testUser);
        Transaction t4 = new Transaction(new BigDecimal("14400.00"), LocalDate.of(2024, 1, 1), "Rent", CategoryType.EXPENSE, "Rent", rentCategory, testUser);

        when(transactionRepository.findByUserAndDateBetween(eq(testUser), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Arrays.asList(t1, t2, t3, t4));

        YearlyReportResponse report = reportService.getYearlyReport(2024);

        assertNotNull(report);
        assertEquals(2024, report.getYear());
        assertEquals(new BigDecimal("36000.00"), report.getTotalIncome().get("Salary"));
        assertEquals(new BigDecimal("6000.00"), report.getTotalIncome().get("Freelance"));
        assertEquals(new BigDecimal("4800.00"), report.getTotalExpenses().get("Food"));
        assertEquals(new BigDecimal("14400.00"), report.getTotalExpenses().get("Rent"));
        assertEquals(new BigDecimal("22800.00"), report.getNetSavings());
    }

    @Test
    void getYearlyReport_InvalidYear_ThrowsBadRequest() {
        assertThrows(BadRequestException.class, () -> reportService.getYearlyReport(1899));
    }
}
