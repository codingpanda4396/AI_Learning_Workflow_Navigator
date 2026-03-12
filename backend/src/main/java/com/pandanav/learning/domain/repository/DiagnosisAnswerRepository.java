package com.pandanav.learning.domain.repository;

import com.pandanav.learning.domain.model.DiagnosisAnswer;

import java.util.List;

public interface DiagnosisAnswerRepository {

    void saveAll(List<DiagnosisAnswer> answers);
}
