package navigator.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import navigator.domain.model.DiagnosisAnswer;
import navigator.infrastructure.persistence.entity.DiagnosisAnswerEntity;
import navigator.infrastructure.persistence.mapper.DiagnosisAnswerMapper;
import navigator.infrastructure.persistence.repository.DiagnosisAnswerRepository;
import navigator.infrastructure.persistence.serde.JsonSerde;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class DiagnosisAnswerRepositoryImpl implements DiagnosisAnswerRepository {

    private final DiagnosisAnswerMapper mapper;
    private final JsonSerde jsonSerde;

    public DiagnosisAnswerRepositoryImpl(DiagnosisAnswerMapper mapper, JsonSerde jsonSerde) {
        this.mapper = mapper;
        this.jsonSerde = jsonSerde;
    }

    @Override
    public void saveAnswers(Long diagnosisSessionId, List<DiagnosisAnswer> answers) {
        if (diagnosisSessionId == null || answers == null || answers.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (DiagnosisAnswer answer : answers) {
            DiagnosisAnswerEntity entity = new DiagnosisAnswerEntity();
            entity.setId(IdWorker.getId());
            entity.setDiagnosisSessionId(diagnosisSessionId);
            entity.setQuestionId(answer.getQuestionId());
            entity.setAnswerJson(jsonSerde.toJson(answer));
            entity.setCreatedAt(now);
            mapper.insert(entity);
        }
    }
}

