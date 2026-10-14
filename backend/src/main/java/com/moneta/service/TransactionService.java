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
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;
    private final AuthService authService;

    public TransactionService(TransactionRepository transactionRepository,
                              CategoryService categoryService,
                              AuthService authService) {
        this.transactionRepository = transactionRepository;
        this.categoryService = categoryService;
        this.authService = authService;
    }

    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        User currentUser = authService.getCurrentAuthenticatedUser();

        validateTransactionRequest(request);

        Category category = categoryService.findAccessibleCategoryByName(request.getCategory(), currentUser);

        Transaction transaction = new Transaction(
                request.getAmount(),
                request.getDate(),
                category.getName(),
                category.getType(),
                request.getDescription(),
                category,
                currentUser
        );

        Transaction saved = transactionRepository.save(transaction);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public TransactionListResponse getTransactions(LocalDate startDate,
                                                   LocalDate endDate,
                                                   String category,
                                                   Long categoryId,
                                                   String typeStr) {
        User currentUser = authService.getCurrentAuthenticatedUser();

        final CategoryType type;
        if (typeStr != null && !typeStr.trim().isEmpty()) {
            try {
                type = CategoryType.valueOf(typeStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid transaction type: " + typeStr);
            }
        } else {
            type = null;
        }

        Specification<Transaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user"), currentUser));

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("date"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("date"), endDate));
            }
            if (category != null && !category.trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("category")), category.trim().toLowerCase()));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryEntity").get("id"), categoryId));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }

            query.orderBy(cb.desc(root.get("date")), cb.desc(root.get("id")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Transaction> transactions = transactionRepository.findAll(spec);

        List<TransactionResponse> responses = new ArrayList<>();
        for (Transaction t : transactions) {
            responses.add(mapToResponse(t));
        }

        return new TransactionListResponse(responses);
    }

    @Transactional
    public TransactionResponse updateTransaction(Long id, UpdateTransactionRequest request) {
        User currentUser = authService.getCurrentAuthenticatedUser();

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        if (!transaction.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Cannot modify another user's transaction");
        }

        if (request.getDate() != null && !request.getDate().equals(transaction.getDate())) {
            throw new BadRequestException("Transaction date cannot be modified");
        }

        if (request.getAmount() != null) {
            if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Amount must be a positive decimal value");
            }
            transaction.setAmount(request.getAmount());
        }

        if (request.getDescription() != null) {
            transaction.setDescription(request.getDescription());
        }

        if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
            Category category = categoryService.findAccessibleCategoryByName(request.getCategory(), currentUser);
            transaction.setCategory(category.getName());
            transaction.setCategoryEntity(category);
            transaction.setType(category.getType());
        }

        Transaction updated = transactionRepository.save(transaction);
        return mapToResponse(updated);
    }

    @Transactional
    public MessageResponse deleteTransaction(Long id) {
        User currentUser = authService.getCurrentAuthenticatedUser();

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        if (!transaction.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Cannot delete another user's transaction");
        }

        transactionRepository.delete(transaction);
        return new MessageResponse("Transaction deleted successfully");
    }

    private void validateTransactionRequest(TransactionRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be a positive decimal value");
        }
        if (request.getDate() == null) {
            throw new BadRequestException("Date is required");
        }
        if (request.getDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Date cannot be a future date");
        }
        if (request.getCategory() == null || request.getCategory().trim().isEmpty()) {
            throw new BadRequestException("Category is required");
        }
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getDate(),
                transaction.getCategory(),
                transaction.getDescription(),
                transaction.getType().name()
        );
    }
}
