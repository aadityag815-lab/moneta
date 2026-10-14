package com.moneta.service;

import com.moneta.dto.GoalListResponse;
import com.moneta.dto.GoalRequest;
import com.moneta.dto.GoalResponse;
import com.moneta.dto.MessageResponse;
import com.moneta.dto.UpdateGoalRequest;
import com.moneta.entity.CategoryType;
import com.moneta.entity.SavingsGoal;
import com.moneta.entity.Transaction;
import com.moneta.entity.User;
import com.moneta.exception.BadRequestException;
import com.moneta.exception.ForbiddenException;
import com.moneta.exception.ResourceNotFoundException;
import com.moneta.repository.SavingsGoalRepository;
import com.moneta.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class GoalService {

    private final SavingsGoalRepository savingsGoalRepository;
    private final TransactionRepository transactionRepository;
    private final AuthService authService;

    public GoalService(SavingsGoalRepository savingsGoalRepository,
                       TransactionRepository transactionRepository,
                       AuthService authService) {
        this.savingsGoalRepository = savingsGoalRepository;
        this.transactionRepository = transactionRepository;
        this.authService = authService;
    }

    @Transactional
    public GoalResponse createGoal(GoalRequest request) {
        User currentUser = authService.getCurrentAuthenticatedUser();

        validateGoalRequest(request);

        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();

        SavingsGoal goal = new SavingsGoal(
                request.getGoalName().trim(),
                request.getTargetAmount(),
                request.getTargetDate(),
                startDate,
                currentUser
        );

        SavingsGoal saved = savingsGoalRepository.save(goal);
        return calculateAndMapGoal(saved, currentUser);
    }

    @Transactional(readOnly = true)
    public GoalListResponse getAllGoals() {
        User currentUser = authService.getCurrentAuthenticatedUser();
        List<SavingsGoal> goals = savingsGoalRepository.findByUserOrderByIdAsc(currentUser);

        List<GoalResponse> responses = new ArrayList<>();
        for (SavingsGoal g : goals) {
            responses.add(calculateAndMapGoal(g, currentUser));
        }

        return new GoalListResponse(responses);
    }

    @Transactional(readOnly = true)
    public GoalResponse getGoalById(Long id) {
        User currentUser = authService.getCurrentAuthenticatedUser();

        SavingsGoal goal = savingsGoalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found with id: " + id));

        if (!goal.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Cannot access another user's goal");
        }

        return calculateAndMapGoal(goal, currentUser);
    }

    @Transactional
    public GoalResponse updateGoal(Long id, UpdateGoalRequest request) {
        User currentUser = authService.getCurrentAuthenticatedUser();

        SavingsGoal goal = savingsGoalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found with id: " + id));

        if (!goal.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Cannot modify another user's goal");
        }

        if (request.getTargetAmount() != null) {
            if (request.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Target amount must be a positive decimal value");
            }
            goal.setTargetAmount(request.getTargetAmount());
        }

        if (request.getTargetDate() != null) {
            if (!request.getTargetDate().isAfter(LocalDate.now())) {
                throw new BadRequestException("Target date must be a future date");
            }
            goal.setTargetDate(request.getTargetDate());
        }

        if (request.getGoalName() != null && !request.getGoalName().trim().isEmpty()) {
            goal.setGoalName(request.getGoalName().trim());
        }

        SavingsGoal updated = savingsGoalRepository.save(goal);
        return calculateAndMapGoal(updated, currentUser);
    }

    @Transactional
    public MessageResponse deleteGoal(Long id) {
        User currentUser = authService.getCurrentAuthenticatedUser();

        SavingsGoal goal = savingsGoalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found with id: " + id));

        if (!goal.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Cannot delete another user's goal");
        }

        savingsGoalRepository.delete(goal);
        return new MessageResponse("Goal deleted successfully");
    }

    private void validateGoalRequest(GoalRequest request) {
        if (request.getGoalName() == null || request.getGoalName().trim().isEmpty()) {
            throw new BadRequestException("Goal name is required");
        }
        if (request.getTargetAmount() == null || request.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Target amount must be a positive decimal value");
        }
        if (request.getTargetDate() == null || !request.getTargetDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Target date must be a future date");
        }
    }

    private GoalResponse calculateAndMapGoal(SavingsGoal goal, User user) {
        List<Transaction> transactions = transactionRepository.findByUserAndDateGreaterThanEqual(user, goal.getStartDate());

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (Transaction t : transactions) {
            if (t.getType() == CategoryType.INCOME) {
                totalIncome = totalIncome.add(t.getAmount());
            } else if (t.getType() == CategoryType.EXPENSE) {
                totalExpenses = totalExpenses.add(t.getAmount());
            }
        }

        BigDecimal currentProgress = totalIncome.subtract(totalExpenses).setScale(2, RoundingMode.HALF_UP);
        BigDecimal targetAmount = goal.getTargetAmount().setScale(2, RoundingMode.HALF_UP);

        BigDecimal progressPercentage = BigDecimal.ZERO;
        if (targetAmount.compareTo(BigDecimal.ZERO) > 0) {
            progressPercentage = currentProgress
                    .multiply(BigDecimal.valueOf(100))
                    .divide(targetAmount, 2, RoundingMode.HALF_UP);
        }

        BigDecimal remainingAmount = targetAmount.subtract(currentProgress).setScale(2, RoundingMode.HALF_UP);

        return new GoalResponse(
                goal.getId(),
                goal.getGoalName(),
                targetAmount,
                goal.getTargetDate(),
                goal.getStartDate(),
                currentProgress,
                progressPercentage,
                remainingAmount
        );
    }
}
