package navigator.infrastructure.persistence.repository;

import navigator.domain.model.DiagnosisAnswer;

import java.util.List;

public interface DiagnosisAnswerRepository {

    void saveAnswers(Long diagnosisSessionId, List<DiagnosisAnswer> answers);
}

