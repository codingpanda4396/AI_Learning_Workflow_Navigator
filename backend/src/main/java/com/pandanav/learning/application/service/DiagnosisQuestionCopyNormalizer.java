package com.pandanav.learning.application.service;

import com.pandanav.learning.api.contract.ContractCatalog;
import com.pandanav.learning.domain.model.DiagnosisQuestion;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DiagnosisQuestionCopyNormalizer {

    public List<DiagnosisQuestion> normalize(List<DiagnosisQuestion> questions) {
        if (questions == null || questions.isEmpty()) {
            return questions == null ? List.of() : questions;
        }
        return questions.stream().map(this::normalizeQuestion).toList();
    }

    private DiagnosisQuestion normalizeQuestion(DiagnosisQuestion question) {
        String type = ContractCatalog.diagnosisQuestionTypeCode(question.type());
        return switch (type) {
            case "SINGLE_CHOICE" -> normalizeSingleChoice(question);
            case "MULTIPLE_CHOICE" -> normalizeMultipleChoice(question);
            case "TEXT" -> normalizeText(question);
            default -> question;
        };
    }

    private DiagnosisQuestion normalizeSingleChoice(DiagnosisQuestion question) {
        String title = stripBannedWords(question.title());
        String description = appendIfMissing(stripBannedWords(question.description()), "请选择最符合的一项。");
        return new DiagnosisQuestion(
            question.questionId(),
            question.dimension(),
            question.type(),
            question.required(),
            question.options(),
            title,
            description,
            "",
            question.submitHint(),
            question.sectionLabel()
        );
    }

    private DiagnosisQuestion normalizeMultipleChoice(DiagnosisQuestion question) {
        String description = appendIfMissing(question.description(), "可多选。");
        return new DiagnosisQuestion(
            question.questionId(),
            question.dimension(),
            question.type(),
            question.required(),
            question.options(),
            question.title(),
            description,
            "",
            question.submitHint(),
            question.sectionLabel()
        );
    }

    private DiagnosisQuestion normalizeText(DiagnosisQuestion question) {
        String placeholder = question.placeholder() == null ? "" : question.placeholder().trim();
        if (placeholder.isBlank()) {
            placeholder = "请简要输入你的情况";
        }
        return new DiagnosisQuestion(
            question.questionId(),
            question.dimension(),
            question.type(),
            question.required(),
            question.options(),
            question.title(),
            question.description(),
            placeholder,
            question.submitHint(),
            question.sectionLabel()
        );
    }

    private String stripBannedWords(String copy) {
        if (copy == null || copy.isBlank()) {
            return copy == null ? "" : copy;
        }
        return copy
            .replace("说说", "请选择")
            .replace("描述", "选择")
            .replace("输入", "选择")
            .trim();
    }

    private String appendIfMissing(String copy, String suffix) {
        String safeCopy = copy == null ? "" : copy.trim();
        if (safeCopy.contains(suffix.replace("。", ""))) {
            return safeCopy;
        }
        if (safeCopy.isBlank()) {
            return suffix;
        }
        return safeCopy.endsWith("。") ? safeCopy + suffix : safeCopy + " " + suffix;
    }
}
