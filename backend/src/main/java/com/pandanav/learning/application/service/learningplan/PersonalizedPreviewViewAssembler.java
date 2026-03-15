package com.pandanav.learning.application.service.learningplan;

import com.pandanav.learning.api.dto.plan.LearningPlanPreviewResponse;
import com.pandanav.learning.domain.model.LearnerProfileSnapshot;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;
import com.pandanav.learning.domain.model.LearningPlanPreview;
import com.pandanav.learning.domain.model.LearningPlanSummary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class PersonalizedPreviewViewAssembler {

    private final PreviewDisplayCodeMapper codeMapper;
    private final ConceptDisplayTitleMapper conceptDisplayTitleMapper;

    public PersonalizedPreviewViewAssembler(
        PreviewDisplayCodeMapper codeMapper,
        ConceptDisplayTitleMapper conceptDisplayTitleMapper
    ) {
        this.codeMapper = codeMapper;
        this.conceptDisplayTitleMapper = conceptDisplayTitleMapper;
    }

    public PersonalizedPreviewView assemble(
        LearningPlanPlanningContext context,
        LearningPlanPreview preview,
        String whyThisStep,
        List<String> profileDrivenReasoning,
        String expectedGain
    ) {
        LearningPlanSummary summary = preview.summary();
        String displayTitle = conceptDisplayTitleMapper.toDisplayTitle(summary.recommendedStartNodeName());
        Integer estimatedMinutes = resolveEstimatedMinutes(summary);
        String concept = extractConcept(displayTitle);

        String learningPreference = codeMapper.learningPreference(readStrategyHint(context, "learningPreference"));
        String goalOrientation = codeMapper.goalOrientation(readStrategyHint(context, "goalOrientation"));
        String timeBudget = codeMapper.timeBudget(readConstraint(context, "timeBudget"));
        String capability = resolveCapabilityText(context);

        return new PersonalizedPreviewView(
            buildSummary(concept, capability, learningPreference, goalOrientation, timeBudget),
            buildTaskCard(displayTitle, concept, estimatedMinutes),
            buildReasons(concept, estimatedMinutes, learningPreference, goalOrientation, timeBudget, whyThisStep, profileDrivenReasoning),
            buildExplanationPanel(capability, goalOrientation, learningPreference, timeBudget, expectedGain, concept)
        );
    }

    private LearningPlanPreviewResponse.PersonalizedSummaryResponse buildSummary(
        String concept,
        String capability,
        String learningPreference,
        String goalOrientation,
        String timeBudget
    ) {
        String title = "你已经接触过" + concept + "，但基础还不稳定";
        if (capability.contains("较好")) {
            title = "你在" + concept + "上已有基础，可以开始更聚焦的训练";
        } else if (capability.contains("一定基础")) {
            title = "你在" + concept + "上有一定基础，当前适合快速校准薄弱点";
        }
        String description = "考虑到你更适合" + learningPreference + "，系统先安排一个短任务，帮助你快速进入状态。";
        List<String> tags = new ArrayList<>();
        tags.add(goalOrientation);
        tags.add(timeBudget);
        tags.add(learningPreference);
        return new LearningPlanPreviewResponse.PersonalizedSummaryResponse(
            title,
            description,
            tags.stream().filter(item -> item != null && !item.isBlank()).distinct().limit(3).toList()
        );
    }

    private LearningPlanPreviewResponse.CurrentTaskCardResponse buildTaskCard(
        String displayTitle,
        String concept,
        Integer estimatedMinutes
    ) {
        String goal = "先建立对" + concept + "关键结构与基本连接方式的直观认识。";
        List<String> tasks = new ArrayList<>();
        List<String> gains = new ArrayList<>();
        if (concept.contains("链表")) {
            tasks.add("写出链表节点结构 Node");
            tasks.add("实现一次头插法");
            tasks.add("用两组测试数据验证结果");
            gains.add("理解链表节点结构");
            gains.add("理解 next 指针的含义");
            gains.add("能写出最基础的链表结构");
        } else if (concept.contains("图")) {
            tasks.add("写出图节点与边的基本表示");
            tasks.add("实现一次邻接表建图");
            tasks.add("用两组样例验证连边结果");
            gains.add("理解节点与边的关系");
            gains.add("掌握邻接表的基本表达");
            gains.add("能完成简单建图");
        } else {
            tasks.add("写出" + concept + "的核心结构定义");
            tasks.add("完成一次最小可运行示例");
            tasks.add("用两组输入验证结果");
            gains.add("理解" + concept + "的关键结构");
            gains.add("掌握最基本的实现步骤");
            gains.add("能独立完成基础样例");
        }
        return new LearningPlanPreviewResponse.CurrentTaskCardResponse(
            displayTitle,
            estimatedMinutes,
            goal,
            tasks,
            gains
        );
    }

    private LearningPlanPreviewResponse.PersonalizedReasonsResponse buildReasons(
        String concept,
        Integer estimatedMinutes,
        String learningPreference,
        String goalOrientation,
        String timeBudget,
        String whyThisStep,
        List<String> profileDrivenReasoning
    ) {
        LinkedHashSet<String> whyRecommended = new LinkedHashSet<>();
        whyRecommended.add("你当前目标是" + goalOrientation + "，先稳住" + concept + "基础会更高效。");
        whyRecommended.add("你的学习方式更适合" + learningPreference + "，短任务更容易快速进入状态。");
        whyRecommended.add("你当前可投入" + timeBudget + "，这一步控制在 " + estimatedMinutes + " 分钟更容易坚持。");
        if (profileDrivenReasoning != null) {
            for (String item : profileDrivenReasoning) {
                String normalized = normalizeReason(item);
                if (!normalized.isBlank()) {
                    whyRecommended.add(normalized);
                }
                if (whyRecommended.size() >= 3) {
                    break;
                }
            }
        }

        LinkedHashSet<String> whyStepFirst = new LinkedHashSet<>();
        whyStepFirst.add(whyStepFirstSentenceForConcept(concept));
        whyStepFirst.add("如果这一步不稳，后续练习容易反复卡在基础概念和表示方式上。");
        if (whyThisStep != null && !whyThisStep.isBlank()) {
            whyStepFirst.add(normalizeReason(whyThisStep));
        }
        return new LearningPlanPreviewResponse.PersonalizedReasonsResponse(
            whyRecommended.stream().limit(3).toList(),
            whyStepFirst.stream().limit(3).toList()
        );
    }

    private LearningPlanPreviewResponse.ExplanationPanelResponse buildExplanationPanel(
        String capability,
        String goalOrientation,
        String learningPreference,
        String timeBudget,
        String expectedGain,
        String concept
    ) {
        List<LearningPlanPreviewResponse.LearnerProfileItemResponse> learnerProfile = List.of(
            new LearningPlanPreviewResponse.LearnerProfileItemResponse("当前基础", capability),
            new LearningPlanPreviewResponse.LearnerProfileItemResponse("学习目标", goalOrientation),
            new LearningPlanPreviewResponse.LearnerProfileItemResponse("学习方式", learningPreference),
            new LearningPlanPreviewResponse.LearnerProfileItemResponse("时间节奏", timeBudget)
        );
        String systemDecision = "系统先安排一个低门槛练习，帮助你快速进入" + concept + "状态，并在练习中识别真正薄弱点。";
        if (expectedGain != null && !expectedGain.isBlank()) {
            systemDecision = expectedGain.trim();
        }
        return new LearningPlanPreviewResponse.ExplanationPanelResponse(learnerProfile, systemDecision);
    }

    private String resolveCapabilityText(LearningPlanPlanningContext context) {
        String fromFeature = codeMapper.foundationLevel(readFeatureValue(context, "foundation_level"));
        if (!fromFeature.contains("待进一步确认")) {
            return fromFeature;
        }
        return codeMapper.capabilityLevel(readStrategyHint(context, "currentLevel"));
    }

    private String readStrategyHint(LearningPlanPlanningContext context, String key) {
        LearnerProfileSnapshot snapshot = context == null ? null : context.learnerProfileSnapshot();
        if (snapshot == null || snapshot.getStrategyHints() == null || key == null) {
            return "";
        }
        Object value = snapshot.getStrategyHints().get(key);
        return value == null ? "" : String.valueOf(value).trim().toUpperCase(Locale.ROOT);
    }

    private String readConstraint(LearningPlanPlanningContext context, String key) {
        LearnerProfileSnapshot snapshot = context == null ? null : context.learnerProfileSnapshot();
        if (snapshot == null || snapshot.getConstraints() == null || key == null) {
            return "";
        }
        Object value = snapshot.getConstraints().get(key);
        return value == null ? "" : String.valueOf(value).trim().toUpperCase(Locale.ROOT);
    }

    private String readFeatureValue(LearningPlanPlanningContext context, String featureKey) {
        LearnerProfileSnapshot snapshot = context == null ? null : context.learnerProfileSnapshot();
        if (snapshot == null || snapshot.getFeatureSummary() == null || featureKey == null || featureKey.isBlank()) {
            return "";
        }
        Object features = snapshot.getFeatureSummary().get("features");
        if (!(features instanceof List<?> list)) {
            return "";
        }
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            Object key = map.get("featureKey");
            if (key == null || !featureKey.equalsIgnoreCase(String.valueOf(key).trim())) {
                continue;
            }
            Object value = map.get("featureValue");
            return value == null ? "" : String.valueOf(value).trim().toUpperCase(Locale.ROOT);
        }
        return "";
    }

    private String normalizeReason(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = text.trim()
            .replace("画像", "学习画像")
            .replace("策略", "学习方式");
        if (!normalized.contains("你")) {
            return "结合你的学习情况，" + normalized;
        }
        return normalized;
    }

    private Integer resolveEstimatedMinutes(LearningPlanSummary summary) {
        if (summary.taskEstimatedMinutes() != null && summary.taskEstimatedMinutes() > 0) {
            return summary.taskEstimatedMinutes();
        }
        if (summary.estimatedMinutes() != null && summary.estimatedMinutes() > 0) {
            return Math.max(5, summary.estimatedMinutes() / 2);
        }
        return 8;
    }

    private String extractConcept(String displayTitle) {
        if (displayTitle == null || displayTitle.isBlank()) {
            return "当前知识点";
        }
        String text = displayTitle.trim();
        if (text.startsWith("理解") && text.endsWith("的基本结构") && text.length() > 7) {
            return text.substring(2, text.length() - 5);
        }
        return text;
    }

    /** 按主题区分，禁止图场景出现“插入、删除”等链表用语。 */
    private String whyStepFirstSentenceForConcept(String concept) {
        if (concept == null) {
            return "当前知识点基础结构是后续学习的前提，先完成这一步更稳。";
        }
        if (concept.contains("图") || concept.contains("路径")) {
            return concept + "的基础表示与路径概念是后续算法（如最短路径）的共同前提，先建立节点、边与路径的直观认识更稳。";
        }
        if (concept.contains("树") || concept.contains("二叉树")) {
            return concept + "的基础结构是后续遍历与递归应用的共同前提，先建立结构定义与基本操作更稳。";
        }
        if (concept.contains("链表")) {
            return concept + "的节点与指针关系是后续插入、删除与综合应用的共同前提，先建立基本结构更稳。";
        }
        if (concept.contains("数组")) {
            return concept + "的索引与遍历是后续查找、排序等操作的共同前提，先建立基本用法更稳。";
        }
        return concept + "的基础结构是后续学习的前提，先完成这一步更稳。";
    }

    public record PersonalizedPreviewView(
        LearningPlanPreviewResponse.PersonalizedSummaryResponse personalizedSummary,
        LearningPlanPreviewResponse.CurrentTaskCardResponse currentTaskCard,
        LearningPlanPreviewResponse.PersonalizedReasonsResponse personalizedReasons,
        LearningPlanPreviewResponse.ExplanationPanelResponse explanationPanel
    ) {
    }
}
