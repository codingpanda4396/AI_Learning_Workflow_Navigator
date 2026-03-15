package com.pandanav.learning.application.service.learningplan;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class ConceptDisplayTitleMapper {

    public String toDisplayTitle(String rawTitle) {
        if (rawTitle == null || rawTitle.isBlank()) {
            return "理解当前知识点的基本结构";
        }
        String text = rawTitle.trim();
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.startsWith("foundation of ")) {
            String concept = text.substring("Foundation of ".length()).trim();
            if (!concept.isEmpty()) {
                return "理解" + concept + "的基本结构";
            }
        }
        return text;
    }
}
