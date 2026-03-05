
---

```md
# spec/03_policy.md

# Policy Rules (MVP)

## Purpose
Given the latest evaluation evidence (score + error tags) and current mastery state, the Policy Engine decides the next action:
- insert remedial tasks (UNDERSTANDING)
- insert variant/reinforcement training tasks (TRAINING)
- advance to next concept node
- or keep current flow

## Inputs
- `session_id`
- `user_id`
- `chapter_id`
- `current_node_id`
- `score` (0~100) from evaluator
- `error_tags[]` (set of ErrorTag)
- `mastery_value` (0~1) for current node after applying delta

## Outputs
- `next_action` (NextAction)
- `next_task` (optional: created task info)
- `updated_session_position` (optional: new current_node_id/current_stage)

---

## Mastery Thresholds (MVP)
- `PASS_SCORE_HIGH = 80`
- `PASS_SCORE_LOW = 60`
- `ADVANCE_MASTERY = 0.70`

---

## Decision Rules (Deterministic)

### Rule A: Remedial Understanding
IF `score < PASS_SCORE_LOW` THEN:
- `next_action = INSERT_REMEDIAL_UNDERSTANDING`
- Create a new task:
  - stage = UNDERSTANDING
  - node_id = current_node_id
  - objective must be driven by error_tags (see Objective Templates)
- Set session position:
  - current_stage = UNDERSTANDING

### Rule B: Training Variants
ELSE IF `PASS_SCORE_LOW <= score < PASS_SCORE_HIGH` THEN:
- `next_action = INSERT_TRAINING_VARIANTS`
- Create a new task:
  - stage = TRAINING
  - node_id = current_node_id
  - objective: generate 3 more variant questions focusing on weakest error_tags
- Set session position:
  - current_stage = TRAINING

### Rule C: High Score → Advance or Reinforce
ELSE (score >= PASS_SCORE_HIGH):
- IF `mastery_value >= ADVANCE_MASTERY` THEN:
  - `next_action = ADVANCE_TO_NEXT_NODE`
  - Advance rule (MVP):
    - choose the next node by `order_no` within the chapter (ascending)
    - next_node must exist; if not, next_action becomes NOOP (chapter complete)
  - Create next node tasks (optional in MVP):
    - at least create next node UNDERSTANDING task as the immediate next step
  - Set session position:
    - current_node_id = next_node_id
    - current_stage = UNDERSTANDING
- ELSE:
  - `next_action = INSERT_TRAINING_REINFORCEMENT`
  - Create a new TRAINING task for the same node to consolidate
  - Set session position:
    - current_stage = TRAINING

---

## Objective Templates (MVP)

### Remedial UNDERSTANDING Objective
Base: "针对【{node_name}】的薄弱点进行补救讲解：要求对比误区 + 给出反例 + 给出步骤化机制说明。"

ErrorTag mapping:
- CONCEPT_CONFUSION:
  - "重点澄清相近概念的差异，并给出对比表 + 典型混淆题。"
- MISSING_STEPS:
  - "用流程图式步骤说明机制链路，并指出每一步的目的与输入输出。"
- BOUNDARY_CASE:
  - "补充边界条件/异常场景（如超时、重放、半开连接）并说明为何规则仍成立。"
- TERMINOLOGY:
  - "校准关键术语定义与标准表述，列出必须掌握的关键词。"
- SHALLOW_REASONING:
  - "要求给出因果链解释：为什么→导致什么→如何验证。"
- MEMORY_GAP:
  - "给出记忆框架（口诀/分组）与 1 分钟复述脚本。"

### Training Variant Objective
Base: "围绕【{node_name}】生成 3 道变式题，覆盖：概念辨析/机制步骤/边界情况，并给出评分 rubric 与参考要点。"

### Training Reinforcement Objective
Base: "围绕【{node_name}】生成 3 道巩固题（难度略升），重点覆盖最近 evidence 的 error_tags，并给出评分 rubric。"

---

## Notes (MVP)
- Policy must be deterministic for the same inputs.
- Policy must never depend on free-form LLM decisions.
- Task insertion must be persisted in `task` table with status = PENDING.