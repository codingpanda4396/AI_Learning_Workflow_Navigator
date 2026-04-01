最适合放进演示视频的，是这 4 组。它们分支最明显，观众一眼就能看出“同一个系统，会因诊断不同而给出不同规划”。

**方案 1**
零基础入门，走“基础修补”

目标输入：
`我想先搞懂二叉树的基本结构`

诊断选择：
1. `q_goal_outcome`: `先过关，保住基础` `PASS_THE_BASICS`
2. `q_foundation_state`: `刚开始接触，基本不了解` `BEGINNER`
3. `q_primary_gap`: `概念本身没弄懂` `CONCEPT_GAP`
4. `q_scope_of_problem`: `主要卡在一个明确知识点` `SINGLE_POINT`
5. `q_preferred_entry_mode`: `先把核心概念讲清楚` `CONCEPT_FIRST`
6. `q_execution_risk`: `内容一多就容易乱` `COGNITIVE_OVERLOAD_RISK`

对应结果：
- 推荐策略：`FOUNDATION_PATCH`，前端会显示成“基础修补”
- 推荐起点：先理解该主题的定义、组成与最小示例
- 阶段路径：`基础澄清 -> 自解释校准`
- 任务风格：先讲概念，再给最小例子，再让用户复述，最后做检查
- 演示感受：系统像在“扶着新手起步”

**方案 2**
考前快复习，走“冲刺纠偏”

目标输入：
`明天考 408，我想快速复习 DFS 和 BFS`

诊断选择：
1. `q_goal_outcome`: `希望能独立做典型题` `SOLVE_TYPICAL_PROBLEMS`
2. `q_foundation_state`: `概念基本懂，但不太稳` `CAN_EXPLAIN_BUT_NOT_STABLE`
3. `q_primary_gap`: `看到题时分不清题型` `QUESTION_TYPE_RECOGNITION_GAP`
4. `q_scope_of_problem`: `卡在几个零散点` `MULTI_POINT`
5. `q_preferred_entry_mode`: `先讲容易混淆的点和关键区别` `CORE_CONTRAST_FIRST`
6. `q_execution_risk`: `时间太紧，学不完整` `TIME_PRESSURE`

对应结果：
- 推荐策略：`SPRINT_CORRECTION`，前端会显示成“冲刺纠偏”
- 推荐起点：先对比 DFS/BFS 的核心差异与典型题入口
- 阶段路径：`核心对比 -> 典型题快练`
- 任务风格：先对比，再看一题带讲解，再做 1 道微练习，最后检查
- 演示感受：系统像在“考前帮你抓最容易丢分的点”

**方案 3**
单点卡住，走“局部修补”

目标输入：
`我链表插入删除总是写错，想先补这个卡点`

诊断选择：
1. `q_goal_outcome`: `先补一个明确卡点` `FILL_A_SPECIFIC_GAP`
2. `q_foundation_state`: `整体还可以，只是有局部薄弱点` `SOLID_WITH_LOCAL_GAPS`
3. `q_primary_gap`: `知道原理，但题目上不会下手` `PROCEDURE_GAP`
4. `q_scope_of_problem`: `主要卡在一个明确知识点` `SINGLE_POINT`
5. `q_preferred_entry_mode`: `先看具体例子，再回到概念` `EXAMPLE_FIRST`
6. `q_execution_risk`: `目前主要不是执行问题` `LOW_RISK`

对应结果：
- 推荐策略：`LOCAL_REPAIR`，前端会显示成“局部修补”
- 推荐起点：先定点修补当前卡点
- 阶段路径：`卡点定位 -> 定点修补`
- 任务风格：先让用户暴露自己到底哪里不会，再给针对性讲解和例子，最后检查是否解除卡点
- 演示感受：系统像在“精准维修”，不是重讲整章

**方案 4**
系统学习一整块内容，走“框架搭建”

目标输入：
`我想系统过一轮 408 数据结构，先把整体框架搭起来`

诊断选择：
1. `q_goal_outcome`: `先建立整体框架` `BUILD_FRAMEWORK`
2. `q_foundation_state`: `知道一点基础，但不稳定` `BASIC_BUT_FRAGILE`
3. `q_primary_gap`: `知识点之间串不起来` `RELATIONSHIP_GAP`
4. `q_scope_of_problem`: `不是某一章，是整体都比较乱` `COURSE_LEVEL`
5. `q_preferred_entry_mode`: `先给整体框架，再逐步填细节` `FRAMEWORK_FIRST`
6. `q_execution_risk`: `容易中断，今天学了明天接不上` `CONTINUITY_RISK`

对应结果：
- 推荐策略：`FRAMEWORK_BUILD`，前端会显示成“框架搭建”
- 推荐起点：先建立知识框架与主题关系图
- 阶段路径：`框架搭建 -> 局部填充 -> 典型题连接`
- 任务风格：先搭知识图，再补关键主题，再把框架和题目连起来
- 演示感受：系统像在“带用户建地图”，不是只给几个任务点

如果你还想再补一个更偏“刷题提分”的版本，可以拍第 5 组：

**方案 5**
会一点但做题不稳，走“刷题强化”

目标输入：
`我想提高 DFS/BFS 做题正确率`

诊断选择：
1. `q_goal_outcome`: `希望能独立做典型题` `SOLVE_TYPICAL_PROBLEMS`
2. `q_foundation_state`: `概念基本懂，但不会做题` `CAN_EXPLAIN_BUT_NOT_APPLY`
3. `q_primary_gap`: `知道原理，但不会下手` `PROCEDURE_GAP`
4. `q_scope_of_problem`: `卡在几个零散点` `MULTI_POINT`
5. `q_preferred_entry_mode`: `先做题，在过程中带着学` `PRACTICE_FIRST`
6. `q_execution_risk`: `目前主要不是执行问题` `LOW_RISK`

对应结果：
- 推荐策略：`DRILL_STRENGTHEN`，前端会显示成“刷题强化”
- 推荐起点：先做 1 个带讲解的典型例题
- 阶段路径：`题型识别 -> 微练习纠偏`
- 任务风格：例题带路，小练习纠偏，再要求用户解释为什么这么做

最建议你的视频拍法是：
1. 拍 `方案 1` 和 `方案 2` 对比，差异最大。
2. 再拍 `方案 3`，突出“系统不是只会重讲，而是会定点修补”。
3. 如果时长够，再加 `方案 4`，展示“系统学习”和“局部修补”是两种完全不同的规划逻辑。

如果你愿意，我下一步可以直接帮你整理成一版“演示视频分镜脚本”，包括每一幕该点哪个选项、镜头里要强调哪句文案。