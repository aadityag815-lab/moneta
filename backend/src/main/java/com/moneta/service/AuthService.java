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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.getUsername().trim())) {
            throw new ConflictException("User already exists with username: " + request.getUsername());
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User(
                request.getUsername().trim().toLowerCase(),
                encodedPassword,
                request.getFullName().trim(),
                request.getPhoneNumber().trim()
        );

        User savedUser = userRepository.save(user);
        return new RegisterResponse("User registered successfully", savedUser.getId());
    }

    public MessageResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername().trim().toLowerCase(),
                            request.getPassword()
                    )
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            HttpSession session = httpRequest.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

            return new MessageResponse("Login successful");
        } catch (BadCredentialsException ex) {
            throw new UnauthorizedException("Invalid username or password");
        }
    }

    public MessageResponse logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return new MessageResponse("Logout successful");
    }

    @Transactional(readOnly = true)
    public User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedException("User is not authenticated");
        }

        String username = authentication.getName();
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UnauthorizedException("User not found: " + username));
    }

    @Transactional(readOnly = true)
    public UserSummaryResponse getCurrentUserProfile() {
        User user = getCurrentAuthenticatedUser();
        return new UserSummaryResponse(user.getId(), user.getUsername(), user.getFullName(), user.getPhoneNumber());
    }
}
