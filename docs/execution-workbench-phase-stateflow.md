# 执行页跳转与状态说明

## 约束总则

- URL 只承载 `phase`：`structure | understanding | training | reflection`
- `renderState` 只在页面内存中流转，不进入 URL
- `renderState` 固定四态：`prompt -> think -> output -> feedback`

## 路由同步

- 切阶段使用：
  - `router.replace({ query: { ...route.query, phase: nextPhase } })`
- 未携带 phase 时，按当前阶段自动回填 query

## 阶段内时序

### STRUCTURE

1. 进入：`phase=structure`，`renderState=prompt`
2. 用户选项：记录选择并锁定，进入 `think -> output`
3. 系统反馈：显示 `PhaseFeedbackCard`，进入 `feedback`
4. CTA：进入 `understanding`，状态重置到 `prompt`

### UNDERSTANDING

1. 进入：`phase=understanding`，`renderState=prompt`
2. 用户选项：进入 `think -> output`
3. 系统反馈：进入 `feedback`
4. CTA：进入 `training`，状态重置到 `prompt`

### TRAINING

1. 进入：`phase=training`，`renderState=prompt`
2. 用户输入：进入 `think`
3. 点击提交：进入 `output`
4. 系统反馈：进入 `feedback`
5. CTA：进入 `reflection`，状态重置到 `prompt`

### REFLECTION

1. 进入：`phase=reflection`，`renderState=prompt`
2. 选择反思项/策略：进入 `think`
3. 提交反思：进入 `output`
4. 系统确认：进入 `feedback`
5. CTA：跳转报告页
