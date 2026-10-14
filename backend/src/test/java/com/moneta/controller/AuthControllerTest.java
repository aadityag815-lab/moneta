package com.moneta.controller;

import com.moneta.dto.LoginRequest;
import com.moneta.dto.MessageResponse;
import com.moneta.dto.RegisterRequest;
import com.moneta.dto.RegisterResponse;
import com.moneta.dto.UserSummaryResponse;
import com.moneta.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private AuthController authController;

    @Test
    void register() {
        RegisterRequest req = new RegisterRequest("user@example.com", "pass123", "User", "12345");
        when(authService.register(req)).thenReturn(new RegisterResponse("User registered successfully", 1L));

        ResponseEntity<RegisterResponse> res = authController.register(req);
        assertEquals(HttpStatus.CREATED, res.getStatusCode());
        assertEquals(1L, res.getBody().getUserId());
    }

    @Test
    void login() {
        LoginRequest req = new LoginRequest("user@example.com", "pass123");
        when(authService.login(any(), any())).thenReturn(new MessageResponse("Login successful"));

        ResponseEntity<MessageResponse> res = authController.login(req, httpRequest);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals("Login successful", res.getBody().getMessage());
    }

    @Test
    void logout() {
        when(authService.logout(httpRequest)).thenReturn(new MessageResponse("Logout successful"));

        ResponseEntity<MessageResponse> res = authController.logout(httpRequest);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals("Logout successful", res.getBody().getMessage());
    }

    @Test
    void getCurrentUser() {
        when(authService.getCurrentUserProfile()).thenReturn(new UserSummaryResponse(1L, "user@example.com", "User", "12345"));

        ResponseEntity<UserSummaryResponse> res = authController.getCurrentUser();
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(1L, res.getBody().getUserId());
    }
}
