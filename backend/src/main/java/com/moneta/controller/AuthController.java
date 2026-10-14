package com.moneta.controller;

import com.moneta.dto.LoginRequest;
import com.moneta.dto.MessageResponse;
import com.moneta.dto.RegisterRequest;
import com.moneta.dto.RegisterResponse;
import com.moneta.dto.UserSummaryResponse;
import com.moneta.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<MessageResponse> login(@Valid @RequestBody LoginRequest request,
                                                 HttpServletRequest httpRequest) {
        MessageResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(HttpServletRequest httpRequest) {
        MessageResponse response = authService.logout(httpRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserSummaryResponse> getCurrentUser() {
        UserSummaryResponse response = authService.getCurrentUserProfile();
        return ResponseEntity.ok(response);
    }
}
