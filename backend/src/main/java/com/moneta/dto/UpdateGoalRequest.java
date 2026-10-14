package com.moneta.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import java.math.BigDecimal;
import java.time.LocalDate;

public class UpdateGoalRequest {

    private String goalName;

    @DecimalMin(value = "0.01", message = "Target amount must be a positive decimal value")
    private BigDecimal targetAmount;

    @Future(message = "Target date must be a future date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate targetDate;

    public UpdateGoalRequest() {
    }

    public UpdateGoalRequest(BigDecimal targetAmount, LocalDate targetDate) {
        this.targetAmount = targetAmount;
        this.targetDate = targetDate;
    }

    public String getGoalName() {
        return goalName;
    }

    public void setGoalName(String goalName) {
        this.goalName = goalName;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(BigDecimal targetAmount) {
        this.targetAmount = targetAmount;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }
}
