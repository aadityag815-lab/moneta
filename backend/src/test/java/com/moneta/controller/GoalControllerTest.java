package com.moneta.controller;

import com.moneta.dto.GoalListResponse;
import com.moneta.dto.GoalRequest;
import com.moneta.dto.GoalResponse;
import com.moneta.dto.MessageResponse;
import com.moneta.dto.UpdateGoalRequest;
import com.moneta.service.GoalService;
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
class GoalControllerTest {

    @Mock
    private GoalService goalService;

    @InjectMocks
    private GoalController goalController;

    @Test
    void createGoal() {
        GoalRequest req = new GoalRequest("Emergency Fund", new BigDecimal("5000.00"), LocalDate.now().plusMonths(6), null);
        when(goalService.createGoal(req))
                .thenReturn(new GoalResponse(1L, "Emergency Fund", new BigDecimal("5000.00"), LocalDate.now().plusMonths(6), LocalDate.now(), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("5000.00")));

        ResponseEntity<GoalResponse> res = goalController.createGoal(req);
        assertEquals(HttpStatus.CREATED, res.getStatusCode());
        assertEquals("Emergency Fund", res.getBody().getGoalName());
    }

    @Test
    void getAllGoals() {
        when(goalService.getAllGoals()).thenReturn(new GoalListResponse(Collections.emptyList()));

        ResponseEntity<GoalListResponse> res = goalController.getAllGoals();
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void getGoalById() {
        when(goalService.getGoalById(1L))
                .thenReturn(new GoalResponse(1L, "Emergency Fund", new BigDecimal("5000.00"), LocalDate.now().plusMonths(6), LocalDate.now(), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("5000.00")));

        ResponseEntity<GoalResponse> res = goalController.getGoalById(1L);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void updateGoal() {
        UpdateGoalRequest req = new UpdateGoalRequest(new BigDecimal("6000.00"), LocalDate.now().plusMonths(12));
        when(goalService.updateGoal(eq(1L), any()))
                .thenReturn(new GoalResponse(1L, "Emergency Fund", new BigDecimal("6000.00"), LocalDate.now().plusMonths(12), LocalDate.now(), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("6000.00")));

        ResponseEntity<GoalResponse> res = goalController.updateGoal(1L, req);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(new BigDecimal("6000.00"), res.getBody().getTargetAmount());
    }

    @Test
    void deleteGoal() {
        when(goalService.deleteGoal(1L)).thenReturn(new MessageResponse("Goal deleted successfully"));

        ResponseEntity<MessageResponse> res = goalController.deleteGoal(1L);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals("Goal deleted successfully", res.getBody().getMessage());
    }
}
