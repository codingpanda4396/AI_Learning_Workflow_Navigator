package com.pandanav.learning.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pandanav.learning.domain.model.AppUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/api/auth/register")
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        AppUser user = authService.register(request.username(), request.password());
        return new RegisterResponse(user.getId());
    }

    @PostMapping("/api/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request.username(), request.password());
        return new LoginResponse(
            result.token(),
            new UserInfo(result.user().getId(), result.user().getUsername())
        );
    }

    @GetMapping("/api/users/me")
    public UserInfo me() {
        Long userId = UserContextHolder.getRequiredUserId();
        AppUser user = authService.getCurrentUser(userId);
        return new UserInfo(user.getId(), user.getUsername());
    }

    public record RegisterRequest(
        @NotBlank
        String username,
        @NotBlank
        String password
    ) {
    }

    public record LoginRequest(
        @NotBlank
        String username,
        @NotBlank
        String password
    ) {
    }

    public record RegisterResponse(@JsonProperty("user_id") Long userId) {
    }

    public record LoginResponse(
        String token,
        UserInfo user
    ) {
    }

    public record UserInfo(Long id, String username) {
    }
}
