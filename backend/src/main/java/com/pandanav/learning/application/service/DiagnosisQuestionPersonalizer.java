package com.pandanav.learning.application.service;

import com.pandanav.learning.domain.model.DiagnosisQuestion;
import com.pandanav.learning.domain.model.PlanningContext;

import java.util.List;

public interface DiagnosisQuestionPersonalizer {
    List<DiagnosisQuestion> personalize(List<DiagnosisQuestion> baseQuestions, PlanningContext context);
}
