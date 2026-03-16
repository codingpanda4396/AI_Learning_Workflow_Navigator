package navigator.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import navigator.domain.model.DiagnosisEvidenceSummary;
import navigator.domain.model.LearnerProfileSnapshot;
import navigator.infrastructure.persistence.entity.LearnerProfileSnapshotEntity;
import navigator.infrastructure.persistence.mapper.LearnerProfileSnapshotMapper;
import navigator.infrastructure.persistence.repository.LearnerProfileSnapshotRepository;
import navigator.infrastructure.persistence.serde.JsonSerde;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public class LearnerProfileSnapshotRepositoryImpl implements LearnerProfileSnapshotRepository {

    private final LearnerProfileSnapshotMapper mapper;
    private final JsonSerde jsonSerde;

    public LearnerProfileSnapshotRepositoryImpl(LearnerProfileSnapshotMapper mapper, JsonSerde jsonSerde) {
        this.mapper = mapper;
        this.jsonSerde = jsonSerde;
    }

    @Override
    public void saveProfile(Long diagnosisSessionId,
                            Long sessionId,
                            LearnerProfileSnapshot profile,
                            DiagnosisEvidenceSummary evidenceSummary) {
        if (diagnosisSessionId == null || sessionId == null || profile == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        LearnerProfileSnapshotEntity entity = new LearnerProfileSnapshotEntity();
        entity.setId(IdWorker.getId());
        entity.setDiagnosisSessionId(diagnosisSessionId);
        entity.setSessionId(sessionId);
        entity.setProfileJson(jsonSerde.toJson(profile));
        entity.setPlanningMode(null); // 规划模式目前由上游 GoalContextSnapshot 控制，这里暂不重复存储
        entity.setEntryStrategy(profile.getSuggestedEntryStrategy());
        entity.setEntryGranularity(profile.getSuggestedGranularity());
        entity.setFeedbackMode(profile.getSuggestedFeedbackFrequency());
        entity.setRiskTagsJson(jsonSerde.toJson(profile.getRiskTags()));
        entity.setKeyEvidenceJson(evidenceSummary != null ? jsonSerde.toJson(evidenceSummary.getKeyEvidence()) : null);
        entity.setCreatedAt(now);
        mapper.insert(entity);
    }
}

