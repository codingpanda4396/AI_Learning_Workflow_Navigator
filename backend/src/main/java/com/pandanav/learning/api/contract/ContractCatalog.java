package com.pandanav.learning.api.contract;

import com.pandanav.learning.api.dto.CodeLabelDto;
import com.pandanav.learning.api.dto.diagnosis.DiagnosisQuestionOptionDto;
import com.pandanav.learning.domain.enums.CapabilityLevel;
import com.pandanav.learning.domain.enums.DiagnosisDimension;
import com.pandanav.learning.domain.model.DiagnosisQuestionOption;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ContractCatalog {

    private static final Map<DiagnosisDimension, List<OptionDef>> DIAGNOSIS_OPTIONS = Map.of(
        DiagnosisDimension.FOUNDATION, List.of(
            option("BEGINNER", "刚开始接触"),
            option("BASIC", "学过但还不太熟"),
            option("PROFICIENT", "基础比较熟"),
            option("ADVANCED", "已经能独立应用")
        ),
        DiagnosisDimension.EXPERIENCE, List.of(
            option("COURSEWORK", "上过相关课程"),
            option("ASSIGNMENTS", "做过作业或实验"),
            option("PROJECTS", "做过项目或作品"),
            option("EXAM_PREP", "准备过考试或面试"),
            option("NO_EXPERIENCE", "几乎没有相关经验")
        ),
        DiagnosisDimension.GOAL_STYLE, List.of(
            option("COURSE", "应对课程学习与作业"),
            option("EXAM", "准备考试或测验"),
            option("INTERVIEW", "准备实习或求职面试"),
            option("PROJECT", "完成项目或作品")
        ),
        DiagnosisDimension.TIME_BUDGET, List.of(
            option("LIGHT", "每周 1-3 小时"),
            option("STANDARD", "每周 4-6 小时"),
            option("INTENSIVE", "每周 7-10 小时"),
            option("IMMERSIVE", "每周 10 小时以上")
        ),
        DiagnosisDimension.LEARNING_PREFERENCE, List.of(
            option("CONCEPT_FIRST", "先讲清概念，再做练习"),
            option("EXAMPLE_FIRST", "先看例子，再总结方法"),
            option("PRACTICE_FIRST", "先做题，在反馈中查漏补缺"),
            option("PROJECT_DRIVEN", "边学边做项目，穿插补基础")
        )
    );

    private static final Map<String, String> QUESTION_TYPE_LABELS = Map.of(
        "single_choice", "单选",
        "multiple_choice", "多选",
        "text", "文本"
    );

    private static final Map<String, String> CAPABILITY_LEVEL_LABELS = Map.of(
        "BEGINNER", "入门",
        "INTERMEDIATE", "进阶中",
        "ADVANCED", "熟练"
    );

    private static final Map<String, String> GOAL_ORIENTATION_LABELS = Map.of(
        "COURSE", "课程学习",
        "EXAM", "考试准备",
        "INTERVIEW", "面试准备",
        "PROJECT", "项目实践"
    );

    private static final Map<String, String> LEARNING_PREFERENCE_LABELS = Map.of(
        "CONCEPT_FIRST", "先讲概念再练习",
        "EXAMPLE_FIRST", "先看例子再总结",
        "PRACTICE_FIRST", "先练再纠偏",
        "PROJECT_DRIVEN", "项目驱动"
    );

    private static final Map<String, String> TIME_BUDGET_LABELS = Map.of(
        "LIGHT", "每周 1-3 小时",
        "STANDARD", "每周 4-6 小时",
        "INTENSIVE", "每周 7-10 小时",
        "IMMERSIVE", "每周 10 小时以上"
    );

    private static final Map<String, String> NEXT_ACTION_LABELS = Map.of(
        "PATH_PLAN", "进入个性化学习路径"
    );

    private static final Map<String, String> PLAN_INTENSITY_LABELS = Map.of(
        "LIGHT", "轻量",
        "STANDARD", "标准",
        "INTENSIVE", "强化"
    );

    private static final Map<String, String> PLAN_MODE_LABELS = Map.of(
        "LEARN_THEN_PRACTICE", "先讲后练",
        "PRACTICE_DRIVEN", "以练促学",
        "MIXED", "混合推进"
    );

    private static final Map<String, String> PLAN_SOURCE_LABELS = Map.of(
        "RULE_ENGINE", "规则规划",
        "RULE_FALLBACK", "规则兜底"
    );

    private static final Map<String, String> CONTENT_SOURCE_LABELS = Map.of(
        "LLM", "AI 生成文案",
        "RULE_TEMPLATE", "规则模板文案",
        "RULE_FALLBACK", "规则兜底文案"
    );

    private static final Map<String, String> PREVIEW_STATUS_LABELS = Map.of(
        "PREVIEW_READY", "预览草稿已生成",
        "COMMITTED", "已确认并生成正式计划"
    );

    private static final Map<String, String> STAGE_LABELS = Map.of(
        "STRUCTURE", "结构梳理",
        "UNDERSTANDING", "理解深化",
        "TRAINING", "训练巩固",
        "REFLECTION", "反思迁移"
    );

    private static final Map<String, String> PATH_STATUS_LABELS = Map.of(
        "NEW", "待建立",
        "PARTIAL", "部分掌握",
        "STABLE", "相对稳定",
        "WEAK", "薄弱"
    );

    private ContractCatalog() {
    }

    public static CodeLabelDto diagnosisDimension(DiagnosisDimension dimension) {
        return new CodeLabelDto(dimension.name(), switch (dimension) {
            case FOUNDATION -> "基础掌握";
            case EXPERIENCE -> "过往经验";
            case GOAL_STYLE -> "目标导向";
            case TIME_BUDGET -> "时间预算";
            case LEARNING_PREFERENCE -> "学习偏好";
        });
    }

    public static CodeLabelDto diagnosisQuestionType(String typeCode) {
        String normalized = normalize(typeCode);
        return new CodeLabelDto(normalized, QUESTION_TYPE_LABELS.getOrDefault(normalized.toLowerCase(Locale.ROOT), normalized));
    }

    public static String diagnosisQuestionTypeCode(String typeCode) {
        return normalize(typeCode);
    }

    public static List<CodeLabelDto> diagnosisOptions(DiagnosisDimension dimension, List<String> labels) {
        List<OptionDef> optionDefs = DIAGNOSIS_OPTIONS.getOrDefault(dimension, List.of());
        Map<String, CodeLabelDto> mapped = new LinkedHashMap<>();
        if (labels != null && !labels.isEmpty()) {
            for (int i = 0; i < labels.size(); i++) {
                String label = labels.get(i);
                if (i < optionDefs.size()) {
                    OptionDef option = optionDefs.get(i);
                    mapped.put(option.code(), new CodeLabelDto(option.code(), label));
                    continue;
                }
                CodeLabelDto dto = diagnosisOption(dimension, label).orElseGet(() -> new CodeLabelDto(normalize(label), label));
                mapped.putIfAbsent(dto.code(), dto);
            }
            return mapped.values().stream().toList();
        }
        for (OptionDef option : optionDefs) {
            mapped.put(option.code(), new CodeLabelDto(option.code(), option.label()));
        }
        return mapped.values().stream().toList();
    }

    public static List<DiagnosisQuestionOption> diagnosisQuestionOptions(DiagnosisDimension dimension) {
        List<OptionDef> optionDefs = DIAGNOSIS_OPTIONS.getOrDefault(dimension, List.of());
        List<DiagnosisQuestionOption> options = new java.util.ArrayList<>();
        for (int i = 0; i < optionDefs.size(); i++) {
            OptionDef option = optionDefs.get(i);
            options.add(new DiagnosisQuestionOption(option.code(), option.label(), i + 1));
        }
        return options;
    }

    public static List<DiagnosisQuestionOptionDto> diagnosisQuestionOptions(List<DiagnosisQuestionOption> options) {
        if (options == null) {
            return List.of();
        }
        return options.stream()
            .map(option -> new DiagnosisQuestionOptionDto(option.code(), option.label(), option.order()))
            .toList();
    }

    public static Optional<CodeLabelDto> diagnosisOption(DiagnosisDimension dimension, String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        for (OptionDef option : DIAGNOSIS_OPTIONS.getOrDefault(dimension, List.of())) {
            if (option.matches(raw)) {
                return Optional.of(new CodeLabelDto(option.code(), option.label()));
            }
        }
        return Optional.empty();
    }

    public static String diagnosisOptionCode(DiagnosisDimension dimension, String raw) {
        return diagnosisOption(dimension, raw)
            .map(CodeLabelDto::code)
            .orElseGet(() -> normalize(raw));
    }

    public static String diagnosisOptionLabel(DiagnosisDimension dimension, String raw) {
        return diagnosisOption(dimension, raw)
            .map(CodeLabelDto::label)
            .orElse(raw);
    }

    public static CodeLabelDto capabilityLevel(CapabilityLevel level) {
        String code = level == null ? "" : level.name();
        return new CodeLabelDto(code, CAPABILITY_LEVEL_LABELS.getOrDefault(code, code));
    }

    public static CodeLabelDto goalOrientation(String code) {
        return labeled(code, GOAL_ORIENTATION_LABELS);
    }

    public static CodeLabelDto learningPreference(String code) {
        return labeled(code, LEARNING_PREFERENCE_LABELS);
    }

    public static CodeLabelDto timeBudget(String codeOrLabel) {
        String code = TIME_BUDGET_LABELS.containsKey(normalize(codeOrLabel))
            ? normalize(codeOrLabel)
            : diagnosisOptionCode(DiagnosisDimension.TIME_BUDGET, codeOrLabel);
        return new CodeLabelDto(code, TIME_BUDGET_LABELS.getOrDefault(code, diagnosisOptionLabel(DiagnosisDimension.TIME_BUDGET, codeOrLabel)));
    }

    public static CodeLabelDto nextAction(String code) {
        return labeled(code, NEXT_ACTION_LABELS);
    }

    public static CodeLabelDto planIntensity(String code) {
        return labeled(code, PLAN_INTENSITY_LABELS);
    }

    public static CodeLabelDto planLearningMode(String code) {
        return labeled(code, PLAN_MODE_LABELS);
    }

    public static CodeLabelDto planSource(String code) {
        return labeled(code, PLAN_SOURCE_LABELS);
    }

    public static CodeLabelDto contentSource(String code) {
        return labeled(code, CONTENT_SOURCE_LABELS);
    }

    public static CodeLabelDto previewStatus(String code) {
        return labeled(code, PREVIEW_STATUS_LABELS);
    }

    public static CodeLabelDto stage(String code) {
        return labeled(code, STAGE_LABELS);
    }

    public static CodeLabelDto pathStatus(String code) {
        return labeled(code, PATH_STATUS_LABELS);
    }

    public static CodeLabelDto pathDifficulty(Integer difficulty) {
        int value = difficulty == null ? 2 : difficulty;
        String code = switch (value) {
            case 1 -> "FOUNDATION";
            case 2, 3 -> "CORE";
            default -> "CHALLENGE";
        };
        String label = switch (code) {
            case "FOUNDATION" -> "基础";
            case "CHALLENGE" -> "挑战";
            default -> "核心";
        };
        return new CodeLabelDto(code, label);
    }

    private static CodeLabelDto labeled(String code, Map<String, String> labels) {
        String normalized = normalize(code);
        return new CodeLabelDto(normalized, labels.getOrDefault(normalized, normalized));
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
    }

    private static OptionDef option(String code, String label) {
        return new OptionDef(code, label);
    }

    private record OptionDef(String code, String label) {
        private boolean matches(String raw) {
            return Objects.equals(code, normalize(raw)) || Objects.equals(label, raw.trim());
        }
    }
}
