package com.pandanav.learning.application.service;

import com.pandanav.learning.api.dto.diagnosis.CreateDiagnosisSessionResponse;
import com.pandanav.learning.api.dto.diagnosis.DiagnosisDecisionHintsDto;
import com.pandanav.learning.api.dto.diagnosis.DiagnosisExplanationDto;
import com.pandanav.learning.api.dto.diagnosis.DiagnosisFallbackDto;
import com.pandanav.learning.api.dto.diagnosis.DiagnosisMetadataDto;
import com.pandanav.learning.api.dto.diagnosis.DiagnosisNextActionDto;
import com.pandanav.learning.api.dto.diagnosis.DiagnosisQuestionDto;
import com.pandanav.learning.api.dto.diagnosis.DiagnosisStrategyDto;
import com.pandanav.learning.api.dto.diagnosis.LearnerSnapshotDto;
import com.pandanav.learning.api.dto.diagnosis.PersonalizationMetaDto;
import com.pandanav.learning.api.dto.diagnosis.QuestionRationaleDto;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Assembles CreateDiagnosisSessionResponse from saved session and built DTOs.
 */
@Component
public class DiagnosisResponseAssembler {

    public CreateDiagnosisSessionResponse assemble(
        Long diagnosisId,
        Long sessionId,
        String status,
        String generationMode,
        List<DiagnosisQuestionDto> questions,
        DiagnosisExplanationDto diagnosisExplanation,
        DiagnosisNextActionDto nextAction,
        DiagnosisDecisionHintsDto decisionHints,
        DiagnosisFallbackDto fallback,
        DiagnosisMetadataDto metadata,
        LearnerSnapshotDto learnerSnapshot,
        DiagnosisStrategyDto diagnosisStrategy,
        List<QuestionRationaleDto> questionRationales,
        PersonalizationMetaDto personalizationMeta
    ) {
        return new CreateDiagnosisSessionResponse(
            diagnosisId,
            sessionId,
            status,
            generationMode,
            questions,
            diagnosisExplanation,
            nextAction,
            decisionHints,
            fallback,
            metadata,
            learnerSnapshot,
            diagnosisStrategy,
            questionRationales,
            personalizationMeta
        );
    }
}
