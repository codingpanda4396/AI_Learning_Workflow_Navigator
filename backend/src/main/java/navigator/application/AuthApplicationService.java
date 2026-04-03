package navigator.application;

import navigator.api.BusinessErrorCode;
import navigator.api.BusinessException;
import navigator.api.auth.CurrentUser;
import navigator.api.dto.AuthMeData;
import navigator.api.dto.AuthUserData;
import navigator.api.dto.RecentLearningEntryData;
import navigator.application.auth.AuthTokenService;
import navigator.infrastructure.persistence.entity.DiagnosisSessionEntity;
import navigator.infrastructure.persistence.entity.LearningPlanEntity;
import navigator.infrastructure.persistence.entity.LearningSessionEntity;
import navigator.infrastructure.persistence.entity.UserEntity;
import navigator.infrastructure.persistence.entity.UserSessionEntity;
import navigator.infrastructure.persistence.entity.SessionTaskEntity;
import navigator.infrastructure.persistence.repository.DiagnosisSessionRepository;
import navigator.infrastructure.persistence.repository.LearningPlanRepository;
import navigator.infrastructure.persistence.repository.LearningSessionRepository;
import navigator.infrastructure.persistence.repository.SessionTaskRepository;
import navigator.infrastructure.persistence.repository.UserRepository;
import navigator.infrastructure.persistence.repository.UserSessionRepository;
import navigator.domain.enums.LearningSessionStatus;
import navigator.domain.enums.LearningSessionStatusSupport;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthApplicationService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final LearningSessionRepository learningSessionRepository;
    private final LearningPlanRepository learningPlanRepository;
    private final DiagnosisSessionRepository diagnosisSessionRepository;
    private final SessionTaskRepository sessionTaskRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthApplicationService(UserRepository userRepository,
                                  UserSessionRepository userSessionRepository,
                                  LearningSessionRepository learningSessionRepository,
                                  LearningPlanRepository learningPlanRepository,
                                  DiagnosisSessionRepository diagnosisSessionRepository,
                                  SessionTaskRepository sessionTaskRepository) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.learningSessionRepository = learningSessionRepository;
        this.learningPlanRepository = learningPlanRepository;
        this.diagnosisSessionRepository = diagnosisSessionRepository;
        this.sessionTaskRepository = sessionTaskRepository;
    }

    public AuthResult register(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        validatePassword(password);
        if (userRepository.findByUsername(normalizedUsername) != null) {
            throw new BusinessException(BusinessErrorCode.USERNAME_ALREADY_EXISTS, "username already exists");
        }
        LocalDateTime now = LocalDateTime.now();
        UserEntity user = new UserEntity();
        user.setUsername(normalizedUsername);
        user.setDisplayName(normalizedUsername);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userRepository.save(user);
        return createSessionResult(user);
    }

    public AuthResult login(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        UserEntity user = userRepository.findByUsername(normalizedUsername);
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(BusinessErrorCode.INVALID_CREDENTIALS, "invalid username or password");
        }
        return createSessionResult(user);
    }

    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        userSessionRepository.deleteByTokenHash(AuthTokenService.sha256(rawToken));
    }

    public AuthMeData me(CurrentUser currentUser) {
        if (currentUser == null) {
            return AuthMeData.builder().authenticated(false).build();
        }
        RecentLearningEntryData recent = buildRecentLearningEntry(currentUser.id());
        return AuthMeData.builder()
                .authenticated(true)
                .user(AuthUserData.builder()
                        .id(currentUser.id())
                        .username(currentUser.username())
                        .displayName(currentUser.displayName())
                        .build())
                .recentLearningEntry(recent)
                .build();
    }

    private AuthResult createSessionResult(UserEntity user) {
        String token = AuthTokenService.newToken();
        LocalDateTime now = LocalDateTime.now();
        UserSessionEntity session = new UserSessionEntity();
        session.setUserId(user.getId());
        session.setTokenHash(AuthTokenService.sha256(token));
        session.setCreatedAt(now);
        session.setLastAccessedAt(now);
        session.setExpiresAt(now.plusDays(7));
        userSessionRepository.save(session);
        return new AuthResult(
                AuthUserData.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .displayName(user.getDisplayName())
                        .build(),
                token
        );
    }

    private RecentLearningEntryData buildRecentLearningEntry(Long userId) {
        LearningSessionEntity session = learningSessionRepository.findLatestByUserId(userId);
        if (session == null || session.getGoalId() == null) {
            return null;
        }
        LearningPlanEntity plan = learningPlanRepository.findBySessionId(session.getId());
        DiagnosisSessionEntity diagnosis = diagnosisSessionRepository.findBySessionId(session.getId());
        List<SessionTaskEntity> tasks = sessionTaskRepository.findBySessionId(session.getId());
        boolean reportReady = isReportReady(session);
        String currentTaskCode = null;
        if (!reportReady && tasks != null && !tasks.isEmpty()) {
            int index = session.getCompletedTaskCount() != null ? session.getCompletedTaskCount() : 0;
            if (index >= 0 && index < tasks.size()) {
                currentTaskCode = tasks.get(index).getTaskCode();
            }
        }
        String sessionStatus = resolveFlowStatus(session, reportReady);
        return RecentLearningEntryData.builder()
                .goalId("goal_" + session.getGoalId())
                .diagnosisId(diagnosis != null ? "diag_" + diagnosis.getId() : null)
                .planId(plan != null ? "plan_" + plan.getId() : null)
                .sessionId("learn_session_" + session.getId())
                .currentTaskId(currentTaskCode)
                .sessionStatus(sessionStatus)
                .build();
    }

    private boolean isReportReady(LearningSessionEntity session) {
        int totalTasks = session.getTotalTaskCount() != null ? session.getTotalTaskCount() : 0;
        int completedTasks = session.getCompletedTaskCount() != null ? session.getCompletedTaskCount() : 0;
        return LearningSessionStatusSupport.isReportReady(session.getStatus(), completedTasks, totalTasks);
    }

    private String resolveFlowStatus(LearningSessionEntity session, boolean reportReady) {
        if (reportReady) {
            return LearningSessionStatus.REPORT_READY.name();
        }
        if (session.getPlanId() != null) {
            return LearningSessionStatus.TASK_ACTIVE.name();
        }
        if (session.getDiagnosisSessionId() != null && LearningSessionStatusSupport.isDiagnosisCompleted(session.getStatus())) {
            return "PLAN_ACTIVE";
        }
        return "DIAGNOSIS_ACTIVE";
    }

    private String normalizeUsername(String username) {
        String value = username == null ? "" : username.trim().toLowerCase();
        if (value.length() < 3 || value.length() > 32 || !value.matches("[a-z0-9_]+")) {
            throw new BusinessException(BusinessErrorCode.INVALID_ARGUMENT, "username must be 3-32 chars: a-z, 0-9, _");
        }
        return value;
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 6) {
            throw new BusinessException(BusinessErrorCode.INVALID_ARGUMENT, "password must be at least 6 characters");
        }
    }

    public record AuthResult(AuthUserData user, String rawToken) {
    }
}
