# Token-Saving Prompt Templates

## Session Prefix (Recommended)
```text
从现在开始按省额度模式执行：直接实现、最小改动、禁止范围外修改、默认输出 unified diff + touched files + 验证结果；不复述需求，不贴大段代码，不做额外解释；除非我说“展开”。
```

## Template A: General Development
```text
目标:
- <一句话目标>

范围:
- 只允许修改: <path1>, <path2>
- 禁止修改: <path3>

验收:
- <可验证条件1>
- <可验证条件2>

执行要求:
- 直接改代码，不先讲方案
- 输出 unified diff
- 只汇报 touched files + 验证结果
- 解释不超过5行，除非我说“展开”
```

## Template B: Bug Fix
```text
修复目标:
- <bug现象>

定位范围:
- <suspect paths>

验收:
- 复现步骤通过/失败条件消失
- 不改变无关行为

执行:
- 最小修复，不重构
- 输出 unified diff + 根因一句话 + 验证结果
```

## Template C: Backend API Change
```text
任务:
- 新增/修改接口: <endpoint>

限制:
- 遵循现有 controller/service/mapper 风格
- 如需改库，只能加 Flyway migration
- 不改无关接口

验收:
- mvn -q -DskipTests compile 通过
- 字段命名与现有契约一致

输出:
- unified diff + touched files + compile结果
```

## Template D: Frontend Integration
```text
任务:
- 对接接口: <api>
- 页面: <view path>

限制:
- 不改页面整体设计
- 字段名严格按后端契约
- 不改无关页面

验收:
- pnpm build 通过
- 指定交互可用

输出:
- unified diff + touched files + build结果
```

## Template E: Review (High Signal Only)
```text
请只做高价值review:
- 仅输出 P0/P1/P2 问题
- 每条: 文件+行号+风险+最小修复建议
- 无问题就明确写“未发现高优先级问题”
- 不要给风格类建议
```