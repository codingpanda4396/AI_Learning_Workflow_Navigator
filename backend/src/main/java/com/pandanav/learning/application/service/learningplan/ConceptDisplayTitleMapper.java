package com.pandanav.learning.application.service.learningplan;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class ConceptDisplayTitleMapper {

    private static final String FOUNDATION_OF_PREFIX = "foundation of ";

    /** 统一转为产品化中文标题，禁止向用户暴露 "Foundation of X" 等内部格式。 */
    public String toDisplayTitle(String rawTitle) {
        if (rawTitle == null || rawTitle.isBlank()) {
            return "理解当前知识点的基本结构";
        }
        String text = rawTitle.trim();
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.startsWith(FOUNDATION_OF_PREFIX)) {
            String concept = text.substring(FOUNDATION_OF_PREFIX.length()).trim();
            if (!concept.isEmpty()) {
                return "理解" + concept + "的基本结构";
            }
        }
        return text;
    }
}
