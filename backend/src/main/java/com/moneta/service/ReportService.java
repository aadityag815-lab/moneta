package com.moneta.service;

import com.moneta.dto.MonthlyReportResponse;
import com.moneta.dto.YearlyReportResponse;
import com.moneta.entity.CategoryType;
import com.moneta.entity.Transaction;
import com.moneta.entity.User;
import com.moneta.exception.BadRequestException;
import com.moneta.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final AuthService authService;

    public ReportService(TransactionRepository transactionRepository, AuthService authService) {
        this.transactionRepository = transactionRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public MonthlyReportResponse getMonthlyReport(int year, int month) {
        if (year < 1900 || year > 2100) {
            throw new BadRequestException("Invalid year: " + year);
        }
        if (month < 1 || month > 12) {
            throw new BadRequestException("Invalid month: " + month + ". Must be between 1 and 12");
        }

        User currentUser = authService.getCurrentAuthenticatedUser();

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<Transaction> transactions = transactionRepository.findByUserAndDateBetween(currentUser, startDate, endDate);

        Map<String, BigDecimal> totalIncome = new LinkedHashMap<>();
        Map<String, BigDecimal> totalExpenses = new LinkedHashMap<>();
        BigDecimal sumIncome = BigDecimal.ZERO;
        BigDecimal sumExpenses = BigDecimal.ZERO;

        for (Transaction t : transactions) {
            BigDecimal amt = t.getAmount().setScale(2, RoundingMode.HALF_UP);
            String cat = t.getCategory();

            if (t.getType() == CategoryType.INCOME) {
                totalIncome.put(cat, totalIncome.getOrDefault(cat, BigDecimal.ZERO).add(amt).setScale(2, RoundingMode.HALF_UP));
                sumIncome = sumIncome.add(amt);
            } else if (t.getType() == CategoryType.EXPENSE) {
                totalExpenses.put(cat, totalExpenses.getOrDefault(cat, BigDecimal.ZERO).add(amt).setScale(2, RoundingMode.HALF_UP));
                sumExpenses = sumExpenses.add(amt);
            }
        }

        BigDecimal netSavings = sumIncome.subtract(sumExpenses).setScale(2, RoundingMode.HALF_UP);

        return new MonthlyReportResponse(month, year, totalIncome, totalExpenses, netSavings);
    }

    @Transactional(readOnly = true)
    public YearlyReportResponse getYearlyReport(int year) {
        if (year < 1900 || year > 2100) {
            throw new BadRequestException("Invalid year: " + year);
        }

        User currentUser = authService.getCurrentAuthenticatedUser();

        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        List<Transaction> transactions = transactionRepository.findByUserAndDateBetween(currentUser, startDate, endDate);

        Map<String, BigDecimal> totalIncome = new LinkedHashMap<>();
        Map<String, BigDecimal> totalExpenses = new LinkedHashMap<>();
        BigDecimal sumIncome = BigDecimal.ZERO;
        BigDecimal sumExpenses = BigDecimal.ZERO;

        for (Transaction t : transactions) {
            BigDecimal amt = t.getAmount().setScale(2, RoundingMode.HALF_UP);
            String cat = t.getCategory();

            if (t.getType() == CategoryType.INCOME) {
                totalIncome.put(cat, totalIncome.getOrDefault(cat, BigDecimal.ZERO).add(amt).setScale(2, RoundingMode.HALF_UP));
                sumIncome = sumIncome.add(amt);
            } else if (t.getType() == CategoryType.EXPENSE) {
                totalExpenses.put(cat, totalExpenses.getOrDefault(cat, BigDecimal.ZERO).add(amt).setScale(2, RoundingMode.HALF_UP));
                sumExpenses = sumExpenses.add(amt);
            }
        }

        BigDecimal netSavings = sumIncome.subtract(sumExpenses).setScale(2, RoundingMode.HALF_UP);

        return new YearlyReportResponse(year, totalIncome, totalExpenses, netSavings);
    }
}
