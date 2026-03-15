# learning-plans/preview 接口优化说明

## 修改文件列表

| 文件 | 修改点 |
|------|--------|
| `PreviewDisplayCodeMapper.java` | 学习偏好增加 TEXT_FIRST/VISUAL_FIRST/CODE_FIRST 与 submissions 一致；foundationLevel/capabilityLevel 中 BEGINNER 改为「刚开始接触」；新增 alternativeNotRecommendedReason、sanitizeUserFacingText；修复 goalOrientation 缩进。 |
| `SnapshotDrivenPreviewExplanationAssembler.java` | explanationPanel 当前基础优先用 snapshot.summary().currentState()，无则用 mapper；禁止「根据诊断结果安排」，改为「暂未识别」；personalizedReasons 仅在有真实映射值时加入理由；levelPhrase 与 submissions 一致；图/路径场景 whyThisStepFirstSentence 强化「表示与路径直觉」；taskCardTasks 图场景改为用户可执行动作短句。 |
| `LearningPlanService.java` | nextActions 优先用 currentTaskCard.tasks，否则兜底动作句；confidenceHint 无 evidenceSummary 时固定为「这是基于本轮诊断生成的首个起步方案，后续会根据你的实际完成情况继续微调。」；skipRisk/whyThisStep/entryReason/expectedGain 经 sanitizeUserFacingText 清洗；alternatives 使用 alternativeNotRecommendedReason 生成 notRecommendedReason；buildNextActions 兜底句改为动作化短句。 |
| `ConceptDisplayTitleMapper.java` | 使用小写常量 "foundation of " 做前缀判断，避免内部格式泄漏。 |

## 与 submissions 画像一致性

- **单一事实源**：preview 中「当前基础 / 学习偏好 / 当前目标 / 时间节奏」一律来自 `LearnerProfileStructuredSnapshotDto`，经 `PreviewDisplayCodeMapper` 映射。
- **当前基础**：优先使用 `snapshot.summary().currentState()`（与诊断提交页 submissions 同一来源）；缺失时用 `foundationLevel()` 映射，且 BEGINNER/NONE/WEAK →「刚开始接触」，与 DiagnosisProfileDerivationService.buildCurrentState 及 evidence「你选择了「刚开始接触」」一致。
- **学习偏好**：Mapper 增加 TEXT_FIRST→「先看文字讲解」、VISUAL_FIRST、CODE_FIRST，与 ContractCatalog.SNAPSHOT_PREFERENCE_LABELS 一致，不再出现「根据诊断结果安排」。

## 中英文混杂与内部码清理

- **sanitizeUserFacingText**：对所有用户可见文案（entryReason、whyThisStep、skipRisk、expectedGain、nextActions 各项）做一次清洗，将 "Foundation of X"（不区分大小写）替换为「X 基础」。
- **ConceptDisplayTitleMapper**：recommendedEntryTitle 统一用 "foundation of " 小写前缀截取，输出「理解X的基本结构」，不向用户暴露 "Foundation of 图"。
- **alternatives**：notRecommendedReason 由 PreviewDisplayCodeMapper.alternativeNotRecommendedReason(code) 生成纯中文策略理由，不再使用可能含内部码的模板。

## 优化后 preview 示例（节选）

```json
{
  "recommendedEntry": {
    "conceptId": "101",
    "title": "理解图的基本结构",
    "estimatedMinutes": 8,
    "reason": "先从「理解图的基本结构」开始，因为它是达成「理解最短路径算法」所需的前置基础。"
  },
  "learnerSnapshot": {
    "currentState": "你刚开始接触这个主题，系统会从最基础的结构和概念带你起步。",
    "evidence": ["你选择了「刚开始接触」", "你当前更大的卡点是「概念本身就不太清楚」"]
  },
  "whyThisStep": "因为你当前刚开始接触、又希望系统理解核心原理，所以系统先安排理解图的基本结构，帮助你先建立后续「理解最短路径算法」所需的最小前置认知。",
  "skipRisk": "如果跳过「理解图的基本结构」，后续节点的概念连接更容易断链并反复返工。",
  "expectedGain": "完成这一步后，你会更容易进入后续训练并减少回退。",
  "confidenceHint": "这是基于本轮诊断生成的首个起步方案，后续会根据你的实际完成情况继续微调。",
  "nextActions": [
    "先用一个简单样例认清图中的节点、边和路径",
    "再区分邻接表和邻接矩阵各自表示什么",
    "最后用一条路径例子建立后续最短路径的直觉"
  ],
  "explanationPanel": {
    "learnerProfile": [
      { "label": "当前基础", "value": "你刚开始接触这个主题，系统会从最基础的结构和概念带你起步。" },
      { "label": "主要卡点", "value": "概念本身不太清楚" },
      { "label": "学习偏好", "value": "先看文字讲解" },
      { "label": "当前目标", "value": "系统理解核心原理" },
      { "label": "时间节奏", "value": "20~30 分钟" }
    ],
    "systemDecision": "完成这一步后，你会更容易进入后续训练并减少回退。"
  },
  "personalizedReasons": {
    "whyRecommended": [
      "你当前目标是系统理解核心原理，先稳住图基础会更高效。",
      "你更适合先看文字讲解，这一步会按这个方式安排。",
      "你当前可投入20~30 分钟，这一步约8分钟，节奏已匹配。"
    ],
    "whyThisStepFirst": [
      "图的表示方式与路径概念是理解最短路径算法的共同前提；先建立节点、边和路径的直观认识，比直接进算法步骤更稳。",
      "如果跳过这一步，后续在图上的练习容易反复卡在基础概念和表示方式上。"
    ]
  },
  "alternatives": [
    { "code": "FAST_TRACK", "label": "快速推进", "notRecommendedReason": "你当前基础更适合先稳一步再推进，直接快跑容易在概念连接处卡住。" }
  ]
}
```

## 冗余字段处理

- **未删除字段**：保留 explanationPanel、personalizedReasons、keyEvidence、profileDrivenReasoning；语义已区分（画像+决策 / 推荐理由 / 证据列表 / 画像驱动推理），仅收紧内容来源与文案，避免重复表达。
- **confidenceHint**：由「当前证据有限」等表述改为首轮规划统一话术，避免给用户「系统没把握」的感觉。
