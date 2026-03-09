# LLM 个性化路径规划技术方案（MVP -> 可灰度上线）

## 1. 目标
- 用 LLM 提升学习路径个性化，不推翻现有主链路。
- 保留当前稳定机制：`concept_node` 顺序 + 四阶段任务生成。
- 增加“个性化重排与插入能力”：按学生目标、掌握度、错误模式动态调整下一步任务。

## 2. 现状与问题
- 当前路径规划以固定模板为主，个性化弱。
- 路径选项是静态预设（steady/exam_sprint/project_apply），缺少用户画像驱动。
- 任务规划默认每个节点固定四阶段，无法根据卡点动态缩放。

## 3. 总体设计
采用“双层规划器”：
1) Rule Planner（保底层）
- 继续使用现有线性路径与固定四阶段模板，保证稳定与可回退。

2) LLM Planner（个性化层）
- 在 Rule Planner 输出基础上做“可控改写”：
- 重排节点优先级（仅限当前 chapter 节点集合内）
- 插入补救任务（UNDERSTANDING/TRAINING）
- 调整训练密度（题量/变式强度）
- 输出必须是结构化 JSON，经过校验后才生效

## 4. 核心改造点（贴合当前代码）
### 4.1 新增领域对象
- `PersonalizedPathContext`
  - goal_text
  - goal_diagnosis(smart_breakdown)
  - mastery_by_node
  - recent_error_tags
  - recent_scores
  - chapter_nodes

- `PersonalizedPathPlan`
  - ordered_nodes
  - inserted_tasks
  - plan_reasoning_summary
  - risk_flags

### 4.2 新增 Prompt
- `PATH_PLAN_V1`
- 输入：`PersonalizedPathContext`
- 输出建议：
```json
{
  "ordered_nodes": [
    {"node_id": 101, "priority": 1, "reason": "..."}
  ],
  "inserted_tasks": [
    {
      "node_id": 101,
      "stage": "UNDERSTANDING",
      "objective": "...",
      "trigger": "MISSING_STEPS"
    }
  ],
  "plan_reasoning_summary": "...",
  "risk_flags": ["..."]
}
```

### 4.3 服务编排
新增 `PersonalizedPathPlannerService`：
1. 先调用 Rule Planner 生成 baseline。
2. 组装 `PersonalizedPathContext`。
3. 调用 LLM 获取个性化计划。
4. 进行 schema + 业务约束校验：
   - 只能引用本 chapter 节点
   - stage 必须是现有枚举
   - 插入任务数量上限（建议 1~3）
   - objective 长度与敏感词校验
5. 校验通过则落地；失败则回退 baseline。

### 4.4 持久化策略（最小改造）
不强依赖新表，先落 JSON：
- 在 `learning_event` 新增事件类型：`PATH_PERSONALIZED_PLANNED`
- `event_data` 存：promptKey/version、context摘要、plan JSON、validation结果

后续再考虑独立表：`session_path_plan_snapshot`。

## 5. API 方案
保留现有接口，新增可选参数和新接口：

1) `POST /api/session/{sessionId}/plan`
- 新增 query: `mode=rule|llm|auto`（默认 auto）
- `auto` 策略：有足够画像数据时走 llm，否则 rule

2) 新增 `GET /api/session/{sessionId}/plan-preview`
- 返回本次计划来源（RULE/LLM）
- 返回 plan summary 与 risk flags

## 6. 约束与安全
- 强制 JSON schema 校验 + 业务白名单校验。
- LLM 不得创建未知节点、不得跨 chapter 规划。
- 任何异常自动 fallback 到 Rule Planner。
- 记录 promptKey/version + model + provider，便于审计。

## 7. 灰度与上线策略
阶段 1：影子模式（不生效）
- 后台并行生成 LLM plan，仅记录，不影响用户。
- 观察指标：可解析率、校验通过率、与规则计划差异。

阶段 2：小流量生效（5%-10%）
- 仅对新 session 开启。
- 出错自动回退 Rule Planner。

阶段 3：扩大流量
- 根据学习效果指标逐步扩大。

## 8. 验收指标（必须量化）
- 结构化输出可解析率 >= 98%
- 校验通过率 >= 95%
- fallback 率 <= 10%
- 训练任务完成率提升 >= 8%
- 二次补救任务后分数提升（median）>= 10 分

## 9. 开发拆分（建议 3 个迭代）
迭代 A（基础设施）
- Prompt + DTO + parser + validator + logging
- 影子模式接入

迭代 B（业务生效）
- `mode=auto` 生效
- 插入任务策略与上限控制

迭代 C（优化）
- A/B test
- 策略学习（根据历史效果动态调参）

## 10. 结论
建议采用“Rule 保底 + LLM 增强 + 严格校验 + 渐进灰度”的方案。
这样能在不破坏当前稳定性的前提下，显著提升路径个性化程度，并可持续迭代。
