package com.pandanav.learning.application.service.practice;

import com.pandanav.learning.domain.enums.PracticeQuestionType;

import java.util.List;

public record PracticeDraftItem(
    PracticeQuestionType questionType,
    String stem,
    List<String> options,
    String standardAnswer,
    String explanation,
    String difficulty
) {
}
