package com.pandanav.learning.application.service;

import com.pandanav.learning.api.contract.ContractCatalog;
import com.pandanav.learning.api.dto.diagnosis.CapabilityProfileDto;
import com.pandanav.learning.api.dto.diagnosis.CapabilityProfileResponse;
import com.pandanav.learning.api.dto.diagnosis.DiagnosisInsightsDto;
import com.pandanav.learning.domain.model.CapabilityProfile;
import com.pandanav.learning.domain.model.CapabilityProfileContext;
import com.pandanav.learning.domain.repository.CapabilityProfileRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.infrastructure.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CapabilityProfileQueryService {

    private final SessionRepository sessionRepository;
    private final CapabilityProfileRepository capabilityProfileRepository;

    public CapabilityProfileQueryService(
        SessionRepository sessionRepository,
        CapabilityProfileRepository capabilityProfileRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.capabilityProfileRepository = capabilityProfileRepository;
    }

    public CapabilityProfileResponse getLatestProfile(Long sessionId, Long userId) {
        sessionRepository.findByIdAndUserPk(sessionId, userId)
            .orElseThrow(() -> new NotFoundException("Learning session not found."));
        CapabilityProfile profile = capabilityProfileRepository.findLatestBySessionId(sessionId)
            .orElseThrow(() -> new NotFoundException("Capability profile not found."));
        return new CapabilityProfileResponse(sessionId, toDto(profile), new DiagnosisInsightsDto(profile.getSummaryText(), profile.getPlanExplanation()));
    }

    public CapabilityProfileContext getContextForSession(Long sessionId, Long userId) {
        sessionRepository.findByIdAndUserPk(sessionId, userId)
            .orElseThrow(() -> new NotFoundException("Learning session not found."));
        CapabilityProfile profile = capabilityProfileRepository.findLatestBySessionId(sessionId)
            .orElseThrow(() -> new NotFoundException("Capability profile not found."));
        // TODO: path plan / tutor / practice can consume this structured context directly in a later incremental change.
        return new CapabilityProfileContext(
            sessionId,
            profile.getCurrentLevel().name(),
            profile.getGoalOrientation(),
            profile.getTimeBudget(),
            profile.getLearningPreference(),
            profile.getSummaryText()
        );
    }

    private CapabilityProfileDto toDto(CapabilityProfile profile) {
        return new CapabilityProfileDto(
            ContractCatalog.capabilityLevel(profile.getCurrentLevel()),
            profile.getStrengths(),
            profile.getWeaknesses(),
            ContractCatalog.learningPreference(profile.getLearningPreference()),
            ContractCatalog.timeBudget(profile.getTimeBudget()),
            ContractCatalog.goalOrientation(profile.getGoalOrientation())
        );
    }
}
