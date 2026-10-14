package com.moneta.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDate;

public class UpdateTransactionRequest {

    @DecimalMin(value = "0.01", message = "Amount must be a positive decimal value")
    private BigDecimal amount;

    private String description;

    private String category;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    public UpdateTransactionRequest() {
    }

    public UpdateTransactionRequest(BigDecimal amount, String description, String category) {
        this.amount = amount;
        this.description = description;
        this.category = category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
