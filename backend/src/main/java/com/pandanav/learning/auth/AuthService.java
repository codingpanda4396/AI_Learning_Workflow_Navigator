package com.pandanav.learning.auth;

import com.pandanav.learning.domain.model.AppUser;
import com.pandanav.learning.domain.repository.AppUserRepository;
import com.pandanav.learning.infrastructure.exception.BadRequestException;
import com.pandanav.learning.infrastructure.exception.ConflictException;
import com.pandanav.learning.infrastructure.exception.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(AppUserRepository appUserRepository, JwtUtil jwtUtil) {
        this.appUserRepository = appUserRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public AppUser register(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        String normalizedPassword = normalizePassword(password);
        AppUser user = new AppUser();
        user.setUsername(normalizedUsername);
        user.setPasswordHash(passwordEncoder.encode(normalizedPassword));
        try {
            return appUserRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Username already exists.");
        }
    }

    public LoginResult login(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        String normalizedPassword = normalizePassword(password);
        AppUser user = appUserRepository.findByUsername(normalizedUsername)
            .orElseThrow(() -> new NotFoundException("User not found."));
        if (!passwordEncoder.matches(normalizedPassword, user.getPasswordHash())) {
            throw new BadRequestException("Invalid username or password.");
        }
        appUserRepository.updateLastLoginAt(user.getId());
        String token = jwtUtil.generateToken(user.getId());
        return new LoginResult(token, user);
    }

    public AppUser getCurrentUser(Long userId) {
        return appUserRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found."));
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new BadRequestException("username is required.");
        }
        String normalized = username.trim();
        if (normalized.length() > 64) {
            throw new BadRequestException("username exceeds max length.");
        }
        return normalized;
    }

    private String normalizePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new BadRequestException("password is required.");
        }
        String normalized = password.trim();
        if (normalized.length() < 6) {
            throw new BadRequestException("password must be at least 6 characters.");
        }
        if (normalized.length() > 128) {
            throw new BadRequestException("password exceeds max length.");
        }
        return normalized;
    }

    public record LoginResult(String token, AppUser user) {
    }
}
