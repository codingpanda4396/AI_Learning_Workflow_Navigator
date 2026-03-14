package com.pandanav.learning.application.service;

import com.pandanav.learning.api.dto.diagnosis.CapabilityProfileResponse;
import com.pandanav.learning.domain.enums.CapabilityLevel;
import com.pandanav.learning.domain.model.CapabilityProfile;
import com.pandanav.learning.domain.model.CapabilityProfileContext;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.repository.CapabilityProfileRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CapabilityProfileQueryServiceTest {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private CapabilityProfileRepository capabilityProfileRepository;

    @Test
    void shouldReturnLatestProfile() {
        LearningSession session = new LearningSession();
        session.setId(88L);
        session.setUserPk(10L);

        CapabilityProfile profile = new CapabilityProfile();
        profile.setLearningSessionId(88L);
        profile.setCurrentLevel(CapabilityLevel.BEGINNER);
        profile.setStrengths(List.of("目标明确"));
        profile.setWeaknesses(List.of("基础还不稳定"));
        profile.setLearningPreference("PRACTICE_FIRST");
        profile.setTimeBudget("每周 4-6 小时");
        profile.setGoalOrientation("INTERVIEW");
        profile.setSummaryText("用户当前基础偏弱，但目标明确。");
        profile.setPlanExplanation("系统会先补基础，再逐步增加训练。");
        profile.setVersion(2);

        when(sessionRepository.findByIdAndUserPk(88L, 10L)).thenReturn(Optional.of(session));
        when(capabilityProfileRepository.findLatestBySessionId(88L)).thenReturn(Optional.of(profile));

        CapabilityProfileQueryService service = new CapabilityProfileQueryService(sessionRepository, capabilityProfileRepository);

        CapabilityProfileResponse response = service.getLatestProfile(88L, 10L);
        CapabilityProfileContext context = service.getContextForSession(88L, 10L);

        assertEquals("BEGINNER", response.capabilityProfile().currentLevel());
        assertEquals("INTERVIEW", response.capabilityProfile().goalOrientation());
        assertEquals("每周 4-6 小时", context.timeBudget());
        assertEquals("PRACTICE_FIRST", context.learningPreference());
        assertEquals("系统会先补基础，再逐步增加训练。", response.insights().planExplanation());
    }
}
