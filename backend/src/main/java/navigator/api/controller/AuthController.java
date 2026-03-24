package navigator.api.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import navigator.api.GlobalResponse;
import navigator.api.auth.AuthCookieSupport;
import navigator.api.auth.CurrentUserHolder;
import navigator.api.dto.AuthMeData;
import navigator.api.dto.AuthUserData;
import navigator.api.dto.LoginRequest;
import navigator.api.dto.RegisterRequest;
import navigator.application.AuthApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthApplicationService authApplicationService;

    public AuthController(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    @PostMapping("/register")
    public GlobalResponse<AuthUserData> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletResponse response) {
        AuthApplicationService.AuthResult result = authApplicationService.register(request.getUsername(), request.getPassword());
        writeCookie(response, result.rawToken(), false);
        return GlobalResponse.ok(result.user());
    }

    @PostMapping("/login")
    public GlobalResponse<AuthUserData> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletResponse response) {
        AuthApplicationService.AuthResult result = authApplicationService.login(request.getUsername(), request.getPassword());
        writeCookie(response, result.rawToken(), false);
        return GlobalResponse.ok(result.user());
    }

    @PostMapping("/logout")
    public GlobalResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String rawToken = readCookie(request);
        authApplicationService.logout(rawToken);
        writeCookie(response, "", true);
        return GlobalResponse.ok(null);
    }

    @GetMapping("/me")
    public GlobalResponse<AuthMeData> me() {
        return GlobalResponse.ok(authApplicationService.me(CurrentUserHolder.get()));
    }

    private void writeCookie(HttpServletResponse response, String value, boolean clear) {
        Cookie cookie = new Cookie(AuthCookieSupport.COOKIE_NAME, value);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(clear ? 0 : AuthCookieSupport.COOKIE_MAX_AGE_SECONDS);
        response.addCookie(cookie);
    }

    private String readCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookie != null && AuthCookieSupport.COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
