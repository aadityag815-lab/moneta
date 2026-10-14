package com.moneta.service;

import com.moneta.dto.LoginRequest;
import com.moneta.dto.MessageResponse;
import com.moneta.dto.RegisterRequest;
import com.moneta.dto.RegisterResponse;
import com.moneta.dto.UserSummaryResponse;
import com.moneta.entity.User;
import com.moneta.exception.ConflictException;
import com.moneta.exception.UnauthorizedException;
import com.moneta.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private HttpSession httpSession;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void register_Success() {
        RegisterRequest request = new RegisterRequest("user@example.com", "password123", "John Doe", "+1234567890");

        when(userRepository.existsByUsernameIgnoreCase("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        User savedUser = new User("user@example.com", "encodedPassword", "John Doe", "+1234567890");
        savedUser.setId(1L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        RegisterResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("User registered successfully", response.getMessage());
        assertEquals(1L, response.getUserId());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_DuplicateUsername_ThrowsConflictException() {
        RegisterRequest request = new RegisterRequest("user@example.com", "password123", "John Doe", "+1234567890");
        when(userRepository.existsByUsernameIgnoreCase("user@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest("user@example.com", "password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(httpServletRequest.getSession(true)).thenReturn(httpSession);

        MessageResponse response = authService.login(request, httpServletRequest);

        assertNotNull(response);
        assertEquals("Login successful", response.getMessage());
        verify(httpSession).setAttribute(anyString(), any());
    }

    @Test
    void login_InvalidCredentials_ThrowsUnauthorizedException() {
        LoginRequest request = new LoginRequest("user@example.com", "wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(UnauthorizedException.class, () -> authService.login(request, httpServletRequest));
    }

    @Test
    void logout_InvalidatesSession() {
        when(httpServletRequest.getSession(false)).thenReturn(httpSession);

        MessageResponse response = authService.logout(httpServletRequest);

        assertEquals("Logout successful", response.getMessage());
        verify(httpSession).invalidate();
    }

    @Test
    void getCurrentAuthenticatedUser_Success() {
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("user@example.com");
        when(authentication.getName()).thenReturn("user@example.com");
        SecurityContextHolder.setContext(securityContext);

        User user = new User("user@example.com", "pass", "John Doe", "1234");
        user.setId(1L);
        when(userRepository.findByUsernameIgnoreCase("user@example.com")).thenReturn(Optional.of(user));

        User result = authService.getCurrentAuthenticatedUser();

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("user@example.com", result.getUsername());
    }

    @Test
    void getCurrentAuthenticatedUser_Unauthenticated_ThrowsUnauthorizedException() {
        assertThrows(UnauthorizedException.class, () -> authService.getCurrentAuthenticatedUser());
    }

    @Test
    void getCurrentUserProfile_Success() {
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("user@example.com");
        when(authentication.getName()).thenReturn("user@example.com");
        SecurityContextHolder.setContext(securityContext);

        User user = new User("user@example.com", "pass", "John Doe", "1234");
        user.setId(1L);
        when(userRepository.findByUsernameIgnoreCase("user@example.com")).thenReturn(Optional.of(user));

        UserSummaryResponse response = authService.getCurrentUserProfile();

        assertNotNull(response);
        assertEquals(1L, response.getUserId());
        assertEquals("user@example.com", response.getUsername());
        assertEquals("John Doe", response.getFullName());
    }
}
