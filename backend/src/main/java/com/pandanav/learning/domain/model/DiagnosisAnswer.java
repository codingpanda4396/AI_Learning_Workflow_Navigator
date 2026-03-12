package com.pandanav.learning.domain.model;

import com.pandanav.learning.domain.enums.DiagnosisDimension;

import java.time.OffsetDateTime;

public class DiagnosisAnswer {

    private Long id;
    private Long diagnosisSessionId;
    private String questionId;
    private DiagnosisDimension dimension;
    private String answerType;
    private String answerValueJson;
    private String rawText;
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDiagnosisSessionId() {
        return diagnosisSessionId;
    }

    public void setDiagnosisSessionId(Long diagnosisSessionId) {
        this.diagnosisSessionId = diagnosisSessionId;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public DiagnosisDimension getDimension() {
        return dimension;
    }

    public void setDimension(DiagnosisDimension dimension) {
        this.dimension = dimension;
    }

    public String getAnswerType() {
        return answerType;
    }

    public void setAnswerType(String answerType) {
        this.answerType = answerType;
    }

    public String getAnswerValueJson() {
        return answerValueJson;
    }

    public void setAnswerValueJson(String answerValueJson) {
        this.answerValueJson = answerValueJson;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
