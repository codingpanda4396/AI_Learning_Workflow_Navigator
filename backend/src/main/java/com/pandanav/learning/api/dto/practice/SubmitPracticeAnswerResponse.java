package com.pandanav.learning.api.dto.practice;

public record SubmitPracticeAnswerResponse(
    PracticeSubmissionResponse submission,
    PracticeItemResponse practiceItem,
    PracticeJudgementResponse judgement
) {
}
