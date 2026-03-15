package com.pandanav.learning.application.service.learningplan;

import com.pandanav.learning.api.dto.plan.LearningPlanPreviewResponse;
import com.pandanav.learning.api.dto.diagnosis.LearnerProfileStructuredSnapshotDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * 规划预览解释层统一收口：所有解释字段由 learnerProfileSnapshot 派生，禁止旧模板污染与场景串线。
 */
@Component
public class SnapshotDrivenPreviewExplanationAssembler {

    private final PreviewDisplayCodeMapper codeMapper;

    public SnapshotDrivenPreviewExplanationAssembler(PreviewDisplayCodeMapper codeMapper) {
        this.codeMapper = codeMapper;
    }

    /**
     * 仅描述“系统看到了什么”，不用于“为什么先学这一步”。
     */
    public String buildCurrentState(LearnerProfileStructuredSnapshotDto snapshot) {
        if (snapshot == null || snapshot.summary() == null) {
            return null;
        }
        String s = snapshot.summary().currentState();
        return s != null && !s.isBlank() ? s.trim() : null;
    }

    /**
     * 独立生成“为什么先学这一步”，不得复用 currentState。基于 foundationLevel、primaryBlocker、goalType、timeBudget、recommendedEntry、goalText。
     */
    public String buildWhyThisStep(
        LearnerProfileStructuredSnapshotDto snapshot,
        String goalText,
        String recommendedEntryTitle,
        Integer estimatedMinutes
    ) {
        if (snapshot == null) {
            return null;
        }
        String level = n(snapshot.foundationLevel());
        String goal = n(snapshot.goalType());
        String entry = recommendedEntryTitle != null && !recommendedEntryTitle.isBlank() ? recommendedEntryTitle.trim() : "这一步";
        String goalDesc = goalText != null && !goalText.isBlank() ? goalText.trim() : "后续学习";

        String levelPhrase = levelPhrase(level);
        String goalPhrase = goalPhrase(goal);
        if (entry.length() > 20) {
            entry = "一个短小的" + topicFromTitle(entry) + "基础任务";
        }
        return String.format("因为你当前%s、又%s，所以系统先安排%s，帮助你先建立后续「%s」所需的最小前置认知。",
            levelPhrase, goalPhrase, entry, goalDesc.length() > 24 ? goalDesc.substring(0, 24) + "…" : goalDesc);
    }

    /** 与 submissions 画像一致。 */
    private String levelPhrase(String level) {
        return switch (level) {
            case "BEGINNER", "NONE", "WEAK" -> "刚开始接触";
            case "BASIC" -> "学过但还不太熟";
            case "INTERMEDIATE", "PARTIAL", "COURSEWORK" -> "基础尚可但需巩固";
            case "ADVANCED", "PROFICIENT", "STABLE" -> "基础较好";
            default -> "当前起点如画像所示";
        };
    }

    private String goalPhrase(String goal) {
        return switch (goal) {
            case "QUICK_START" -> "希望快速入门";
            case "INTERVIEW" -> "偏向面试准备";
            case "PROJECT", "PROJECT_DRIVEN" -> "偏向项目实践";
            case "EXAM", "EXAM_PREP" -> "偏向应对考试";
            case "REVIEW_FIX" -> "希望查缺补漏";
            case "UNDERSTAND_PRINCIPLE" -> "希望系统理解原理";
            default -> "希望稳步推进";
        };
    }

    private String topicFromTitle(String title) {
        if (title == null) return "当前主题";
        if (title.contains("图")) return "图";
        if (title.contains("树") || title.contains("二叉树")) return "树";
        if (title.contains("链表")) return "链表";
        if (title.contains("数组")) return "数组";
        return "当前主题";
    }

    public LearningPlanPreviewResponse.PersonalizedSummaryResponse buildPersonalizedSummary(
        LearnerProfileStructuredSnapshotDto snapshot,
        String goalText,
        String recommendedEntryTitle
    ) {
        if (snapshot == null) {
            return null;
        }
        String foundationLevel = n(snapshot.foundationLevel());
        String topicClarity = n(snapshot.topicConceptClarity());
        String topic = topicFromTitle(recommendedEntryTitle != null ? recommendedEntryTitle : "");
        if (topic.equals("当前主题") && recommendedEntryTitle != null && !recommendedEntryTitle.isBlank()) {
            topic = recommendedEntryTitle.trim();
        }

        String title = summaryTitleFromSnapshot(foundationLevel, topicClarity, topic);
        String description = summaryDescriptionFromSnapshot(snapshot, goalText, topic);
        List<String> tags = summaryTagsFromSnapshot(snapshot, topic);
        return new LearningPlanPreviewResponse.PersonalizedSummaryResponse(title, description, tags);
    }

    /** 与 submissions 画像一致：BEGINNER 对应起步/刚开始接触。 */
    private String summaryTitleFromSnapshot(String foundationLevel, String topicClarity, String topic) {
        if ("BEGINNER".equals(foundationLevel) || "NONE".equals(foundationLevel) || "WEAK".equals(foundationLevel)) {
            return "你当前还处在" + topic + "的起步阶段";
        }
        if ("BASIC".equals(foundationLevel) && "PARTLY_CLEAR".equals(topicClarity)) {
            return "你已有一些概念印象，但" + topic + "基础还不稳";
        }
        if ("BASIC".equals(foundationLevel)) {
            return "你学过但还不太熟，先稳一步" + topic + "再推进";
        }
        if ("INTERMEDIATE".equals(foundationLevel) || "PARTIAL".equals(foundationLevel)) {
            return "你在" + topic + "上已有一定基础，可以更聚焦训练";
        }
        if ("ADVANCED".equals(foundationLevel) || "PROFICIENT".equals(foundationLevel)) {
            return "你在" + topic + "上基础较好，可进入强化阶段";
        }
        return "系统已根据诊断识别你在" + topic + "的当前起点";
    }

    private String summaryDescriptionFromSnapshot(LearnerProfileStructuredSnapshotDto snapshot, String goalText, String topic) {
        String pref = codeMapper.learningPreference(n(snapshot.learningPreference()));
        String budget = codeMapper.timeBudget(n(snapshot.timeBudget()));
        boolean hasPref = pref != null && !pref.isBlank();
        boolean hasBudget = budget != null && !budget.isBlank();
        if (!hasPref && !hasBudget) {
            return "系统根据你的目标「" + (goalText != null && !goalText.isBlank() ? truncate(goalText, 20) : "当前学习") + "」安排了第一步，先稳住" + topic + "基础再推进。";
        }
        if (hasPref && hasBudget) {
            return "考虑到你更适合" + pref + "，且可投入" + budget + "，系统先安排一步与目标一致的任务，帮助你快速进入状态。";
        }
        if (hasPref) {
            return "考虑到你更适合" + pref + "，系统先安排一步与目标一致的任务，帮助你快速进入状态。";
        }
        return "你当前可投入" + budget + "，系统先安排一步与目标一致的任务，帮助你快速进入状态。";
    }

    private List<String> summaryTagsFromSnapshot(LearnerProfileStructuredSnapshotDto snapshot, String topic) {
        List<String> tags = new ArrayList<>();
        String goal = n(snapshot.goalType());
        if ("QUICK_START".equals(goal)) tags.add("快速入门");
        else if ("INTERVIEW".equals(goal)) tags.add("面试准备");
        else if ("REVIEW_FIX".equals(goal)) tags.add("查缺补漏");
        String budget = n(snapshot.timeBudget());
        if ("SHORT_10".equals(budget)) tags.add("约10分钟");
        else if ("MEDIUM_30".equals(budget)) tags.add("20~30分钟");
        else if ("LONG_60".equals(budget)) tags.add("40~60分钟");
        if (topic != null && !topic.isBlank()) tags.add(topic + "基础先行");
        String pref = n(snapshot.learningPreference());
        if ("CONCEPT_FIRST".equals(pref) || "LEARN_THEN_PRACTICE".equals(pref)) tags.add("先讲解再练习");
        else if ("PRACTICE_FIRST".equals(pref)) tags.add("先练再纠偏");
        return tags.stream().filter(t -> t != null && !t.isBlank()).distinct().limit(4).toList();
    }

    /** 画像一律来自 snapshot；当前基础优先用 summary.currentState 与 submissions 一致，禁止「根据诊断结果安排」。 */
    public LearningPlanPreviewResponse.ExplanationPanelResponse buildExplanationPanel(
        LearnerProfileStructuredSnapshotDto snapshot,
        String expectedGain,
        String recommendedEntryTitle
    ) {
        if (snapshot == null) {
            return null;
        }
        String foundation = snapshot.summary() != null && snapshot.summary().currentState() != null && !snapshot.summary().currentState().isBlank()
            ? snapshot.summary().currentState().trim()
            : orEmpty(codeMapper.foundationLevel(n(snapshot.foundationLevel())), "暂未识别");
        String blocker = orEmpty(codeMapper.primaryBlockerLabel(n(snapshot.primaryBlocker())), "暂未识别");
        String preference = orEmpty(codeMapper.learningPreference(n(snapshot.learningPreference())), "暂未识别");
        String goal = orEmpty(codeMapper.goalOrientation(n(snapshot.goalType())), "暂未识别");
        String time = orEmpty(codeMapper.timeBudget(n(snapshot.timeBudget())), "暂未识别");

        List<LearningPlanPreviewResponse.LearnerProfileItemResponse> learnerProfile = List.of(
            new LearningPlanPreviewResponse.LearnerProfileItemResponse("当前基础", foundation),
            new LearningPlanPreviewResponse.LearnerProfileItemResponse("主要卡点", blocker),
            new LearningPlanPreviewResponse.LearnerProfileItemResponse("学习偏好", preference),
            new LearningPlanPreviewResponse.LearnerProfileItemResponse("当前目标", goal),
            new LearningPlanPreviewResponse.LearnerProfileItemResponse("时间节奏", time)
        );
        String topic = topicFromTitle(recommendedEntryTitle != null ? recommendedEntryTitle : "");
        String conceptForDecision = (topic != null && !topic.equals("当前主题")) ? topic : "";
        String systemDecision = expectedGain != null && !expectedGain.isBlank()
            ? expectedGain.trim()
            : (!conceptForDecision.isBlank()
                ? "先安排一步" + conceptForDecision + "相关任务，与当前起点和目标一致。"
                : "先安排一步与当前起点和目标一致的任务。");
        return new LearningPlanPreviewResponse.ExplanationPanelResponse(learnerProfile, systemDecision);
    }

    /** 仅当有真实映射值时才加入对应理由，避免假个性化（如「更适合根据诊断结果安排」）。 */
    public LearningPlanPreviewResponse.PersonalizedReasonsResponse buildPersonalizedReasons(
        LearnerProfileStructuredSnapshotDto snapshot,
        String goalText,
        String recommendedEntryTitle,
        String whyThisStep,
        Integer estimatedMinutes
    ) {
        if (snapshot == null) {
            return null;
        }
        String topic = topicFromTitle(recommendedEntryTitle != null ? recommendedEntryTitle : "");
        if (topic.equals("当前主题")) {
            topic = recommendedEntryTitle != null && !recommendedEntryTitle.isBlank() ? recommendedEntryTitle.trim() : "当前知识点";
        }
        String goalLabel = codeMapper.goalOrientation(n(snapshot.goalType()));
        String prefLabel = codeMapper.learningPreference(n(snapshot.learningPreference()));
        String budgetLabel = codeMapper.timeBudget(n(snapshot.timeBudget()));

        LinkedHashSet<String> whyRecommended = new LinkedHashSet<>();
        if (goalLabel != null && !goalLabel.isBlank()) {
            whyRecommended.add("你当前目标是" + goalLabel + "，先稳住" + topic + "基础会更高效。");
        }
        if (prefLabel != null && !prefLabel.isBlank()) {
            whyRecommended.add("你更适合" + prefLabel + "，这一步会按这个方式安排。");
        }
        if (budgetLabel != null && !budgetLabel.isBlank()) {
            if (estimatedMinutes != null && estimatedMinutes > 0) {
                whyRecommended.add("你当前可投入" + budgetLabel + "，这一步约" + estimatedMinutes + "分钟，节奏已匹配。");
            } else {
                whyRecommended.add("你当前可投入" + budgetLabel + "，节奏已做匹配。");
            }
        }
        if (whyRecommended.isEmpty()) {
            whyRecommended.add("先稳住" + topic + "基础，再推进后续目标。");
        }

        LinkedHashSet<String> whyStepFirst = new LinkedHashSet<>();
        whyStepFirst.add(whyThisStepFirstSentence(topic, recommendedEntryTitle));
        whyStepFirst.add("如果跳过这一步，后续在" + topic + "上的练习容易反复卡在基础概念和表示方式上。");
        if (whyThisStep != null && !whyThisStep.isBlank()) {
            whyStepFirst.add(whyThisStep.trim());
        }

        return new LearningPlanPreviewResponse.PersonalizedReasonsResponse(
            whyRecommended.stream().limit(3).toList(),
            whyStepFirst.stream().limit(3).toList()
        );
    }

    /** 按主题区分；图/路径场景强调表示与路径直觉，避免直接进算法步骤。 */
    private String whyThisStepFirstSentence(String topic, String recommendedEntryTitle) {
        if (topic != null && (topic.contains("图") || topic.contains("路径"))) {
            return "图的表示方式与路径概念是理解最短路径算法的共同前提；先建立节点、边和路径的直观认识，比直接进算法步骤更稳。";
        }
        if (topic != null && (topic.contains("树") || topic.contains("二叉树"))) {
            return topic + "的基础结构是后续遍历与递归应用的共同前提，先建立结构定义与基本操作更稳。";
        }
        if (topic != null && topic.contains("链表")) {
            return topic + "的节点与指针关系是后续插入、删除与综合应用的共同前提，先建立基本结构更稳。";
        }
        if (topic != null && topic.contains("数组")) {
            return topic + "的索引与遍历是后续查找、排序等操作的共同前提，先建立基本用法更稳。";
        }
        return (topic != null ? topic : "当前知识点") + "的基础结构是后续学习的前提，先完成这一步更稳。";
    }

    public LearningPlanPreviewResponse.CurrentTaskCardResponse buildCurrentTaskCard(
        String recommendedEntryTitle,
        Integer estimatedMinutes,
        String goalText
    ) {
        String title = recommendedEntryTitle != null && !recommendedEntryTitle.isBlank() ? recommendedEntryTitle.trim() : "理解当前知识点的基本结构";
        String topic = topicFromTitle(recommendedEntryTitle != null ? recommendedEntryTitle : "");
        if (topic.equals("当前主题")) {
            topic = recommendedEntryTitle != null && !recommendedEntryTitle.isBlank() ? recommendedEntryTitle.trim() : "当前知识点";
        }
        int mins = estimatedMinutes != null && estimatedMinutes > 0 ? estimatedMinutes : 15;

        String goal = taskCardGoal(topic, goalText);
        List<String> tasks = taskCardTasks(topic, goalText);
        List<String> gains = taskCardGains(topic);
        return new LearningPlanPreviewResponse.CurrentTaskCardResponse(title, mins, goal, tasks, gains);
    }

    private String taskCardGoal(String topic, String goalText) {
        if (topic != null && (topic.contains("图") || topic.contains("路径"))) {
            return "先建立对图中节点、边与路径的基本表示和直观认识。";
        }
        if (topic != null && (topic.contains("树") || topic.contains("二叉树"))) {
            return "先建立对" + topic + "的结构定义与基本遍历方式的直观认识。";
        }
        if (topic != null && topic.contains("链表")) {
            return "先建立对链表节点与指针连接方式的直观认识。";
        }
        if (topic != null && topic.contains("数组")) {
            return "先建立对数组索引与基本遍历的直观认识。";
        }
        return "先建立对" + (topic != null ? topic : "当前知识点") + "关键结构与基本概念的直观认识。";
    }

    /** 用户可执行的动作短句，非策略标签。 */
    private List<String> taskCardTasks(String topic, String goalText) {
        List<String> tasks = new ArrayList<>();
        if (topic != null && (topic.contains("图") || topic.contains("路径"))) {
            tasks.add("先用一个简单样例认清图中的节点、边和路径");
            tasks.add("再区分邻接表和邻接矩阵各自表示什么");
            tasks.add("最后用一条路径例子建立后续最短路径的直觉");
            return tasks;
        }
        if (topic != null && (topic.contains("树") || topic.contains("二叉树"))) {
            tasks.add("写出" + topic + "的节点结构定义");
            tasks.add("说明前序/中序/后序的含义");
            tasks.add("用一个小例子完成一次遍历");
            return tasks;
        }
        if (topic != null && topic.contains("链表")) {
            tasks.add("写出链表节点结构 Node");
            tasks.add("实现一次头插法");
            tasks.add("用两组测试数据验证结果");
            return tasks;
        }
        if (topic != null && topic.contains("数组")) {
            tasks.add("写出数组的索引与长度概念");
            tasks.add("完成一次遍历或查找示例");
            tasks.add("用两组输入验证结果");
            return tasks;
        }
        tasks.add("写出" + (topic != null ? topic : "当前知识点") + "的核心结构定义");
        tasks.add("完成一次最小可运行示例");
        tasks.add("用两组输入验证结果");
        return tasks;
    }

    private List<String> taskCardGains(String topic) {
        List<String> gains = new ArrayList<>();
        if (topic != null && (topic.contains("图") || topic.contains("路径"))) {
            gains.add("理解节点与边的关系");
            gains.add("掌握邻接表或邻接矩阵的基本表达");
            gains.add("能完成简单建图或路径表示");
            return gains;
        }
        if (topic != null && (topic.contains("树") || topic.contains("二叉树"))) {
            gains.add("理解" + topic + "的关键结构");
            gains.add("掌握基本遍历方式");
            gains.add("能独立完成简单样例");
            return gains;
        }
        if (topic != null && topic.contains("链表")) {
            gains.add("理解链表节点结构");
            gains.add("理解 next 指针的含义");
            gains.add("能写出最基础的链表结构");
            return gains;
        }
        gains.add("理解" + (topic != null ? topic : "当前知识点") + "的关键结构");
        gains.add("掌握最基本的实现步骤");
        gains.add("能独立完成基础样例");
        return gains;
    }

    /** 从 snapshot 收紧 keyEvidence，最多 4 条，每条可追溯。 */
    public List<String> buildKeyEvidence(
        LearnerProfileStructuredSnapshotDto snapshot,
        List<String> extraFromReasoning
    ) {
        List<String> out = new ArrayList<>();
        if (snapshot != null && snapshot.summary() != null && snapshot.summary().evidence() != null) {
            for (String e : snapshot.summary().evidence()) {
                if (e != null && !e.isBlank()) {
                    out.add(e.trim());
                    if (out.size() >= 4) break;
                }
            }
        }
        if (extraFromReasoning != null) {
            for (String e : extraFromReasoning) {
                if (e != null && !e.isBlank() && !out.contains(e.trim())) {
                    out.add(e.trim());
                    if (out.size() >= 4) break;
                }
            }
        }
        return out.stream().limit(4).toList();
    }

    /** 推荐入口理由：与 goalText、recommendedEntry 强关联，纯中文，不泛化。 */
    public String buildRecommendedEntryReason(
        LearnerProfileStructuredSnapshotDto snapshot,
        String goalText,
        String recommendedEntryTitle
    ) {
        if (snapshot == null) {
            return null;
        }
        String goalDesc = goalText != null && !goalText.isBlank() ? truncate(goalText, 24) : "当前目标";
        return "先从「" + (recommendedEntryTitle != null && !recommendedEntryTitle.isBlank() ? recommendedEntryTitle.trim() : "当前关键知识点") + "」开始，因为它是达成「" + goalDesc + "」所需的前置基础。";
    }

    private static String n(String v) {
        return v == null ? "" : v.trim().toUpperCase(Locale.ROOT);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        s = s.trim();
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private static String orEmpty(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }
}
