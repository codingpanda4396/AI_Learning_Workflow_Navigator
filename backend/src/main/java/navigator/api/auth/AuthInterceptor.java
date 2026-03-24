package navigator.api.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import navigator.application.auth.AuthTokenService;
import navigator.infrastructure.persistence.entity.UserEntity;
import navigator.infrastructure.persistence.entity.UserSessionEntity;
import navigator.infrastructure.persistence.repository.UserRepository;
import navigator.infrastructure.persistence.repository.UserSessionRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository;

    public AuthInterceptor(UserSessionRepository userSessionRepository,
                           UserRepository userRepository) {
        this.userSessionRepository = userSessionRepository;
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        CurrentUserHolder.clear();
        String token = readCookie(request, AuthCookieSupport.COOKIE_NAME);
        if (token == null || token.isBlank()) {
            return true;
        }
        UserSessionEntity session = userSessionRepository.findActiveByTokenHash(AuthTokenService.sha256(token));
        if (session == null) {
            return true;
        }
        UserEntity user = userRepository.findById(session.getUserId());
        if (user == null) {
            return true;
        }
        userSessionRepository.touch(session.getId());
        CurrentUserHolder.set(new CurrentUser(user.getId(), user.getUsername(), user.getDisplayName()));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUserHolder.clear();
    }

    private String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookie != null && name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
