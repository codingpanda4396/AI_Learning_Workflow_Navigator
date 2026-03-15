# 诊断评估结果页 - 文案草案

## 页面结构对应文案

### 顶部（Hero）
- **眉标**: 诊断结果
- **标题**: 系统对你的当前判断
- **说明**: 下面是根据你刚才的回答整理出的学习画像，会直接用来安排接下来的学习路径。

### 1. 你的当前状态
- **区块标题**: 你的当前状态
- **主结论**: 由 `capabilityProfile.currentLevel` 经 code→用户文案映射后展示（如「先打牢基础再深入」），不展示原始 code。
- **摘要**: 使用 `insights.summary`，缺省为「系统已根据你的回答整理出当前状态，接下来会据此安排学习路径。」
- **标签**: 学习偏好 / 时间预算 / 目标导向 的**用户文案**（来自 diagnosisDisplay 映射或后端 label）。

### 2. 关键卡点
- **已具备的优势**: 标题「已具备的优势」；列表为 strengths（智能收敛：仅 1 条且过短时不单独成块）；可选一句依据提示（来自 strengthSources）。
- **需要重点补强的部分**: 标题「需要重点补强的部分」；列表为 weaknesses；可选一句依据提示（来自 weaknessSources）。

### 3. 对学习路径的影响
- **区块标题**: 会怎样影响接下来的学习
- **正文**: `insights.planExplanation`，缺省「后续规划会基于这次判断继续细化，从你当前起点更顺畅地推进。」
- **补充**: 下一步将为你生成可确认的学习路径，你可以先看再决定是否开始。

### 4. 折叠区
- **触发文案**: 查看系统如何判断 / 收起
- **展开内容**: 逐条 reasoningSteps，格式为「根据你在「xxx」中的选择/回答，系统判断：xxx」。不展示 questionId、dimension code 等。

### 5. CTA
- **按钮**: 查看学习路径
- **说明**: 系统已根据这次诊断完成起步判断，下一步会给出可确认的学习路径。

## 不展示的内容（去技术味）
- 任何原始 code：FOUNDATION_FIRST、DEEP、MEDIUM、TEXT_WITH_MINI_EXAMPLE 等。
- strategyHints、entryMode、explanationStyle、pace、taskGranularity、focusMode 等 planHints 不直接展示，仅通过「会怎样影响学习」的概括语传达。
