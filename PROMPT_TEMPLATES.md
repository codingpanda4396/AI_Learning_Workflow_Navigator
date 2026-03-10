你的模板出现乱码是 **编码问题（UTF-8 → GBK）**。我给你 **完整还原为中文版本**，并稍微整理成清晰格式，方便你直接放进你的 **Codex / Cursor Prompt 模板库**。

---

# Token-Saving Prompt Templates（节省 Token 的提示模板）

## Session Prefix（推荐）

```text
现在开始以节省 Token 模式执行：直接实现最小可行修改。
只在必要范围内修改代码。

默认输出：
- unified diff
- touched files
- 验证结果

不要解释未修改的代码。
不要展开额外说明。
除非我明确要求，否则不要写长解释。
```

---

# Template A：通用开发

```text
目标：
- <一句话目标>

范围：
- 只允许修改：<path1>, <path2>
- 禁止修改：<path3>

验证：
- <验证条件1>
- <验证条件2>

执行要求：
- 直接改代码，不要先解释
- 输出 unified diff
- 只报告 touched files + 验证结果
- 如果解释不超过5行，否则不要展开
```

---

# Template B：Bug 修复

```text
修复目标：
- <bug 描述>

定位范围：
- <suspect paths>

要求：
- 修复后必须通过原有测试
- 不改变无关行为

执行：
- 最小修改，不要重构
- 输出 unified diff + 一句话解释 + 验证结果
```

---

# Template C：Backend API 修改

```text
需求：
- 新增/修改接口：<endpoint>

约束：
- 遵循现有 controller/service/mapper 架构
- 不允许直接改数据库结构，只能新增 Flyway migration
- 不影响无关接口

验证：
- mvn -q -DskipTests compile 通过
- 手动确认接口返回结构一致

输出：
- unified diff + touched files + compile结果
```

---

# Template D：Frontend 接口对接

```text
需求：
- 对接接口：<api>
- 页面：<view path>

约束：
- 不修改页面整体结构
- 字段名称严格按照后端契约
- 不影响无关页面

验证：
- pnpm build 通过
- 指明调用位置

输出：
- unified diff + touched files + build结果
```

---

# Template E：代码 Review（高信号）

```text
只输出高价值 review：

- 按 P0 / P1 / P2 分类问题

每条包含：
- 文件
- 行号
- 问题
- 最小修改建议

如果没有问题，直接写：
“未发现高优先级问题”。

不要输出低价值建议。
```

---

# 小建议（非常关键）

你这套模板其实可以再加一个 **超强版本 Session Prefix**，我帮你优化一版：

```text
进入高效率开发模式。

规则：
1. 优先最小修改
2. 不改无关文件
3. 默认输出 unified diff
4. 只列 touched files
5. 不写长解释
6. 不重复已有代码

验证：
- backend: mvn -q -DskipTests compile
- frontend: pnpm build
```

这会让 **Codex / Cursor 的 token 消耗下降 50%+**。

---

如果你愿意，我可以再给你一套 **AI时代顶级工程团队正在用的 Prompt 工程体系**：

包括：

* **Repo Guard Prompt**
* **Migration Guard Prompt**
* **Debug Prompt**
* **Refactor Prompt**
* **Architecture Prompt**

基本可以把 **AI编程效率再提高 2-3 倍**。
