你现在的角色是：

1. 资深 Java 后端工程师
2. Spring Boot 3 / JDK17 / PostgreSQL / Flyway 项目重构顾问
3. LLM Prompt Engineering 工程化专家
4. AI Tutor 学习系统后端架构师

你的任务不是从零重写项目，而是：

**基于当前 AI Learning Workflow Navigator 项目中已有的 Prompt 模板体系，做一次“Prompt Engine 工程化升级”。**

目标：
1. 提升 Prompt 输出稳定性
2. 提升 Tutor 教学一致性
3. 提升评估结果可解释性
4. 为后续 Prompt A/B Test 与版本管理做好基础设施
5. 保持与当前现有业务结构兼容，避免大范围推翻重来

请严格按以下 TODO 顺序执行，并在每一步都给出：
- 修改的文件
- 新增的类/接口
- 修改原因
- 核心代码
- 必要注释
- 如涉及 DTO / 枚举 / Provider / Service，请补齐完整代码
- 最后输出一份本次改造总结

========================
一、先分析当前 Prompt 体系
========================

请先阅读并梳理当前项目中与 Prompt 相关的代码，重点关注但不限于：

- DefaultPromptTemplateProvider
- GoalDiagnosisService
- RealTutorProvider
- MockTutorProvider
- TutorMessageService
- 所有 LLM response parse 相关代码
- 可能存在的 DTO / record / VO / parser / provider 接口

先输出：
1. 当前 Prompt 分类
2. 当前 Prompt 输入变量
3. 当前 Prompt 输出字段
4. 当前存在的工程问题
5. 哪些改造可以低风险落地，哪些属于后续增强

要求：
- 不要泛泛而谈，要结合真实代码结构
- 不要脱离当前项目重造一套理想系统

========================
二、TODO-1：统一 Prompt 规范基类
========================

任务：
为当前 Prompt 体系增加统一规范，避免每个 Prompt 自己拼字符串、风格不一致。

请完成：

1. 设计一个统一的 PromptSpec / PromptTemplate / PromptDefinition 结构
2. 支持以下信息：
   - promptKey
   - promptVersion
   - systemPrompt
   - userPromptTemplate
   - expectedJsonSchemaText
   - outputRules
   - modelHint（可为空）
3. 保持对现有 DefaultPromptTemplateProvider 的兼容，优先小步重构
4. 不要引入复杂框架，保持简单清晰

输出要求：
- 给出类设计
- 给出核心实现
- 给出如何替换当前散乱模板常量的方式

目标：
让后续 Stage Prompt / Eval Prompt / Diagnosis Prompt / Tutor Prompt 都能通过统一结构管理。

========================
三、TODO-2：为 Stage Prompt 增加结构化 Schema 约束
========================

任务：
改造以下阶段内容生成 Prompt：

- STRUCTURE
- UNDERSTANDING
- TRAINING
- REFLECTION

要求：
1. 每个 Prompt 都补充 expected JSON schema 文本说明
2. 每个字段增加数量/长度约束
3. 增加“禁止输出 schema 外字段”的明确要求
4. 增加“字段值必须简体中文”的统一要求
5. 增加“内容必须紧扣当前知识点与任务目标，不泛化空谈”的约束

请直接给出每个阶段升级后的 Prompt 模板。

建议的改进方向：

STRUCTURE：
- title
- summary
- key_points (3~5)
- common_misconceptions (2~3)
- suggested_sequence (3~5)

UNDERSTANDING：
- concept_explanation
- analogy
- worked_example
- step_by_step_reasoning
- common_errors
- check_questions

TRAINING：
- questions (3~5)
- 至少包含：基础理解题、概念应用题、推理题
- 每题字段：id,type,question,reference_points,difficulty

REFLECTION：
- reflection_prompt
- review_checklist
- next_step_suggestion

额外要求：
- 每个字段给出长度控制建议
- 输出的 Prompt 文案保持工程可直接复制进 Java 常量

========================
四、TODO-3：增加 Tutor Prompt 的教学策略约束
========================

任务：
升级 RealTutorProvider 中的 Tutor System Prompt，使其从“普通问答助手”变成“学习流程导师”。

请实现：

1. 将当前 Tutor Prompt 从中英文混用改为统一风格（建议统一中文）
2. 增加教学策略规则：
   - 优先引导，不直接给答案
   - 先判断学生卡点
   - 能提问就先提问
   - 学生连续卡住时给 hint
   - 必要时给 partial answer
   - 学生理解正确时推进到更高一级问题
3. 设计 hint_mode / direct_answer_mode 的上下文开关（即便当前接口还没完全透出，也先把结构设计好）
4. 限制 Tutor 单次回复长度，避免长篇大论
5. 增加禁止幻觉规则：
   - 不编造课程上下文中不存在的事实
   - 不假设学生已经掌握未提供的前置知识

请输出：
- Tutor Prompt 新版模板
- 涉及的 Java 代码修改
- 如果需要，新增配置对象或上下文字段

========================
五、TODO-4：升级训练答案评估 Prompt，加入 Rubric
========================

任务：
改造 EVALUATE_PROMPT_V1，使打分更可解释，而不是只返回一个总分。

请完成：

1. 增加 rubric 评分维度：
   - concept_correctness (0-40)
   - reasoning_quality (0-30)
   - completeness (0-20)
   - clarity (0-10)
2. 总分为各维度求和
3. 保留：
   - score
   - normalized_score
   - feedback
   - error_tags
   - strengths
   - weaknesses
   - suggested_next_action
4. 新增：
   - rubric 字段
5. 明确：
   - normalized_score = score / 100
   - error_tags 控制在 2~4 个
   - strengths / weaknesses 各 2~3 条

请同步检查：
- 解析 JSON 的 DTO / record 是否需要扩展
- 数据库存储是否受影响
- 前端字段兼容是否需要保底处理

输出：
- 升级后的评估 Prompt
- DTO 修改
- parser 修改
- 兼容性说明

========================
六、TODO-5：升级 Goal Diagnosis，改成 SMART Rubric
========================

任务：
将学习目标诊断从模糊打分改成可解释的 SMART 评分。

请完成：

1. 评分维度改造为：
   - specific_score
   - measurable_score
   - achievable_score
   - relevant_score
   - time_bound_score
2. 总分 goal_score 仍为 0-100
3. 保留：
   - summary
   - strengths
   - risks
   - rewritten_goal
4. 新增 smart_breakdown 或 smart 字段用于返回 5 维评分
5. rewritten_goal 要求：
   - 一句话
   - 可执行
   - 可衡量
   - 适合大学生学习场景

请输出：
- 新版 Goal Diagnosis Prompt
- 相关 DTO / parser / service 修改
- 若有旧字段兼容问题请说明

========================
七、TODO-6：新增 Prompt Version 管理能力
========================

任务：
当前项目里虽然已有 V1 命名，但缺少真正工程化的版本管理。

请实现：

1. 为 Prompt 定义增加：
   - promptKey
   - promptVersion
2. 设计统一枚举，例如：
   - STRUCTURE_V1
   - UNDERSTANDING_V1
   - TRAINING_V1
   - REFLECTION_V1
   - EVALUATE_V1
   - GOAL_DIAGNOSE_V1
   - TUTOR_V1
3. 为后续升级预留：
   - STRUCTURE_V1_1
   - EVALUATE_V2
4. 在调用 LLM 时，尽量把当前使用的 promptKey + version 一并记录到日志
5. 若当前已有调用日志封装，则直接集成；若没有，则至少补充结构化日志

要求：
- 不强行上数据库
- 先实现代码层面的版本管理能力

========================
八、TODO-7：补充 Prompt 输出失败的兜底与校验
========================

任务：
提高 LLM 输出异常时的系统韧性。

请完成：

1. 检查当前 JSON parse 失败时的处理逻辑
2. 增加统一校验：
   - 缺字段
   - 字段类型错误
   - 数组长度不合法
   - 空字符串
3. 增加 fallback 策略：
   - 轻量字段修复（如果你认为合理）
   - 无法修复则返回标准错误对象
4. 保证：
   - 不因 LLM 格式问题导致整个核心链路直接 500
5. 给出推荐做法：
   - parser 层校验
   - service 层兜底
   - controller 层统一错误响应

输出：
- 校验工具类
- 兜底策略
- 关键代码修改

========================
九、TODO-8：新增一个知识点拆解 Prompt（可选增强，但建议落地）
========================

任务：
为后续“学习路径生成 / 知识图谱化”预埋能力，新增一个 Prompt：

目标：
给一个 chapter / concept / goal，拆成结构化 concept nodes。

输出建议结构：

{
  "concept_nodes": [
    {
      "id": "node1",
      "title": "xxx",
      "description": "xxx",
      "prerequisites": []
    }
  ]
}

要求：
1. 最多拆成 3~6 个节点
2. 节点标题简洁
3. prerequisite 关系合理
4. 不要过度图谱化，先适配当前 MVP
5. 如果当前项目已有 concept node/domain，请优先对齐现有结构

输出：
- Prompt 模板
- DTO
- 可挂接的 service 设计
- 标明这是增强项，不影响主链路

========================
十、TODO-9：补充单元测试 / 样例测试
========================

任务：
围绕本次 Prompt Engine 改造，补充最小必要测试。

至少覆盖：

1. Prompt 模板渲染测试
2. Eval Prompt DTO 解析测试
3. Goal Diagnosis DTO 解析测试
4. Stage Prompt schema 校验测试
5. Tutor Prompt 构造测试
6. LLM 异常 JSON 的兜底测试

要求：
- 使用当前项目已有测试风格
- 测试不要空壳
- 保证可以帮助后续重构

========================
十一、最终输出格式要求
========================

在完成所有改造后，请按以下结构输出结果：

1. 本次改造概览
2. 修改/新增文件清单
3. 每个 TODO 的实现说明
4. 核心代码
5. 风险点与兼容性说明
6. 后续建议（只写真正值得做的 3-5 条）

注意事项：
- 不要大面积改动无关业务
- 不要脱离当前项目架构重新发明轮子
- 优先保证现有功能可运行
- 所有代码风格尽量贴合当前项目
- 注释写中文，简洁明确






每个 TODO 的实现说明
TODO-1（统一 Prompt 规范基类）

新增 PromptSpec/PromptDefinition/PromptTemplateRenderer
DefaultPromptTemplateProvider 改为统一 definitions 注册 + 变量渲染
兼容方式：保留原 PromptTemplateProvider 入口，调用方不需要大改
TODO-2（Stage Prompt schema 约束）

STRUCTURE/UNDERSTANDING/TRAINING/REFLECTION 全部补了：
expected_json_schema
数量/长度约束
禁止 schema 外字段
简体中文要求
紧扣知识点与任务目标要求
TODO-3（Tutor 教学策略约束）

Tutor system prompt 统一中文风格
增加“先引导再答案、先识别卡点、可提问先提问、连续卡住给 hint、必要时 partial answer、理解正确后升级问题”
新增上下文开关：hintMode/directAnswerMode（TutorReplyMode）
单次回复长度做截断（防长篇）
幻觉约束已入系统提示
RealTutorProvider 改为使用统一 PromptProvider 生成 system prompt
TODO-4（Eval Rubric）

评估 Prompt 新增 rubric 四维度
EvaluationResult 增加 rubric、promptKey
SubmitTaskResponse 增加 rubric 字段
LlmAnswerEvaluator 加入校验 + repair（轻量修复：分数归一化、数组截断）
DB 不变（仍存 parsed_json / feedback_json，结构兼容）
TODO-5（Goal Diagnosis -> SMART）

Prompt 改为 SMART 五维打分 + goal_score
响应 DTO 增加 smart_breakdown
GoalDiagnosisService 改为 LLM 解析 SMART，不合法时 fallback 规则诊断
TODO-6（Prompt Version 管理）

PromptTemplateKey 升级为 promptKey + promptVersion
包含：STRUCTURE_V1...TUTOR_V1，并预留 STRUCTURE_V1_1、EVALUATE_V2
LlmPrompt/StageContent/EvaluationResult 都带 promptKey + promptVersion
OpenAiCompatibleLlmGateway 增加结构化日志输出 prompt key/version
TODO-7（输出失败兜底与校验）

新增 PromptOutputValidator，覆盖：
缺字段
类型错误
数组长度不合法
空字符串
额外字段检查
轻量修复：Eval 的 score/normalized_score、数组截断
兜底链路：
parser 异常 -> evaluator/stage service 抛错
service 层：CompositeAnswerEvaluator 回退 rule；TaskRunnerService 回退模板；GoalDiagnosisService 回退规则
controller 层：沿用全局异常映射，不直接 500 崩主链（有 fallback 时）
TODO-8（知识点拆解 Prompt，增强项）

新增 CONCEPT_DECOMPOSE_V1 Prompt
新增 DTO：ConceptNodeDecomposeResult
新增 service：ConceptNodeDecomposeService
标注：增强项，不接入主链路，不影响现有流程
TODO-9（测试）

已新增并更新测试覆盖：
Prompt 模板渲染
Eval 解析
Goal Diagnosis 解析
Stage schema 校验
Tutor prompt 构造
异常 JSON 兜底（Composite fallback）
本地执行：mvn test -q 通过
核心代码（节选）
public record PromptSpec(
    String promptKey,
    String promptVersion,
    String systemPrompt,
    String userPromptTemplate,
    String expectedJsonSchemaText,
    String outputRules,
    String modelHint
) {}
public enum PromptTemplateKey {
    STRUCTURE_V1("STRUCTURE", "v1"),
    STRUCTURE_V1_1("STRUCTURE", "v1.1"),
    UNDERSTANDING_V1("UNDERSTANDING", "v1"),
    TRAINING_V1("TRAINING", "v1"),
    REFLECTION_V1("REFLECTION", "v1"),
    EVALUATE_V1("EVALUATE", "v1"),
    EVALUATE_V2("EVALUATE", "v2"),
    GOAL_DIAGNOSE_V1("GOAL_DIAGNOSE", "v1"),
    TUTOR_V1("TUTOR", "v1"),
    CONCEPT_DECOMPOSE_V1("CONCEPT_DECOMPOSE", "v1");
}
// 统一组装：业务上下文 + schema + output rules
private LlmPrompt toPrompt(PromptDefinition definition, Map<String, String> variables) {
    PromptSpec spec = definition.spec();
    String rendered = renderer.render(spec.userPromptTemplate(), variables);
    String userPrompt = rendered
        + "\n\nexpected_json_schema:\n" + spec.expectedJsonSchemaText()
        + "\n\noutput_rules:\n" + spec.outputRules();
    return new LlmPrompt(
        definition.templateKey(),
        spec.promptKey(),
        spec.promptVersion(),
        spec.systemPrompt(),
        userPrompt,
        spec.expectedJsonSchemaText(),
        spec.outputRules(),
        spec.modelHint()
    );
}
// Eval: 先 repair，再 validate，不合法走 fallback 链路
promptOutputValidator.repairEvaluation(objectNode);
List<String> errors = promptOutputValidator.validateEvaluation(objectNode);
if (!errors.isEmpty()) {
    throw new InternalServerException("LLM evaluation output invalid: " + String.join("; ", errors));
}
// Tutor: 使用统一模板 + 模式开关
return promptTemplateProvider.buildTutorSystemPrompt(new TutorPromptContext(
    safe(request.taskStage()),
    safe(request.taskObjective()),
    safe(request.nodeName()),
    safe(request.sessionGoal()),
    request.hintMode(),
    request.directAnswerMode()
));
风险点与兼容性说明
主链路兼容：保持现有 service 接口和数据库表结构，不引入新中间件
数据库影响：无 schema 变更；llm_call_log 继续使用现有字段
前端兼容：
SubmitTaskResponse 新增 rubric，旧前端可忽略，不破坏已存在字段
Goal 诊断新增 smart_breakdown，旧字段保留
潜在风险：
Stage 输出现在更严格，LLM 不合格时更容易进入 fallback（这是预期的稳态行为）
如果后续前端要展示 rubric/smart_breakdown，需加展示映射
后续建议（3-5 条）

把 PromptTemplateKey 与 PromptSpec 导出到可观测面板（按 key/version 统计成功率、fallback 率）

增加 Prompt A/B 路由开关（按 sessionId hash 或灰度比例）

在 llm_call_log.parsed_json 上补查询视图，便于按 rubric/SMART 维度做教学分析

给 Tutor 增加“连续卡点计数”持久化（当前仅预留模式字段）

前端补充 rubric 与 smart_breakdown 展示，提高可解释性闭环