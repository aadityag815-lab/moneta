package com.moneta.controller;

import com.moneta.dto.GoalListResponse;
import com.moneta.dto.GoalRequest;
import com.moneta.dto.GoalResponse;
import com.moneta.dto.MessageResponse;
import com.moneta.dto.UpdateGoalRequest;
import com.moneta.service.GoalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping
    public ResponseEntity<GoalResponse> createGoal(@Valid @RequestBody GoalRequest request) {
        GoalResponse response = goalService.createGoal(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<GoalListResponse> getAllGoals() {
        GoalListResponse response = goalService.getAllGoals();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GoalResponse> getGoalById(@PathVariable("id") Long id) {
        GoalResponse response = goalService.getGoalById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoalResponse> updateGoal(@PathVariable("id") Long id,
                                                   @Valid @RequestBody UpdateGoalRequest request) {
        GoalResponse response = goalService.updateGoal(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteGoal(@PathVariable("id") Long id) {
        MessageResponse response = goalService.deleteGoal(id);
        return ResponseEntity.ok(response);
    }
}
