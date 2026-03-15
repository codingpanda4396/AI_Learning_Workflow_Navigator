# Submissions 接口输出质量优化说明

## 修改文件列表与说明

| 文件 | 修改内容 |
|------|----------|
| `DiagnosisProfileDerivationService.java` | 扩展 `deriveRiskTags`：新增 PROCESS_CONFUSION、INDEPENDENT_SOLVING_WEAKNESS、EXAM_ORIENTED_SURFACE_LEARNING_RISK、CONCEPT_NOT_STABLE，均来自答题信号；风险数量限制为 1~3 个。 |
| `SnapshotToDisplayMapper.java` | 重写 `deriveStrengths`：从目标/偏好/时间/基础/练习量提炼具体优势，避免“目标明确”等空泛句；重写 `deriveWeaknesses`：与 riskTags 联动，补充新风险对应中文弱项；空 strengths 时用“已根据你的选择确定起点与目标…”。 |
| `PlanExplanationDisplayHelper.java`（新增） | 将 entryMode / pace / taskGranularity / riskTags 转为用户可见中文，禁止在 planExplanation 中输出内部 code。 |
| `DiagnosisService.java` | `buildPlanExplanationFromSnapshot` 仅输出用户文案（调用 Helper）；`sanitizeFeatureSummaryForResponse`：features 为空时返回 null，不返回空结构；提交响应中 insights.featureSummary 使用清理后结果。 |
| `DiagnosisExplanationAssembler.java` | `snapshotStrengthItems`：增加偏好、时间充足等证据项，目标分面试/考试/项目细化；`snapshotWeaknessItems`：补充 PROCESS_CONFUSION、INDEPENDENT_SOLVING_WEAKNESS、EXAM_ORIENTED_SURFACE_LEARNING_RISK、CONCEPT_NOT_STABLE、BOUNDARY_WEAKNESS 对应证据。 |
| `PreviewDisplayCodeMapper.java` | `riskFlagLabel` 增加上述新风险码的中文；新增 `paceLabel`、`taskGranularityLabel`，与 submissions 的 planExplanation 一致，供 preview 使用。 |
| `DefaultTopicQuestionBank.java` | 修复类缺少闭合 `}` 导致的编译错误（既有问题）。 |

## strengths / weaknesses / riskTags 新生成逻辑

### strengths
- **规则**：从 snapshot 的 foundation / practice / goalType / learningPreference / timeBudget 推导。
- **具体**：ADVANCED 或 PROFICIENT → “基础相对扎实…”；MANY 练习且非 BEGINNER → “已有较多练习…”；INTERVIEW/EXAM → “目标清晰（面试/考试导向）…”；PROJECT → “以项目或实践为导向…”；其他目标 → “学习目标明确…”；有偏好 → “已选择学习偏好…”；时间非 SHORT_10 → “时间投入较充足…”。
- **空时**：仅一条“已根据你的选择确定起点与目标，将据此安排学习节奏。”

### weaknesses
- **规则**：与 riskTags、foundation、blocker、practice 联动。
- **具体**：BEGINNER → 基础不稳；NONE 练习 → 练习少需例子；FOLLOW_BUT_CANNOT_DO → 独立完成不足；各 riskTag 对应固定中文弱项（如 PROCESS_CONFUSION → “操作步骤容易混淆…”），不制造焦虑。
- **空时**：仅一条“将在后续训练中继续定位薄弱点。”

### riskTags
- **来源**：仅从答题信号。顺序与优先级：FOUNDATION_GAP(BEGINNER) → CONCEPT_NOT_STABLE(概念混淆/主题概念不清) → PROCESS_CONFUSION(主题操作) → INDEPENDENT_SOLVING_WEAKNESS(卡点) → TRANSFER_WEAKNESS → EXPRESSION_WEAKNESS → BOUNDARY_WEAKNESS → INTERVIEW_FOUNDATION_RISK → EXAM_ORIENTED_SURFACE_LEARNING_RISK。
- **数量**：最多取 3 个。

## 优化后 submissions 示例响应（片段）

```json
{
  "capabilityProfile": {
    "currentLevel": { "code": "BASIC", "label": "学过相关内容，但基础还不稳定" },
    "strengths": [
      "已选择学习偏好，后续讲解与练习会按此调整。",
      "时间投入较充足，可支持更完整的学习节奏。"
    ],
    "weaknesses": [
      "当前基础还不够稳定，需要先补齐关键概念。",
      "核心概念还不稳，建议先巩固定义与结构。"
    ]
  },
  "insights": {
    "summary": "你刚开始接触这个主题，系统会从最基础的结构和概念带你起步。",
    "planExplanation": "入口方式：先补基础再推进，节奏：常规节奏，任务粒度：中等步长。 建议先建立基本概念框架再推进。 已标记：基础仍需补齐、核心概念还不稳，规划时会考虑。",
    "featureSummary": null
  },
  "reasoningSteps": [ ... ],
  "strengthSources": [ ... ],
  "weaknessSources": [ ... ],
  "learnerProfileSnapshot": {
    "foundationLevel": "BEGINNER",
    "riskTags": [ "FOUNDATION_GAP", "CONCEPT_NOT_STABLE" ],
    "planHints": { "entryMode": "FOUNDATION_FIRST", "pace": "NORMAL", "taskGranularity": "MEDIUM", ... }
  }
}
```

- 用户可见文案中不再出现 FOUNDATION_FIRST、DEEP、MEDIUM、TEXT_WITH_MINI_EXAMPLE 等内部码。
- `featureSummary` 在无真实 feature 时为 `null`，不返回空结构。

## 作为 preview 强约束输入源的字段

以下来自 **learnerProfileSnapshot**（及 insights 中同源展示），preview 必须严格沿用、不得冲突映射：

- **foundationLevel**：当前级别
- **learningPreference**：学习偏好
- **goalType**：目标类型（QUICK_START / EXAM / INTERVIEW / PROJECT / PATCH_WEAKNESS）
- **timeBudget**：时间预算（SHORT_10 / MEDIUM_30 / LONG_60 / SYSTEMATIC）
- **riskTags**：风险标签列表（展示时用 PreviewDisplayCodeMapper.riskFlagLabel）
- **planHints**：entryMode / pace / taskGranularity / focusMode / explanationStyle（展示用 PreviewDisplayCodeMapper.recommendedStrategyTitle、paceLabel、taskGranularityLabel）
- **summary.currentState**：与 insights.summary 一致，为画像结论的单一来源

策略码仅存在于 **learnerProfileSnapshot.planHints** 和下游内部使用；**insights.planExplanation** 与 **capabilityProfile.strengths/weaknesses** 仅输出用户可见中文。
