# Step 3/4/5 代码梳理交付

## 目录总览
- 1) Controller 代码与接口标注
- 2) DTO 代码与字段表
- 3) application/service/usecase 代码与用例流程
- 4) domain 代码（枚举/模型/策略接口）与规则
- 5) infrastructure/persistence 代码与表映射
- 6) Flyway 迁移与最终表结构视图
- 7) 前端页面草图/字段需求（基于现有后端）
- 8) 页面-接口对齐表
- 9) 最小改造优先方案（P0/P1/P2）
- 10) 给 Cursor/Codex 的工程任务清单

## 1) Controller 代码（完整）
### 文件路径
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\controller\DbDebugController.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\controller\HealthController.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\controller\SessionController.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\controller\TaskController.java

### 接口标注（method + path + dto + 关键异常）
- `GET /health` -> req: 无, resp: `ApiResponse`, 异常: 通用 500
- `GET /debug/db` -> req: 无, resp: `Map<String,Object>`, 异常: DB/Flyway 运行异常
- `POST /api/session/create` -> req: `CreateSessionRequest`, resp: `CreateSessionResponse`, 关键异常: 400/500（全局处理含409）
- `POST /api/session/{sessionId}/plan` -> req: path `sessionId`, resp: `PlanSessionResponse`, 关键异常: 404/500
- `GET /api/session/{sessionId}/overview` -> req: path `sessionId`, resp: `SessionOverviewResponse`, 关键异常: 404/500
- `POST /api/task/{taskId}/run` -> req: path `taskId`, resp: `RunTaskResponse`, 关键异常: 404/409/500
- `POST /api/task/{taskId}/submit` -> req: `SubmitTaskRequest`, resp: `SubmitTaskResponse`, 关键异常: 400/404/409/500

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\controller\DbDebugController.java
```java
package com.pandanav.learning.api.controller;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/debug")
public class DbDebugController {

    private final Flyway flyway;
    private final JdbcTemplate jdbcTemplate;

    public DbDebugController(Flyway flyway, JdbcTemplate jdbcTemplate) {
        this.flyway = flyway;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/db")
    public Map<String, Object> db() {
        Integer selectOne = jdbcTemplate.queryForObject("select 1", Integer.class);

        List<Map<String, Object>> migrations = Arrays.stream(flyway.info().all())
            .map(this::toMigrationInfo)
            .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("db", "ok");
        result.put("select1", selectOne);
        result.put("migrationCount", migrations.size());
        result.put("migrations", migrations);
        return result;
    }

    private Map<String, Object> toMigrationInfo(MigrationInfo info) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("version", info.getVersion() == null ? null : info.getVersion().getVersion());
        map.put("description", info.getDescription());
        map.put("script", info.getScript());
        map.put("state", info.getState().getDisplayName());
        return map;
    }
}
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\controller\HealthController.java
```java
package com.pandanav.learning.api.controller;

import com.pandanav.learning.api.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public ApiResponse health() {
        return ApiResponse.ok();
    }
}
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\controller\SessionController.java
```java
package com.pandanav.learning.api.controller;

import com.pandanav.learning.api.dto.ApiErrorResponse;
import com.pandanav.learning.api.dto.session.CreateSessionRequest;
import com.pandanav.learning.api.dto.session.CreateSessionResponse;
import com.pandanav.learning.api.dto.session.PlanSessionResponse;
import com.pandanav.learning.api.dto.session.SessionOverviewResponse;
import com.pandanav.learning.application.usecase.CreateSessionUseCase;
import com.pandanav.learning.application.usecase.GetSessionOverviewUseCase;
import com.pandanav.learning.application.usecase.PlanSessionTasksUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/session")
public class SessionController {

    private final CreateSessionUseCase createSessionUseCase;
    private final PlanSessionTasksUseCase planSessionTasksUseCase;
    private final GetSessionOverviewUseCase getSessionOverviewUseCase;

    public SessionController(
        CreateSessionUseCase createSessionUseCase,
        PlanSessionTasksUseCase planSessionTasksUseCase,
        GetSessionOverviewUseCase getSessionOverviewUseCase
    ) {
        this.createSessionUseCase = createSessionUseCase;
        this.planSessionTasksUseCase = planSessionTasksUseCase;
        this.getSessionOverviewUseCase = getSessionOverviewUseCase;
    }

    @Operation(summary = "Create learning session")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Bad Request",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal Error",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/create")
    public CreateSessionResponse createSession(@Valid @RequestBody CreateSessionRequest request) {
        return createSessionUseCase.execute(request);
    }

    @Operation(summary = "Plan session tasks")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal Error",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/{sessionId}/plan")
    public PlanSessionResponse planSession(@PathVariable @Positive Long sessionId) {
        return planSessionTasksUseCase.execute(sessionId);
    }

    @Operation(summary = "Get session overview")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal Error",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{sessionId}/overview")
    public SessionOverviewResponse getOverview(@PathVariable @Positive Long sessionId) {
        return getSessionOverviewUseCase.execute(sessionId);
    }
}
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\controller\TaskController.java
```java
package com.pandanav.learning.api.controller;

import com.pandanav.learning.api.dto.ApiErrorResponse;
import com.pandanav.learning.api.dto.task.RunTaskResponse;
import com.pandanav.learning.api.dto.task.SubmitTaskRequest;
import com.pandanav.learning.api.dto.task.SubmitTaskResponse;
import com.pandanav.learning.application.usecase.RunTaskUseCase;
import com.pandanav.learning.application.usecase.SubmitTrainingAnswerUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/task")
public class TaskController {

    private final RunTaskUseCase runTaskUseCase;
    private final SubmitTrainingAnswerUseCase submitTrainingAnswerUseCase;

    public TaskController(RunTaskUseCase runTaskUseCase, SubmitTrainingAnswerUseCase submitTrainingAnswerUseCase) {
        this.runTaskUseCase = runTaskUseCase;
        this.submitTrainingAnswerUseCase = submitTrainingAnswerUseCase;
    }

    @Operation(summary = "Run task")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal Error",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/{taskId}/run")
    public RunTaskResponse runTask(@PathVariable @Positive Long taskId) {
        return runTaskUseCase.run(taskId);
    }

    @Operation(summary = "Submit training answer")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Bad Request",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Conflict",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal Error",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/{taskId}/submit")
    public SubmitTaskResponse submitTask(
        @PathVariable @Positive Long taskId,
        @Valid @RequestBody SubmitTaskRequest request
    ) {
        return submitTrainingAnswerUseCase.submit(taskId, request);
    }
}


```

## 2) DTO 代码（完整）
### 文件路径
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\dto\ApiErrorResponse.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\dto\ApiResponse.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\dto\ErrorResponse.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\dto\session\CreateSessionRequest.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\dto\session\CreateSessionResponse.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\dto\session\MasterySummaryResponse.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\dto\session\NextTaskResponse.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\dto\session\PlannedTaskResponse.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\dto\session\PlanSessionResponse.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\dto\session\SessionOverviewResponse.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\dto\session\TimelineItemResponse.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\dto\task\FeedbackResponse.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\dto\task\RunTaskResponse.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\dto\task\SubmitTaskRequest.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\dto\task\SubmitTaskResponse.java

### DTO 字段汇总表（按代码静态梳理）
| DTO | 字段名 | 类型 | 必填 | 示例值 | 备注 |
|---|---|---|---|---|---|
| CreateSessionRequest | user_id | String | 是 | mock_openid_001 | @NotBlank |
| CreateSessionRequest | course_id | String | 是 | computer_network | @NotBlank |
| CreateSessionRequest | chapter_id | String | 是 | tcp | @NotBlank |
| CreateSessionRequest | goal_text | String | 否 | 理解TCP机制 | 目标描述 |
| CreateSessionResponse | session_id | Long | 是 | 123 | 创建会话返回 |
| PlanSessionResponse | session_id | Long | 是 | 123 | |
| PlanSessionResponse | tasks | List<PlannedTaskResponse> | 是 | [...] | |
| PlannedTaskResponse | task_id/stage/node_id/objective/status | Long/String/... | 是 | 1001/TRAINING/101/... | 时间线任务项 |
| SessionOverviewResponse | session_id/course_id/chapter_id/goal_text/current_node_id/current_stage/timeline/next_task/mastery_summary | 复合 | 是 | - | 会话总览 |
| TimelineItemResponse | task_id/stage/node_id/status | Long/String | 是 | - | 时间线项 |
| NextTaskResponse | task_id/stage/node_id | Long/String | 是 | - | 下一个任务 |
| MasterySummaryResponse | node_id/node_name/mastery_value | Long/String/BigDecimal | 是 | 101/三次握手/0.55 | 掌握度概览 |
| RunTaskResponse | task_id/stage/node_id/status/output | Long/String/JsonNode | 是 | - | run 输出 |
| SubmitTaskRequest | user_answer | String | 是 | I think... | @NotBlank |
| SubmitTaskResponse | task_id/stage/node_id/score/error_tags/feedback/mastery_before/mastery_delta/mastery_after/next_action/next_task | 复合 | 是 | - | submit 返回 contract |
| FeedbackResponse | diagnosis/fixes | String/List<String> | 是 | - | 评估反馈 |
| ApiErrorResponse | error/message | String | 是 | BAD_REQUEST/... | 统一错误 |
| ApiResponse | status | String | 是 | ok | health 用 |
| ErrorResponse | error/message | String | 是 | ... | 旧错误DTO（未广泛使用） |

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\dto\ApiErrorResponse.java
```java
package com.pandanav.learning.api.dto;

public record ApiErrorResponse(
    String error,
    String message
) {
}
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\dto\ApiResponse.java
```java
package com.pandanav.learning.api.dto;

public record ApiResponse(String status) {

    public static ApiResponse ok() {
        return new ApiResponse("ok");
    }
}
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\dto\ErrorResponse.java
```java
package com.pandanav.learning.api.dto;

import java.time.OffsetDateTime;

public record ErrorResponse(
    String code,
    String message,
    OffsetDateTime timestamp
) {
}
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\dto\session\CreateSessionRequest.java
```java
package com.pandanav.learning.api.dto.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "CreateSessionRequest")
public record CreateSessionRequest(
    @NotBlank
    @JsonProperty("user_id")
    @Schema(name = "user_id", example = "mock_openid_001")
    String userId,
    @NotBlank
    @JsonProperty("course_id")
    @Schema(name = "course_id", example = "computer_network")
    String courseId,
    @NotBlank
    @JsonProperty("chapter_id")
    @Schema(name = "chapter_id", example = "tcp")
    String chapterId,
    @JsonProperty("goal_text")
    @Schema(name = "goal_text", example = "鐞嗚В TCP 鍙潬浼犺緭鏈哄埗骞惰兘鍋氶")
    String goalText
) {
}
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\dto\session\CreateSessionResponse.java
```java
package com.pandanav.learning.api.dto.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CreateSessionResponse")
public record CreateSessionResponse(
    @JsonProperty("session_id")
    @Schema(name = "session_id", example = "123")
    Long sessionId
) {
}
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\dto\session\MasterySummaryResponse.java
```java
package com.pandanav.learning.api.dto.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name = "MasterySummaryResponse")
public record MasterySummaryResponse(
    @JsonProperty("node_id")
    @Schema(name = "node_id", example = "101")
    Long nodeId,
    @JsonProperty("node_name")
    @Schema(name = "node_name", example = "涓夋鎻℃墜")
    String nodeName,
    @JsonProperty("mastery_value")
    @Schema(name = "mastery_value", example = "0.55")
    BigDecimal masteryValue
) {
}
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\dto\session\NextTaskResponse.java
```java
package com.pandanav.learning.api.dto.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "NextTaskResponse")
public record NextTaskResponse(
    @JsonProperty("task_id")
    @Schema(name = "task_id", example = "1002")
    Long taskId,
    @Schema(example = "UNDERSTANDING")
    String stage,
    @JsonProperty("node_id")
    @Schema(name = "node_id", example = "101")
    Long nodeId
) {
}
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\dto\session\PlannedTaskResponse.java
```java
package com.pandanav.learning.api.dto.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PlannedTaskResponse")
public record PlannedTaskResponse(
    @JsonProperty("task_id")
    @Schema(name = "task_id", example = "1001")
    Long taskId,
    @Schema(example = "STRUCTURE")
    String stage,
    @JsonProperty("node_id")
    @Schema(name = "node_id", example = "101")
    Long nodeId,
    @Schema(example = "鍥寸粫涓夋鎻℃墜鏋勫缓缁撴瀯")
    String objective,
    @Schema(example = "PENDING")
    String status
) {
}
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\dto\session\PlanSessionResponse.java
```java
package com.pandanav.learning.api.dto.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "PlanSessionResponse")
public record PlanSessionResponse(
    @JsonProperty("session_id")
    @Schema(name = "session_id", example = "123")
    Long sessionId,
    List<PlannedTaskResponse> tasks
) {
}
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\dto\session\SessionOverviewResponse.java
```java
package com.pandanav.learning.api.dto.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "SessionOverviewResponse")
public record SessionOverviewResponse(
    @JsonProperty("session_id")
    @Schema(name = "session_id", example = "123")
    Long sessionId,
    @JsonProperty("course_id")
    @Schema(name = "course_id", example = "computer_network")
    String courseId,
    @JsonProperty("chapter_id")
    @Schema(name = "chapter_id", example = "tcp")
    String chapterId,
    @JsonProperty("goal_text")
    @Schema(name = "goal_text", example = "鐞嗚В TCP 鍙潬浼犺緭鏈哄埗骞惰兘鍋氶")
    String goalText,
    @JsonProperty("current_node_id")
    @Schema(name = "current_node_id", example = "101")
    Long currentNodeId,
    @JsonProperty("current_stage")
    @Schema(name = "current_stage", example = "UNDERSTANDING")
    String currentStage,
    List<TimelineItemResponse> timeline,
    @JsonProperty("next_task")
    NextTaskResponse nextTask,
    @JsonProperty("mastery_summary")
    List<MasterySummaryResponse> masterySummary
) {
}
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\dto\session\TimelineItemResponse.java
```java
package com.pandanav.learning.api.dto.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TimelineItemResponse")
public record TimelineItemResponse(
    @JsonProperty("task_id")
    @Schema(name = "task_id", example = "1001")
    Long taskId,
    @Schema(example = "STRUCTURE")
    String stage,
    @JsonProperty("node_id")
    @Schema(name = "node_id", example = "101")
    Long nodeId,
    @Schema(example = "SUCCEEDED")
    String status
) {
}
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\dto\task\FeedbackResponse.java
```java
package com.pandanav.learning.api.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "FeedbackResponse")
public record FeedbackResponse(
    @Schema(example = "Explanation misses key causal steps.")
    String diagnosis,
    List<String> fixes
) {
}
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\dto\task\RunTaskResponse.java
```java
package com.pandanav.learning.api.dto.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RunTaskResponse")
public record RunTaskResponse(
    @JsonProperty("task_id")
    @Schema(name = "task_id", example = "1002")
    Long taskId,
    @Schema(example = "UNDERSTANDING")
    String stage,
    @JsonProperty("node_id")
    @Schema(name = "node_id", example = "101")
    Long nodeId,
    @Schema(example = "SUCCEEDED")
    String status,
    JsonNode output
) {
}
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\dto\task\SubmitTaskRequest.java
```java
package com.pandanav.learning.api.dto.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "SubmitTaskRequest")
public record SubmitTaskRequest(
    @NotBlank
    @JsonProperty("user_answer")
    @Schema(name = "user_answer", example = "I think two-way handshake is enough...")
    String userAnswer
) {
}
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\api\dto\task\SubmitTaskResponse.java
```java
package com.pandanav.learning.api.dto.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pandanav.learning.api.dto.session.NextTaskResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(name = "SubmitTaskResponse")
public record SubmitTaskResponse(
    @JsonProperty("task_id")
    @Schema(name = "task_id", example = "1003")
    Long taskId,
    @Schema(example = "TRAINING")
    String stage,
    @JsonProperty("node_id")
    @Schema(name = "node_id", example = "101")
    Long nodeId,
    @Schema(example = "72")
    Integer score,
    @JsonProperty("error_tags")
    List<String> errorTags,
    FeedbackResponse feedback,
    @JsonProperty("mastery_before")
    BigDecimal masteryBefore,
    @JsonProperty("mastery_delta")
    BigDecimal masteryDelta,
    @JsonProperty("mastery_after")
    BigDecimal masteryAfter,
    @JsonProperty("next_action")
    String nextAction,
    @JsonProperty("next_task")
    NextTaskResponse nextTask
) {
}
```

## 3) application/service/usecase 代码（完整）
### UseCase 接口路径
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\usecase\CreateSessionUseCase.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\usecase\GenerateTasksUseCase.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\usecase\GetSessionOverviewUseCase.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\usecase\GetSessionUseCase.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\usecase\PlanSessionTasksUseCase.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\usecase\RunTaskUseCase.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\usecase\StartSessionUseCase.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\usecase\SubmitTrainingAnswerUseCase.java

### Service 实现路径
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\service\CreateSessionService.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\service\EvaluatorService.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\service\GenerateTasksService.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\service\GetSessionOverviewService.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\service\GetSessionService.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\service\MasteryUpdateService.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\service\NextActionPolicyService.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\service\package-info.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\service\PlanSessionTasksService.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\service\StartSessionService.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\service\SubmitTrainingAnswerService.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\service\TaskApplicationService.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\service\TaskRunnerService.java

### 用例流程摘要（输入 -> 核心步骤 -> 输出）
- CreateSession: `CreateSessionRequest` -> 查询章节首节点/创建 learning_session -> `CreateSessionResponse`
- PlanSessionTasks: `sessionId` -> 拉取章节节点/按 stage 生成 task -> `PlanSessionResponse`
- GetSessionOverview: `sessionId` -> 会话+任务+mastery 聚合 -> `SessionOverviewResponse`
- RunTask: `taskId` -> 幂等检查/状态校验/生成输出并写入 task_attempt -> `RunTaskResponse`
- SubmitTrainingAnswer: `taskId + user_answer` -> task校验/evaluator/mastery/evidence/policy/next_task -> `SubmitTaskResponse`
- 其余 usecase/service：`StartSession/GenerateTasks/GetSession` 当前为骨架或未实现。

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\usecase\CreateSessionUseCase.java
```java
package com.pandanav.learning.application.usecase;

import com.pandanav.learning.api.dto.session.CreateSessionRequest;
import com.pandanav.learning.api.dto.session.CreateSessionResponse;

public interface CreateSessionUseCase {

    CreateSessionResponse execute(CreateSessionRequest request);
}
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\usecase\GenerateTasksUseCase.java
```java
package com.pandanav.learning.application.usecase;

import com.pandanav.learning.application.command.GenerateTasksCommand;
import com.pandanav.learning.domain.model.LearningTask;

import java.util.List;

public interface GenerateTasksUseCase {

    List<LearningTask> execute(GenerateTasksCommand command);
}
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\usecase\GetSessionOverviewUseCase.java
```java
package com.pandanav.learning.application.usecase;

import com.pandanav.learning.api.dto.session.SessionOverviewResponse;

public interface GetSessionOverviewUseCase {

    SessionOverviewResponse execute(Long sessionId);
}
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\usecase\GetSessionUseCase.java
```java
package com.pandanav.learning.application.usecase;

import com.pandanav.learning.application.query.GetSessionQuery;
import com.pandanav.learning.domain.model.LearningSession;

import java.util.Optional;

public interface GetSessionUseCase {

    Optional<LearningSession> execute(GetSessionQuery query);
}
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\usecase\PlanSessionTasksUseCase.java
```java
package com.pandanav.learning.application.usecase;

import com.pandanav.learning.api.dto.session.PlanSessionResponse;

public interface PlanSessionTasksUseCase {

    PlanSessionResponse execute(Long sessionId);
}
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\usecase\RunTaskUseCase.java
```java
package com.pandanav.learning.application.usecase;

import com.pandanav.learning.api.dto.task.RunTaskResponse;

public interface RunTaskUseCase {

    RunTaskResponse run(Long taskId);
}
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\usecase\StartSessionUseCase.java
```java
package com.pandanav.learning.application.usecase;

import com.pandanav.learning.application.command.StartSessionCommand;
import com.pandanav.learning.domain.model.LearningSession;

public interface StartSessionUseCase {

    LearningSession execute(StartSessionCommand command);
}
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\usecase\SubmitTrainingAnswerUseCase.java
```java
package com.pandanav.learning.application.usecase;

import com.pandanav.learning.api.dto.task.SubmitTaskRequest;
import com.pandanav.learning.api.dto.task.SubmitTaskResponse;

public interface SubmitTrainingAnswerUseCase {

    SubmitTaskResponse submit(Long taskId, SubmitTaskRequest request);
}


```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\service\CreateSessionService.java
```java
package com.pandanav.learning.application.service;

import com.pandanav.learning.api.dto.session.CreateSessionRequest;
import com.pandanav.learning.api.dto.session.CreateSessionResponse;
import com.pandanav.learning.application.usecase.CreateSessionUseCase;
import com.pandanav.learning.domain.model.ConceptNode;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.infrastructure.exception.ConflictException;
import com.pandanav.learning.infrastructure.exception.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class CreateSessionService implements CreateSessionUseCase {

    private final SessionRepository sessionRepository;
    private final ConceptNodeRepository conceptNodeRepository;

    public CreateSessionService(SessionRepository sessionRepository, ConceptNodeRepository conceptNodeRepository) {
        this.sessionRepository = sessionRepository;
        this.conceptNodeRepository = conceptNodeRepository;
    }

    @Override
    public CreateSessionResponse execute(CreateSessionRequest request) {
        ConceptNode firstNode = conceptNodeRepository.findFirstByChapterIdOrderByOrderNoAsc(request.chapterId())
            .orElseThrow(() -> new NotFoundException("No concept nodes found for chapter."));

        LearningSession session = new LearningSession();
        session.setUserId(request.userId());
        session.setCourseId(request.courseId());
        session.setChapterId(request.chapterId());
        session.setGoalText(request.goalText());
        session.setCurrentStage(Stage.STRUCTURE);
        session.setCurrentNodeId(firstNode.getId());

        try {
            LearningSession saved = sessionRepository.save(session);
            return new CreateSessionResponse(saved.getId());
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Session already exists for user and chapter.");
        }
    }
}


```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\service\EvaluatorService.java
```java
package com.pandanav.learning.application.service;

import com.pandanav.learning.api.dto.task.FeedbackResponse;
import com.pandanav.learning.domain.enums.ErrorTag;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class EvaluatorService {

    private static final int MIN_REASONING_LENGTH = 40;

    public EvaluationResult evaluate(String conceptName, String objective, String userAnswer) {
        String answer = normalize(userAnswer);
        String context = normalize(conceptName + " " + objective);

        List<String> keyTerms = buildKeyTerms(context);
        List<String> missingTerms = keyTerms.stream()
            .filter(term -> !contains(answer, term))
            .toList();

        int score = 100;
        Set<ErrorTag> errorTags = new LinkedHashSet<>();
        List<String> fixes = new ArrayList<>();

        if (!missingTerms.isEmpty()) {
            score -= Math.min(48, missingTerms.size() * 12);
            errorTags.add(ErrorTag.MISSING_STEPS);
            fixes.add("Add missing key points: " + String.join(", ", missingTerms) + ".");
        }

        if (answer.length() < MIN_REASONING_LENGTH) {
            score -= 15;
            errorTags.add(ErrorTag.SHALLOW_REASONING);
            fixes.add("Expand reasoning with cause-effect chain and concrete steps.");
        }

        if (containsConfusion(answer)) {
            score -= 20;
            errorTags.add(ErrorTag.CONCEPT_CONFUSION);
            fixes.add("Correct the misconception and explain why the correct mechanism is required.");
        }

        if (containsTerminologyIssue(answer)) {
            score -= 10;
            errorTags.add(ErrorTag.TERMINOLOGY);
            fixes.add("Use precise protocol terminology (SYN/ACK/sequence number semantics).");
        }

        if (keyTerms.stream().noneMatch(term -> contains(answer, term))) {
            score -= 12;
            errorTags.add(ErrorTag.MEMORY_GAP);
            fixes.add("Recall and restate the core concept definition before explaining.");
        }

        if (isTcpHandshakeContext(context)
            && !contains(answer, "old syn")
            && !contains(answer, "replay")
            && !contains(answer, "\u65e7 syn")
            && !contains(answer, "\u91cd\u653e")
            && !contains(answer, "half-open")
            && !contains(answer, "\u534a\u5f00")) {
            score -= 8;
            errorTags.add(ErrorTag.BOUNDARY_CASE);
            fixes.add("Include boundary case discussion: old SYN replay and half-open connection risk.");
        }

        score = clamp(score, 0, 100);

        String diagnosis;
        if (errorTags.isEmpty()) {
            diagnosis = "Answer is accurate and complete for current objective.";
        } else {
            diagnosis = "Answer has gaps in " + errorTags.stream().map(Enum::name).reduce((a, b) -> a + ", " + b).orElse("") + ".";
        }

        return new EvaluationResult(
            score,
            List.copyOf(errorTags),
            new FeedbackResponse(diagnosis, List.copyOf(fixes))
        );
    }

    private List<String> buildKeyTerms(String context) {
        if (isTcpHandshakeContext(context)) {
            return List.of("\u4e09\u6b21\u63e1\u624b", "syn", "ack", "\u5e8f\u5217\u53f7");
        }
        return List.of("definition", "mechanism", "steps");
    }

    private boolean isTcpHandshakeContext(String context) {
        return contains(context, "\u4e09\u6b21\u63e1\u624b")
            || contains(context, "tcp")
            || contains(context, "handshake");
    }

    private boolean containsConfusion(String answer) {
        return contains(answer, "\u4e24\u6b21\u63e1\u624b\u5c31\u591f")
            || contains(answer, "two-way handshake is enough")
            || contains(answer, "no need ack")
            || contains(answer, "\u4e0d\u9700\u8981 ack");
    }

    private boolean containsTerminologyIssue(String answer) {
        return contains(answer, "\u56db\u6b21\u63e1\u624b\u5efa\u7acb\u8fde\u63a5")
            || contains(answer, "ack means connect success");
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).trim();
    }

    private boolean contains(String text, String token) {
        return text.contains(token.toLowerCase(Locale.ROOT));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record EvaluationResult(
        Integer score,
        List<ErrorTag> errorTags,
        FeedbackResponse feedback
    ) {
    }
}

```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\service\GenerateTasksService.java
```java
package com.pandanav.learning.application.service;

import com.pandanav.learning.application.command.GenerateTasksCommand;
import com.pandanav.learning.application.usecase.GenerateTasksUseCase;
import com.pandanav.learning.domain.model.LearningTask;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GenerateTasksService implements GenerateTasksUseCase {

    @Override
    public List<LearningTask> execute(GenerateTasksCommand command) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\service\GetSessionOverviewService.java
```java
package com.pandanav.learning.application.service;

import com.pandanav.learning.api.dto.session.MasterySummaryResponse;
import com.pandanav.learning.api.dto.session.NextTaskResponse;
import com.pandanav.learning.api.dto.session.SessionOverviewResponse;
import com.pandanav.learning.api.dto.session.TimelineItemResponse;
import com.pandanav.learning.application.usecase.GetSessionOverviewUseCase;
import com.pandanav.learning.domain.model.Mastery;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.enums.TaskStatus;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import com.pandanav.learning.domain.repository.MasteryRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.domain.repository.TaskRepository;
import com.pandanav.learning.infrastructure.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class GetSessionOverviewService implements GetSessionOverviewUseCase {

    private final SessionRepository sessionRepository;
    private final TaskRepository taskRepository;
    private final ConceptNodeRepository conceptNodeRepository;
    private final MasteryRepository masteryRepository;

    public GetSessionOverviewService(
        SessionRepository sessionRepository,
        TaskRepository taskRepository,
        ConceptNodeRepository conceptNodeRepository,
        MasteryRepository masteryRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.taskRepository = taskRepository;
        this.conceptNodeRepository = conceptNodeRepository;
        this.masteryRepository = masteryRepository;
    }

    @Override
    public SessionOverviewResponse execute(Long sessionId) {
        var session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException("Session or task not found."));

        List<Task> timelineTasks = taskRepository.findBySessionIdWithStatus(sessionId);

        List<TimelineItemResponse> timeline = timelineTasks.stream()
            .map(task -> new TimelineItemResponse(
                task.getId(),
                task.getStage().name(),
                task.getNodeId(),
                task.getStatus().name()
            ))
            .toList();

        NextTaskResponse nextTask = timelineTasks.stream()
            .filter(task -> task.getStatus() != TaskStatus.SUCCEEDED)
            .findFirst()
            .map(task -> new NextTaskResponse(task.getId(), task.getStage().name(), task.getNodeId()))
            .orElse(null);

        List<MasterySummaryResponse> masterySummary = masteryRepository
            .findByUserIdAndChapterId(session.getUserId(), session.getChapterId())
            .stream()
            .map(mastery -> new MasterySummaryResponse(
                mastery.getNodeId(),
                mastery.getNodeName(),
                mastery.getMasteryValue() == null ? BigDecimal.ZERO : mastery.getMasteryValue()
            ))
            .toList();

        return new SessionOverviewResponse(
            session.getId(),
            session.getCourseId(),
            session.getChapterId(),
            session.getGoalText(),
            session.getCurrentNodeId(),
            session.getCurrentStage() == null ? null : session.getCurrentStage().name(),
            timeline,
            nextTask,
            masterySummary
        );
    }
}


```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\service\GetSessionService.java
```java
package com.pandanav.learning.application.service;

import com.pandanav.learning.application.query.GetSessionQuery;
import com.pandanav.learning.application.usecase.GetSessionUseCase;
import com.pandanav.learning.domain.model.LearningSession;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GetSessionService implements GetSessionUseCase {

    @Override
    public Optional<LearningSession> execute(GetSessionQuery query) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\service\MasteryUpdateService.java
```java
package com.pandanav.learning.application.service;

import com.pandanav.learning.domain.model.Mastery;
import com.pandanav.learning.domain.repository.MasteryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class MasteryUpdateService {

    private final MasteryRepository masteryRepository;

    public MasteryUpdateService(MasteryRepository masteryRepository) {
        this.masteryRepository = masteryRepository;
    }

    public MasteryUpdateResult update(String userId, Long nodeId, String nodeName, int score) {
        BigDecimal before = masteryRepository.findByUserIdAndNodeId(userId, nodeId)
            .map(Mastery::getMasteryValue)
            .orElse(BigDecimal.ZERO)
            .setScale(3, RoundingMode.HALF_UP);

        BigDecimal normalizedScore = BigDecimal.valueOf(score)
            .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        BigDecimal after = before.multiply(BigDecimal.valueOf(0.7))
            .add(normalizedScore.multiply(BigDecimal.valueOf(0.3)));
        after = clamp(after).setScale(3, RoundingMode.HALF_UP);

        Mastery mastery = new Mastery();
        mastery.setUserId(userId);
        mastery.setNodeId(nodeId);
        mastery.setNodeName(nodeName);
        mastery.setMasteryValue(after);
        masteryRepository.upsert(mastery);

        BigDecimal delta = after.subtract(before).setScale(3, RoundingMode.HALF_UP);
        return new MasteryUpdateResult(before, delta, after);
    }

    private BigDecimal clamp(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE;
        }
        return value;
    }

    public record MasteryUpdateResult(
        BigDecimal masteryBefore,
        BigDecimal masteryDelta,
        BigDecimal masteryAfter
    ) {
    }
}


```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\service\NextActionPolicyService.java
```java
package com.pandanav.learning.application.service;

import com.pandanav.learning.domain.enums.ErrorTag;
import com.pandanav.learning.domain.enums.NextAction;
import com.pandanav.learning.domain.policy.NextActionPolicy;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class NextActionPolicyService {

    private final NextActionPolicy nextActionPolicy;

    public NextActionPolicyService(NextActionPolicy nextActionPolicy) {
        this.nextActionPolicy = nextActionPolicy;
    }

    public NextAction decide(Integer score, List<ErrorTag> errorTags) {
        if (score == null) {
            return NextAction.NOOP;
        }
        Set<ErrorTag> tags = errorTags == null ? Set.of() : new HashSet<>(errorTags);
        return nextActionPolicy.decide(score, tags);
    }
}

```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\service\package-info.java
```java
package com.pandanav.learning.application.service;
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\service\PlanSessionTasksService.java
```java
package com.pandanav.learning.application.service;

import com.pandanav.learning.api.dto.session.PlanSessionResponse;
import com.pandanav.learning.api.dto.session.PlannedTaskResponse;
import com.pandanav.learning.application.usecase.PlanSessionTasksUseCase;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.enums.TaskStatus;
import com.pandanav.learning.domain.model.ConceptNode;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.policy.TaskObjectiveTemplateStrategy;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.domain.repository.TaskRepository;
import com.pandanav.learning.infrastructure.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PlanSessionTasksService implements PlanSessionTasksUseCase {

    private final SessionRepository sessionRepository;
    private final ConceptNodeRepository conceptNodeRepository;
    private final TaskRepository taskRepository;
    private final TaskObjectiveTemplateStrategy taskObjectiveTemplateStrategy;

    public PlanSessionTasksService(
        SessionRepository sessionRepository,
        ConceptNodeRepository conceptNodeRepository,
        TaskRepository taskRepository,
        TaskObjectiveTemplateStrategy taskObjectiveTemplateStrategy
    ) {
        this.sessionRepository = sessionRepository;
        this.conceptNodeRepository = conceptNodeRepository;
        this.taskRepository = taskRepository;
        this.taskObjectiveTemplateStrategy = taskObjectiveTemplateStrategy;
    }

    @Override
    public PlanSessionResponse execute(Long sessionId) {
        var session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException("Session or task not found."));

        List<ConceptNode> nodes = conceptNodeRepository.findByChapterIdOrderByOrderNoAsc(session.getChapterId());
        if (nodes.isEmpty()) {
            throw new NotFoundException("No concept nodes found for chapter.");
        }

        List<Task> tasksToSave = new ArrayList<>();
        for (ConceptNode node : nodes) {
            tasksToSave.add(buildTask(sessionId, node, Stage.STRUCTURE));
            tasksToSave.add(buildTask(sessionId, node, Stage.UNDERSTANDING));
            tasksToSave.add(buildTask(sessionId, node, Stage.TRAINING));
            tasksToSave.add(buildTask(sessionId, node, Stage.REFLECTION));
        }

        List<Task> saved = taskRepository.saveAll(tasksToSave);
        List<PlannedTaskResponse> responseTasks = saved.stream()
            .map(task -> new PlannedTaskResponse(
                task.getId(),
                task.getStage().name(),
                task.getNodeId(),
                task.getObjective(),
                TaskStatus.PENDING.name()
            ))
            .toList();

        return new PlanSessionResponse(sessionId, responseTasks);
    }

    private Task buildTask(Long sessionId, ConceptNode node, Stage stage) {
        Task task = new Task();
        task.setSessionId(sessionId);
        task.setNodeId(node.getId());
        task.setStage(stage);
        task.setStatus(TaskStatus.PENDING);
        task.setObjective(taskObjectiveTemplateStrategy.buildObjective(stage, node.getName()));
        return task;
    }
}

```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\service\StartSessionService.java
```java
package com.pandanav.learning.application.service;

import com.pandanav.learning.application.command.StartSessionCommand;
import com.pandanav.learning.application.usecase.StartSessionUseCase;
import com.pandanav.learning.domain.model.LearningSession;
import org.springframework.stereotype.Service;

@Service
public class StartSessionService implements StartSessionUseCase {

    @Override
    public LearningSession execute(StartSessionCommand command) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\service\SubmitTrainingAnswerService.java
```java
package com.pandanav.learning.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.api.dto.session.NextTaskResponse;
import com.pandanav.learning.api.dto.task.SubmitTaskRequest;
import com.pandanav.learning.api.dto.task.SubmitTaskResponse;
import com.pandanav.learning.application.service.EvaluatorService.EvaluationResult;
import com.pandanav.learning.application.service.MasteryUpdateService.MasteryUpdateResult;
import com.pandanav.learning.application.usecase.SubmitTrainingAnswerUseCase;
import com.pandanav.learning.domain.enums.NextAction;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.enums.TaskStatus;
import com.pandanav.learning.domain.model.ConceptNode;
import com.pandanav.learning.domain.model.Evidence;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import com.pandanav.learning.domain.repository.EvidenceRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.domain.repository.TaskRepository;
import com.pandanav.learning.infrastructure.exception.ConflictException;
import com.pandanav.learning.infrastructure.exception.InternalServerException;
import com.pandanav.learning.infrastructure.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SubmitTrainingAnswerService implements SubmitTrainingAnswerUseCase {

    private static final String NOT_FOUND_MESSAGE = "Session or task not found.";
    private static final String CONFLICT_MESSAGE = "Task is not in a submittable state.";

    private final TaskRepository taskRepository;
    private final SessionRepository sessionRepository;
    private final ConceptNodeRepository conceptNodeRepository;
    private final EvidenceRepository evidenceRepository;
    private final EvaluatorService evaluatorService;
    private final MasteryUpdateService masteryUpdateService;
    private final NextActionPolicyService nextActionPolicyService;
    private final ObjectMapper objectMapper;

    public SubmitTrainingAnswerService(
        TaskRepository taskRepository,
        SessionRepository sessionRepository,
        ConceptNodeRepository conceptNodeRepository,
        EvidenceRepository evidenceRepository,
        EvaluatorService evaluatorService,
        MasteryUpdateService masteryUpdateService,
        NextActionPolicyService nextActionPolicyService,
        ObjectMapper objectMapper
    ) {
        this.taskRepository = taskRepository;
        this.sessionRepository = sessionRepository;
        this.conceptNodeRepository = conceptNodeRepository;
        this.evidenceRepository = evidenceRepository;
        this.evaluatorService = evaluatorService;
        this.masteryUpdateService = masteryUpdateService;
        this.nextActionPolicyService = nextActionPolicyService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public SubmitTaskResponse submit(Long taskId, SubmitTaskRequest request) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));

        if (!task.canSubmit()) {
            throw new ConflictException(CONFLICT_MESSAGE);
        }

        LearningSession session = sessionRepository.findById(task.getSessionId())
            .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));

        ConceptNode node = conceptNodeRepository.findById(task.getNodeId())
            .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));

        EvaluationResult evaluation = evaluatorService.evaluate(node.getName(), task.getObjective(), request.userAnswer());
        MasteryUpdateResult mastery = masteryUpdateService.update(session.getUserId(), task.getNodeId(), node.getName(), evaluation.score());

        persistEvidence(session, task, evaluation);

        NextAction action = nextActionPolicyService.decide(evaluation.score(), evaluation.errorTags());
        Task nextTask = null;

        if (action == NextAction.ADVANCE_TO_NEXT_NODE) {
            Task advanced = advanceToNextNodeIfPossible(session, task.getNodeId());
            if (advanced == null) {
                action = NextAction.NOOP;
            } else {
                nextTask = advanced;
            }
        } else if (action != NextAction.NOOP) {
            nextTask = createAdaptiveTask(task.getSessionId(), task.getNodeId(), node.getName(), action);
        }

        return new SubmitTaskResponse(
            task.getId(),
            task.getStage().name(),
            task.getNodeId(),
            evaluation.score(),
            evaluation.errorTags().stream().map(Enum::name).toList(),
            evaluation.feedback(),
            mastery.masteryBefore(),
            mastery.masteryDelta(),
            mastery.masteryAfter(),
            action.name(),
            nextTask == null ? null : new NextTaskResponse(nextTask.getId(), nextTask.getStage().name(), nextTask.getNodeId())
        );
    }

    private void persistEvidence(LearningSession session, Task task, EvaluationResult evaluation) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("session_id", session.getId());
        payload.put("task_id", task.getId());
        payload.put("node_id", task.getNodeId());
        payload.put("score", evaluation.score());
        payload.put("error_tags", evaluation.errorTags().stream().map(Enum::name).toList());
        payload.put("feedback_json", Map.of(
            "diagnosis", evaluation.feedback().diagnosis(),
            "fixes", evaluation.feedback().fixes()
        ));

        Evidence evidence = new Evidence();
        evidence.setTaskId(task.getId());
        evidence.setEvidenceType("TRAINING_SUBMISSION_EVAL");
        evidence.setContentJson(toJson(payload));
        evidenceRepository.save(evidence);
    }

    private Task createAdaptiveTask(Long sessionId, Long nodeId, String conceptName, NextAction action) {
        Stage stage = mapStage(action);
        Task task = new Task();
        task.setSessionId(sessionId);
        task.setNodeId(nodeId);
        task.setStage(stage);
        task.setStatus(TaskStatus.PENDING);
        task.setObjective(buildObjective(action, conceptName));
        return taskRepository.save(task);
    }

    private Task advanceToNextNodeIfPossible(LearningSession session, Long currentNodeId) {
        List<ConceptNode> nodes = conceptNodeRepository.findByChapterIdOrderByOrderNoAsc(session.getChapterId());
        if (nodes.isEmpty()) {
            return null;
        }

        int currentIndex = -1;
        Long anchorNodeId = session.getCurrentNodeId() != null ? session.getCurrentNodeId() : currentNodeId;
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getId().equals(anchorNodeId)) {
                currentIndex = i;
                break;
            }
        }
        if (currentIndex < 0 || currentIndex + 1 >= nodes.size()) {
            return null;
        }

        ConceptNode nextNode = nodes.get(currentIndex + 1);
        sessionRepository.updateCurrentPosition(session.getId(), nextNode.getId(), Stage.STRUCTURE);

        return taskRepository.findFirstBySessionIdAndNodeIdAndStage(session.getId(), nextNode.getId(), Stage.STRUCTURE)
            .orElseGet(() -> {
                Task firstTask = new Task();
                firstTask.setSessionId(session.getId());
                firstTask.setNodeId(nextNode.getId());
                firstTask.setStage(Stage.STRUCTURE);
                firstTask.setStatus(TaskStatus.PENDING);
                firstTask.setObjective("Build structure map for concept: " + nextNode.getName());
                return taskRepository.save(firstTask);
            });
    }

    private Stage mapStage(NextAction action) {
        return switch (action) {
            case INSERT_REMEDIAL_UNDERSTANDING -> Stage.UNDERSTANDING;
            case INSERT_TRAINING_VARIANTS, INSERT_TRAINING_REINFORCEMENT -> Stage.TRAINING;
            default -> throw new IllegalArgumentException("Unsupported action for task creation: " + action);
        };
    }

    private String buildObjective(NextAction action, String conceptName) {
        return switch (action) {
            case INSERT_REMEDIAL_UNDERSTANDING ->
                "Remedial understanding for " + conceptName + ": explain mechanism and correct misconceptions.";
            case INSERT_TRAINING_VARIANTS ->
                "Variant training for " + conceptName + ": solve 3 new scenario-based questions.";
            case INSERT_TRAINING_REINFORCEMENT ->
                "Reinforcement training for " + conceptName + ": complete one advanced mixed-case drill.";
            default -> "";
        };
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new InternalServerException("Failed to serialize evidence payload.");
        }
    }
}

```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\service\TaskApplicationService.java
```java
package com.pandanav.learning.application.service;

import com.pandanav.learning.api.dto.task.SubmitTaskRequest;
import com.pandanav.learning.api.dto.task.SubmitTaskResponse;
import com.pandanav.learning.application.usecase.SubmitTrainingAnswerUseCase;
import org.springframework.stereotype.Service;

@Service
public class TaskApplicationService {

    private final SubmitTrainingAnswerUseCase submitTrainingAnswerUseCase;

    public TaskApplicationService(SubmitTrainingAnswerUseCase submitTrainingAnswerUseCase) {
        this.submitTrainingAnswerUseCase = submitTrainingAnswerUseCase;
    }

    public SubmitTaskResponse submitTask(Long taskId, SubmitTaskRequest request) {
        return submitTrainingAnswerUseCase.submit(taskId, request);
    }
}


```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\application\service\TaskRunnerService.java
```java
package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.api.dto.task.RunTaskResponse;
import com.pandanav.learning.application.usecase.RunTaskUseCase;
import com.pandanav.learning.domain.enums.TaskStatus;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.repository.TaskRepository;
import com.pandanav.learning.infrastructure.exception.ConflictException;
import com.pandanav.learning.infrastructure.exception.InternalServerException;
import com.pandanav.learning.infrastructure.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class TaskRunnerService implements RunTaskUseCase {

    private final TaskRepository taskRepository;
    private final ObjectMapper objectMapper;

    public TaskRunnerService(TaskRepository taskRepository, ObjectMapper objectMapper) {
        this.taskRepository = taskRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public RunTaskResponse run(Long taskId) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new NotFoundException("Session or task not found."));

        if (task.getStatus() == TaskStatus.SUCCEEDED && hasOutput(task.getOutputJson())) {
            return toResponse(task, parseOutput(task.getOutputJson()));
        }

        if (task.getStatus() == TaskStatus.RUNNING) {
            throw new ConflictException("Task is currently running.");
        }

        if (!task.canRun()) {
            throw new ConflictException("Task is not in a runnable state.");
        }

        Long attemptId = taskRepository.createRunningAttempt(taskId);
        task.markRunning();

        JsonNode generated = generateByStage(task);
        String outputJson = writeJson(generated);

        taskRepository.markAttemptSucceeded(attemptId, outputJson);
        task.markSucceeded(outputJson);

        return toResponse(task, generated);
    }

    private boolean hasOutput(String outputJson) {
        return outputJson != null && !outputJson.isBlank() && !"null".equalsIgnoreCase(outputJson.trim());
    }

    private JsonNode parseOutput(String outputJson) {
        try {
            return objectMapper.readTree(outputJson);
        } catch (Exception ex) {
            throw new InternalServerException("Stored output_json is invalid.");
        }
    }

    private String writeJson(JsonNode output) {
        try {
            return objectMapper.writeValueAsString(output);
        } catch (Exception ex) {
            throw new InternalServerException("Failed to serialize task output.");
        }
    }

    private RunTaskResponse toResponse(Task task, JsonNode output) {
        return new RunTaskResponse(
            task.getId(),
            task.getStage().name(),
            task.getNodeId(),
            TaskStatus.SUCCEEDED.name(),
            output
        );
    }

    private JsonNode generateByStage(Task task) {
        return switch (task.getStage()) {
            case STRUCTURE -> objectMapper.valueToTree(Map.of(
                "sections", List.of(
                    Map.of("type", "concepts", "title", "core concepts", "bullets", List.of("definition", "terms", "boundaries")),
                    Map.of("type", "structure", "title", "structure", "items", List.of("part A", "part B", "io relation")),
                    Map.of("type", "relations", "title", "relations", "items", List.of("prerequisite relation", "next relation")),
                    Map.of("type", "summary", "title", "summary", "text", "Build structure first, then mechanism understanding.")
                )
            ));
            case UNDERSTANDING -> objectMapper.valueToTree(Map.of(
                "sections", List.of(
                    Map.of("type", "concepts", "title", "concepts", "bullets", List.of("key definition", "required condition", "trigger condition")),
                    Map.of("type", "mechanism", "title", "mechanism", "steps", List.of("step 1 input", "step 2 state transition", "step 3 output")),
                    Map.of("type", "misconceptions", "title", "misconceptions", "items", List.of("misconception 1", "misconception 2")),
                    Map.of("type", "summary", "title", "summary", "text", "Understanding causal links matters more than memorizing steps.")
                )
            ));
            case TRAINING -> objectMapper.valueToTree(Map.of(
                "questions", List.of(
                    Map.of("id", "q1", "type", "short_answer", "prompt", "Explain the core principle."),
                    Map.of("id", "q2", "type", "scenario", "prompt", "Infer state transition from the scenario.")
                ),
                "variants", List.of(
                    Map.of("name", "basic", "focus", "concept accuracy"),
                    Map.of("name", "advanced", "focus", "boundary cases")
                ),
                "rubric", Map.of(
                    "full_score", 100,
                    "dimensions", List.of(
                        Map.of("name", "concept", "weight", 40),
                        Map.of("name", "reasoning", "weight", 40),
                        Map.of("name", "clarity", "weight", 20)
                    )
                )
            ));
            case REFLECTION -> objectMapper.valueToTree(Map.of(
                "diagnosis", "Main path completed, boundary-case mastery still needs reinforcement.",
                "reflection_points", List.of("can restate concept", "can explain causal chain", "can identify misconceptions"),
                "next_steps", List.of("review wrong questions", "finish one variant set", "3-minute oral recap")
            ));
        };
    }
}

```

## 4) domain 层代码（完整）
### 文件路径
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\enums\ErrorTag.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\enums\NextAction.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\enums\Stage.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\enums\TaskStatus.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\model\ConceptNode.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\model\Evidence.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\model\LearningSession.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\model\LearningTask.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\model\Mastery.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\model\package-info.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\model\Stage.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\model\Task.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\model\TaskStatus.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\policy\EvaluationRule.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\policy\NextActionPolicy.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\policy\TaskObjectiveTemplateStrategy.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\repository\ConceptNodeRepository.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\repository\EvidenceRepository.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\repository\LearningSessionRepository.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\repository\MasteryRepository.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\repository\package-info.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\repository\SessionRepository.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\repository\TaskRepository.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\service\DefaultTaskObjectiveTemplateStrategy.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\service\ScoreBasedNextActionPolicy.java

### domain 规则与状态机约束摘要
- 统一枚举已在 `domain/enums`：Stage/TaskStatus/ErrorTag/NextAction。
- `Task` 状态行为：`canRun()`、`canSubmit()`、`markRunning()`、`markSucceeded()`、`markFailed()`。
- 策略接口：`TaskObjectiveTemplateStrategy`、`EvaluationRule`、`NextActionPolicy`。
- 兼容状态：DB中 `CANCELLED` 通过 `TaskStatus.fromDb()` 映射为 `FAILED`。
- 注意：`domain/model/Stage.java` 与 `domain/model/TaskStatus.java` 当前为“迁移占位文件”，实际使用 `domain/enums`。

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\enums\ErrorTag.java
```java
package com.pandanav.learning.domain.enums;

public enum ErrorTag {
    CONCEPT_CONFUSION,
    MISSING_STEPS,
    BOUNDARY_CASE,
    TERMINOLOGY,
    SHALLOW_REASONING,
    MEMORY_GAP
}


```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\enums\NextAction.java
```java
package com.pandanav.learning.domain.enums;

public enum NextAction {
    INSERT_REMEDIAL_UNDERSTANDING,
    INSERT_TRAINING_VARIANTS,
    INSERT_TRAINING_REINFORCEMENT,
    ADVANCE_TO_NEXT_NODE,
    NOOP
}


```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\enums\Stage.java
```java
package com.pandanav.learning.domain.enums;

public enum Stage {
    STRUCTURE,
    UNDERSTANDING,
    TRAINING,
    REFLECTION
}


```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\enums\TaskStatus.java
```java
package com.pandanav.learning.domain.enums;

public enum TaskStatus {
    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED;

    public static TaskStatus fromDb(String value) {
        if (value == null || value.isBlank()) {
            return PENDING;
        }
        if ("CANCELLED".equalsIgnoreCase(value)) {
            return FAILED;
        }
        return TaskStatus.valueOf(value.toUpperCase());
    }
}


```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\model\ConceptNode.java
```java
package com.pandanav.learning.domain.model;

import java.time.OffsetDateTime;

public class ConceptNode {

    private Long id;
    private String chapterId;
    private String name;
    private String outline;
    private Integer orderNo;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChapterId() {
        return chapterId;
    }

    public void setChapterId(String chapterId) {
        this.chapterId = chapterId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOutline() {
        return outline;
    }

    public void setOutline(String outline) {
        this.outline = outline;
    }

    public Integer getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(Integer orderNo) {
        this.orderNo = orderNo;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\model\Evidence.java
```java
package com.pandanav.learning.domain.model;

import java.time.OffsetDateTime;

public class Evidence {

    private Long id;
    private Long taskId;
    private String evidenceType;
    private String contentJson;
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getEvidenceType() {
        return evidenceType;
    }

    public void setEvidenceType(String evidenceType) {
        this.evidenceType = evidenceType;
    }

    public String getContentJson() {
        return contentJson;
    }

    public void setContentJson(String contentJson) {
        this.contentJson = contentJson;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\model\LearningSession.java
```java
package com.pandanav.learning.domain.model;

import com.pandanav.learning.domain.enums.Stage;

import java.time.OffsetDateTime;

public class LearningSession {

    private Long id;
    private String userId;
    private String courseId;
    private String chapterId;
    private String goalText;
    private Long currentNodeId;
    private Stage currentStage;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getChapterId() {
        return chapterId;
    }

    public void setChapterId(String chapterId) {
        this.chapterId = chapterId;
    }

    public String getGoalText() {
        return goalText;
    }

    public void setGoalText(String goalText) {
        this.goalText = goalText;
    }

    public Long getCurrentNodeId() {
        return currentNodeId;
    }

    public void setCurrentNodeId(Long currentNodeId) {
        this.currentNodeId = currentNodeId;
    }

    public Stage getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(Stage currentStage) {
        this.currentStage = currentStage;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\model\LearningTask.java
```java
package com.pandanav.learning.domain.model;

import com.pandanav.learning.domain.enums.Stage;

import java.time.OffsetDateTime;

public class LearningTask {

    private Long id;
    private Long sessionId;
    private Stage stage;
    private Long nodeId;
    private String objective;
    private String inputJson;
    private String expectedOutputSchema;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public String getInputJson() {
        return inputJson;
    }

    public void setInputJson(String inputJson) {
        this.inputJson = inputJson;
    }

    public String getExpectedOutputSchema() {
        return expectedOutputSchema;
    }

    public void setExpectedOutputSchema(String expectedOutputSchema) {
        this.expectedOutputSchema = expectedOutputSchema;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\model\Mastery.java
```java
package com.pandanav.learning.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class Mastery {

    private String userId;
    private Long nodeId;
    private String nodeName;
    private BigDecimal masteryValue;
    private OffsetDateTime updatedAt;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public BigDecimal getMasteryValue() {
        return masteryValue;
    }

    public void setMasteryValue(BigDecimal masteryValue) {
        this.masteryValue = masteryValue;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\model\package-info.java
```java
package com.pandanav.learning.domain.model;
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\model\Stage.java
```java
package com.pandanav.learning.domain.model;

// Moved to com.pandanav.learning.domain.enums.Stage

```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\model\Task.java
```java
package com.pandanav.learning.domain.model;

import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.enums.TaskStatus;

import java.time.OffsetDateTime;

public class Task {

    private Long id;
    private Long sessionId;
    private Stage stage;
    private Long nodeId;
    private String objective;
    private TaskStatus status;
    private String outputJson;
    private String failureReason;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public String getOutputJson() {
        return outputJson;
    }

    public void setOutputJson(String outputJson) {
        this.outputJson = outputJson;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public boolean canRun() {
        return status == TaskStatus.PENDING || status == TaskStatus.FAILED;
    }

    public boolean canSubmit() {
        return stage == Stage.TRAINING && (status == TaskStatus.SUCCEEDED || status == TaskStatus.PENDING);
    }

    public void markRunning() {
        if (status != TaskStatus.PENDING && status != TaskStatus.FAILED) {
            throw new IllegalStateException("Task is not in a runnable state.");
        }
        this.status = TaskStatus.RUNNING;
    }

    public void markSucceeded(String outputJson) {
        this.status = TaskStatus.SUCCEEDED;
        this.outputJson = outputJson;
        this.failureReason = null;
    }

    public void markFailed(String reason) {
        this.status = TaskStatus.FAILED;
        this.failureReason = reason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}


```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\model\TaskStatus.java
```java
package com.pandanav.learning.domain.model;

// Moved to com.pandanav.learning.domain.enums.TaskStatus

```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\policy\EvaluationRule.java
```java
package com.pandanav.learning.domain.policy;

import com.pandanav.learning.domain.enums.ErrorTag;

public interface EvaluationRule {

    void apply(EvaluationContext context, EvaluationDraft draft);

    record EvaluationContext(String conceptName, String objective, String userAnswer) {
    }

    interface EvaluationDraft {

        int score();

        void addScore(int delta);

        void addDiagnosis(String message);

        void addFix(String fix);

        void addErrorTag(ErrorTag errorTag);
    }
}

```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\policy\NextActionPolicy.java
```java
package com.pandanav.learning.domain.policy;

import com.pandanav.learning.domain.enums.NextAction;
import com.pandanav.learning.domain.enums.ErrorTag;

import java.util.Set;

public interface NextActionPolicy {

    NextAction decide(int score, Set<ErrorTag> errorTags);
}


```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\policy\TaskObjectiveTemplateStrategy.java
```java
package com.pandanav.learning.domain.policy;

import com.pandanav.learning.domain.enums.Stage;

public interface TaskObjectiveTemplateStrategy {

    String buildObjective(Stage stage, String conceptName);
}


```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\repository\ConceptNodeRepository.java
```java
package com.pandanav.learning.domain.repository;

import com.pandanav.learning.domain.model.ConceptNode;

import java.util.List;
import java.util.Optional;

public interface ConceptNodeRepository {

    Optional<ConceptNode> findById(Long id);

    Optional<ConceptNode> findFirstByChapterIdOrderByOrderNoAsc(String chapterId);

    List<ConceptNode> findByChapterIdOrderByOrderNoAsc(String chapterId);
}
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\repository\EvidenceRepository.java
```java
package com.pandanav.learning.domain.repository;

import com.pandanav.learning.domain.model.Evidence;

import java.util.List;
import java.util.Optional;

public interface EvidenceRepository {

    Evidence save(Evidence evidence);

    Optional<Evidence> findById(Long id);

    List<Evidence> findByTaskId(Long taskId);
}
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\repository\LearningSessionRepository.java
```java
package com.pandanav.learning.domain.repository;

import com.pandanav.learning.domain.model.LearningSession;

import java.util.Optional;

public interface LearningSessionRepository {

    LearningSession save(LearningSession session);

    Optional<LearningSession> findById(Long id);

    Optional<LearningSession> findByUserIdAndChapterId(String userId, String chapterId);
}
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\repository\MasteryRepository.java
```java
package com.pandanav.learning.domain.repository;

import com.pandanav.learning.domain.model.Mastery;

import java.util.List;
import java.util.Optional;

public interface MasteryRepository {

    List<Mastery> findByUserIdAndChapterId(String userId, String chapterId);

    Optional<Mastery> findByUserIdAndNodeId(String userId, Long nodeId);

    Mastery upsert(Mastery mastery);
}


```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\repository\package-info.java
```java
package com.pandanav.learning.domain.repository;
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\repository\SessionRepository.java
```java
package com.pandanav.learning.domain.repository;

import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.enums.Stage;

import java.util.Optional;

public interface SessionRepository {

    LearningSession save(LearningSession session);

    Optional<LearningSession> findById(Long id);

    void updateCurrentPosition(Long sessionId, Long currentNodeId, Stage currentStage);
}


```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\repository\TaskRepository.java
```java
package com.pandanav.learning.domain.repository;

import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.model.Task;

import java.util.List;
import java.util.Optional;

public interface TaskRepository {

    Task save(Task task);

    List<Task> saveAll(List<Task> tasks);

    Optional<Task> findById(Long id);

    List<Task> findBySessionIdWithStatus(Long sessionId);

    Optional<Task> findFirstBySessionIdAndNodeIdAndStage(Long sessionId, Long nodeId, Stage stage);

    Long createRunningAttempt(Long taskId);

    void markAttemptSucceeded(Long attemptId, String outputJson);
}


```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\service\DefaultTaskObjectiveTemplateStrategy.java
```java
package com.pandanav.learning.domain.service;

import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.policy.TaskObjectiveTemplateStrategy;

public class DefaultTaskObjectiveTemplateStrategy implements TaskObjectiveTemplateStrategy {

    @Override
    public String buildObjective(Stage stage, String conceptName) {
        return switch (stage) {
            case STRUCTURE -> "Build a structure map for concept: " + conceptName;
            case UNDERSTANDING -> "Explain mechanism and misconceptions for concept: " + conceptName;
            case TRAINING -> "Complete adaptive training for concept: " + conceptName;
            case REFLECTION -> "Reflect on errors and next improvements for concept: " + conceptName;
        };
    }
}


```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\domain\service\ScoreBasedNextActionPolicy.java
```java
package com.pandanav.learning.domain.service;

import com.pandanav.learning.domain.enums.ErrorTag;
import com.pandanav.learning.domain.enums.NextAction;
import com.pandanav.learning.domain.policy.NextActionPolicy;

import java.util.Set;

public class ScoreBasedNextActionPolicy implements NextActionPolicy {

    @Override
    public NextAction decide(int score, Set<ErrorTag> errorTags) {
        if (score < 60) {
            return NextAction.INSERT_REMEDIAL_UNDERSTANDING;
        }
        if (score < 80) {
            return NextAction.INSERT_TRAINING_VARIANTS;
        }
        if (score < 90) {
            return NextAction.INSERT_TRAINING_REINFORCEMENT;
        }
        if (score <= 100) {
            return NextAction.ADVANCE_TO_NEXT_NODE;
        }
        return NextAction.NOOP;
    }
}


```

## 5) infrastructure/persistence 代码（完整）
### 文件路径
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\infrastructure\persistence\JdbcConceptNodeRepository.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\infrastructure\persistence\JdbcEvidenceRepository.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\infrastructure\persistence\JdbcLearningSessionRepository.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\infrastructure\persistence\JdbcMasteryRepository.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\infrastructure\persistence\JdbcTaskRepository.java
- D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\infrastructure\persistence\package-info.java

### repository -> 表映射 & 关键 SQL 行为
- JdbcLearningSessionRepository -> `learning_session`：insert/find/updateCurrentPosition
- JdbcConceptNodeRepository -> `concept_node`：findById/findFirst/findByChapter
- JdbcTaskRepository -> `task` + `task_attempt`：save/find/findBySession/createRunningAttempt/markAttemptSucceeded
- JdbcMasteryRepository -> `mastery` + `concept_node`：findByChapter/findByNode/upsert
- JdbcEvidenceRepository -> `evidence`：save/findById/findByTaskId

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\infrastructure\persistence\JdbcConceptNodeRepository.java
```java
package com.pandanav.learning.infrastructure.persistence;

import com.pandanav.learning.domain.model.ConceptNode;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JdbcConceptNodeRepository implements ConceptNodeRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcConceptNodeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<ConceptNode> findById(Long id) {
        try {
            ConceptNode node = jdbcTemplate.queryForObject(
                """
                    SELECT id, chapter_id, name, outline, order_no, created_at, updated_at
                    FROM concept_node
                    WHERE id = ?
                    """,
                (rs, rowNum) -> mapNode(rs),
                id
            );
            return Optional.ofNullable(node);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ConceptNode> findFirstByChapterIdOrderByOrderNoAsc(String chapterId) {
        List<ConceptNode> nodes = jdbcTemplate.query(
            """
                SELECT id, chapter_id, name, outline, order_no, created_at, updated_at
                FROM concept_node
                WHERE chapter_id = ?
                ORDER BY order_no ASC, id ASC
                LIMIT 1
                """,
            (rs, rowNum) -> mapNode(rs),
            chapterId
        );
        return nodes.stream().findFirst();
    }

    @Override
    public List<ConceptNode> findByChapterIdOrderByOrderNoAsc(String chapterId) {
        return jdbcTemplate.query(
            """
                SELECT id, chapter_id, name, outline, order_no, created_at, updated_at
                FROM concept_node
                WHERE chapter_id = ?
                ORDER BY order_no ASC, id ASC
                """,
            (rs, rowNum) -> mapNode(rs),
            chapterId
        );
    }

    private ConceptNode mapNode(java.sql.ResultSet rs) throws java.sql.SQLException {
        ConceptNode node = new ConceptNode();
        node.setId(rs.getLong("id"));
        node.setChapterId(rs.getString("chapter_id"));
        node.setName(rs.getString("name"));
        node.setOutline(rs.getString("outline"));
        node.setOrderNo(rs.getInt("order_no"));
        node.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        node.setUpdatedAt(rs.getObject("updated_at", java.time.OffsetDateTime.class));
        return node;
    }
}
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\infrastructure\persistence\JdbcEvidenceRepository.java
```java
package com.pandanav.learning.infrastructure.persistence;

import com.pandanav.learning.domain.model.Evidence;
import com.pandanav.learning.domain.repository.EvidenceRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcEvidenceRepository implements EvidenceRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcEvidenceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Evidence save(Evidence evidence) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                """
                    INSERT INTO evidence (task_id, evidence_type, content_json)
                    VALUES (?, ?, CAST(? AS jsonb))
                    """,
                new String[]{"id"}
            );
            ps.setLong(1, evidence.getTaskId());
            ps.setString(2, evidence.getEvidenceType());
            ps.setString(3, evidence.getContentJson());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            evidence.setId(key.longValue());
        }
        return evidence;
    }

    @Override
    public Optional<Evidence> findById(Long id) {
        try {
            Evidence evidence = jdbcTemplate.queryForObject(
                """
                    SELECT id, task_id, evidence_type, content_json, created_at
                    FROM evidence
                    WHERE id = ?
                    """,
                (rs, rowNum) -> mapEvidence(rs),
                id
            );
            return Optional.ofNullable(evidence);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public List<Evidence> findByTaskId(Long taskId) {
        return jdbcTemplate.query(
            """
                SELECT id, task_id, evidence_type, content_json, created_at
                FROM evidence
                WHERE task_id = ?
                ORDER BY created_at DESC, id DESC
                """,
            (rs, rowNum) -> mapEvidence(rs),
            taskId
        );
    }

    private Evidence mapEvidence(java.sql.ResultSet rs) throws java.sql.SQLException {
        Evidence evidence = new Evidence();
        evidence.setId(rs.getLong("id"));
        evidence.setTaskId(rs.getLong("task_id"));
        evidence.setEvidenceType(rs.getString("evidence_type"));
        evidence.setContentJson(rs.getString("content_json"));
        evidence.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        return evidence;
    }
}


```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\infrastructure\persistence\JdbcLearningSessionRepository.java
```java
package com.pandanav.learning.infrastructure.persistence;

import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.repository.SessionRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.Optional;

@Repository
public class JdbcLearningSessionRepository implements SessionRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcLearningSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public LearningSession save(LearningSession session) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                """
                    INSERT INTO learning_session (user_id, course_id, chapter_id, goal_text, current_node_id, current_stage)
                    VALUES (?, ?, ?, ?, ?, ?::task_stage)
                    """,
                new String[]{"id"}
            );
            ps.setString(1, session.getUserId());
            ps.setString(2, session.getCourseId());
            ps.setString(3, session.getChapterId());
            ps.setString(4, session.getGoalText());
            ps.setLong(5, session.getCurrentNodeId());
            ps.setString(6, session.getCurrentStage().name());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key != null) {
            session.setId(key.longValue());
        }
        return session;
    }

    @Override
    public Optional<LearningSession> findById(Long id) {
        try {
            LearningSession session = jdbcTemplate.queryForObject(
                """
                    SELECT id, user_id, course_id, chapter_id, goal_text, current_node_id, current_stage, created_at, updated_at
                    FROM learning_session
                    WHERE id = ?
                    """,
                (rs, rowNum) -> {
                    LearningSession item = new LearningSession();
                    item.setId(rs.getLong("id"));
                    item.setUserId(rs.getString("user_id"));
                    item.setCourseId(rs.getString("course_id"));
                    item.setChapterId(rs.getString("chapter_id"));
                    item.setGoalText(rs.getString("goal_text"));
                    item.setCurrentNodeId(rs.getLong("current_node_id"));
                    String stage = rs.getString("current_stage");
                    item.setCurrentStage(stage == null ? null : Stage.valueOf(stage));
                    item.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
                    item.setUpdatedAt(rs.getObject("updated_at", java.time.OffsetDateTime.class));
                    return item;
                },
                id
            );
            return Optional.ofNullable(session);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public void updateCurrentPosition(Long sessionId, Long currentNodeId, Stage currentStage) {
        jdbcTemplate.update(
            """
                UPDATE learning_session
                SET current_node_id = ?,
                    current_stage = ?::task_stage,
                    updated_at = now()
                WHERE id = ?
                """,
            currentNodeId,
            currentStage.name(),
            sessionId
        );
    }
}


```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\infrastructure\persistence\JdbcMasteryRepository.java
```java
package com.pandanav.learning.infrastructure.persistence;

import com.pandanav.learning.domain.model.Mastery;
import com.pandanav.learning.domain.repository.MasteryRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JdbcMasteryRepository implements MasteryRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcMasteryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Mastery> findByUserIdAndChapterId(String userId, String chapterId) {
        return jdbcTemplate.query(
            """
                SELECT cn.id AS node_id,
                       cn.name AS node_name,
                       m.user_id,
                       COALESCE(m.mastery_value, 0.000) AS mastery_value,
                       m.updated_at
                FROM concept_node cn
                LEFT JOIN mastery m
                  ON m.node_id = cn.id
                 AND m.user_id = ?
                WHERE cn.chapter_id = ?
                ORDER BY cn.order_no ASC, cn.id ASC
                """,
            (rs, rowNum) -> mapMastery(rs),
            userId,
            chapterId
        );
    }

    @Override
    public Optional<Mastery> findByUserIdAndNodeId(String userId, Long nodeId) {
        try {
            Mastery mastery = jdbcTemplate.queryForObject(
                """
                    SELECT m.user_id, m.node_id, cn.name AS node_name, m.mastery_value, m.updated_at
                    FROM mastery m
                    JOIN concept_node cn ON cn.id = m.node_id
                    WHERE m.user_id = ? AND m.node_id = ?
                    """,
                (rs, rowNum) -> mapMastery(rs),
                userId,
                nodeId
            );
            return Optional.ofNullable(mastery);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public Mastery upsert(Mastery mastery) {
        jdbcTemplate.update(
            """
                INSERT INTO mastery (user_id, node_id, mastery_value, updated_at)
                VALUES (?, ?, ?, now())
                ON CONFLICT (user_id, node_id)
                DO UPDATE SET mastery_value = EXCLUDED.mastery_value,
                              updated_at = now()
                """,
            mastery.getUserId(),
            mastery.getNodeId(),
            mastery.getMasteryValue()
        );
        return findByUserIdAndNodeId(mastery.getUserId(), mastery.getNodeId())
            .orElse(mastery);
    }

    private Mastery mapMastery(java.sql.ResultSet rs) throws java.sql.SQLException {
        Mastery mastery = new Mastery();
        mastery.setUserId(rs.getString("user_id"));
        mastery.setNodeId(rs.getLong("node_id"));
        mastery.setNodeName(rs.getString("node_name"));
        mastery.setMasteryValue(rs.getBigDecimal("mastery_value"));
        mastery.setUpdatedAt(rs.getObject("updated_at", java.time.OffsetDateTime.class));
        return mastery;
    }
}


```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\infrastructure\persistence\JdbcTaskRepository.java
```java
package com.pandanav.learning.infrastructure.persistence;

import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.enums.TaskStatus;
import com.pandanav.learning.domain.repository.TaskRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcTaskRepository implements TaskRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcTaskRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Task save(Task task) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                """
                    INSERT INTO task (session_id, stage, node_id, objective)
                    VALUES (?, ?::task_stage, ?, ?)
                    """,
                new String[]{"id"}
            );
            ps.setLong(1, task.getSessionId());
            ps.setString(2, task.getStage().name());
            ps.setLong(3, task.getNodeId());
            ps.setString(4, task.getObjective());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key != null) {
            task.setId(key.longValue());
        }
        if (task.getStatus() == null) {
            task.setStatus(TaskStatus.PENDING);
        }
        return task;
    }

    @Override
    public List<Task> saveAll(List<Task> tasks) {
        return tasks.stream().map(this::save).toList();
    }

    @Override
    public Optional<Task> findById(Long id) {
        try {
            Task task = jdbcTemplate.queryForObject(
                """
                    SELECT t.id, t.session_id, t.stage, t.node_id, t.objective, t.created_at, t.updated_at,
                           COALESCE((
                               SELECT ta.status
                               FROM task_attempt ta
                               WHERE ta.task_id = t.id
                               ORDER BY ta.created_at DESC
                               LIMIT 1
                           ), 'PENDING') AS status,
                           (
                               SELECT ta.output_json
                               FROM task_attempt ta
                               WHERE ta.task_id = t.id
                               ORDER BY ta.created_at DESC
                               LIMIT 1
                           ) AS output_json
                    FROM task t
                    WHERE t.id = ?
                    """,
                (rs, rowNum) -> mapTask(rs),
                id
            );
            return Optional.ofNullable(task);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public List<Task> findBySessionIdWithStatus(Long sessionId) {
        return jdbcTemplate.query(
            """
                SELECT t.id, t.session_id, t.stage, t.node_id, t.objective, t.created_at, t.updated_at,
                       COALESCE((
                           SELECT ta.status
                           FROM task_attempt ta
                           WHERE ta.task_id = t.id
                           ORDER BY ta.created_at DESC
                           LIMIT 1
                       ), 'PENDING') AS status,
                       (
                           SELECT ta.output_json
                           FROM task_attempt ta
                           WHERE ta.task_id = t.id
                           ORDER BY ta.created_at DESC
                           LIMIT 1
                       ) AS output_json
                FROM task t
                WHERE t.session_id = ?
                ORDER BY t.created_at ASC, t.id ASC
                """,
            (rs, rowNum) -> mapTask(rs),
            sessionId
        );
    }

    @Override
    public Optional<Task> findFirstBySessionIdAndNodeIdAndStage(Long sessionId, Long nodeId, Stage stage) {
        List<Task> tasks = jdbcTemplate.query(
            """
                SELECT t.id, t.session_id, t.stage, t.node_id, t.objective, t.created_at, t.updated_at,
                       COALESCE((
                           SELECT ta.status
                           FROM task_attempt ta
                           WHERE ta.task_id = t.id
                           ORDER BY ta.created_at DESC
                           LIMIT 1
                       ), 'PENDING') AS status,
                       (
                           SELECT ta.output_json
                           FROM task_attempt ta
                           WHERE ta.task_id = t.id
                           ORDER BY ta.created_at DESC
                           LIMIT 1
                       ) AS output_json
                FROM task t
                WHERE t.session_id = ?
                  AND t.node_id = ?
                  AND t.stage = ?::task_stage
                ORDER BY t.created_at ASC, t.id ASC
                LIMIT 1
                """,
            (rs, rowNum) -> mapTask(rs),
            sessionId,
            nodeId,
            stage.name()
        );
        return tasks.stream().findFirst();
    }

    @Override
    public Long createRunningAttempt(Long taskId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                """
                    INSERT INTO task_attempt (task_id, status, started_at)
                    VALUES (?, 'RUNNING'::run_status, now())
                    """,
                new String[]{"id"}
            );
            ps.setLong(1, taskId);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to create task attempt.");
        }
        return key.longValue();
    }

    @Override
    public void markAttemptSucceeded(Long attemptId, String outputJson) {
        jdbcTemplate.update(
            """
                UPDATE task_attempt
                SET status = 'SUCCEEDED'::run_status,
                    output_json = CAST(? AS jsonb),
                    finished_at = now()
                WHERE id = ?
                """,
            outputJson,
            attemptId
        );
    }

    private Task mapTask(java.sql.ResultSet rs) throws java.sql.SQLException {
        Task task = new Task();
        task.setId(rs.getLong("id"));
        task.setSessionId(rs.getLong("session_id"));
        task.setStage(Stage.valueOf(rs.getString("stage")));
        task.setNodeId(rs.getLong("node_id"));
        task.setObjective(rs.getString("objective"));
        task.setStatus(TaskStatus.fromDb(rs.getString("status")));
        task.setOutputJson(rs.getString("output_json"));
        task.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        task.setUpdatedAt(rs.getObject("updated_at", java.time.OffsetDateTime.class));
        return task;
    }
}



```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\java\com\pandanav\learning\infrastructure\persistence\package-info.java
```java
package com.pandanav.learning.infrastructure.persistence;
```

## 6) Flyway 迁移后的真实表结构
### migration 顺序
- V1__create_enum_types.sql
- V2__create_learning_session.sql
- V3__create_concept_node_tables.sql
- V4__add_session_current_node_fk.sql
- V5__create_task_tables.sql
- V6__create_mastery.sql
- V7__create_evidence.sql

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\resources\db\migration\V1__create_enum_types.sql
```sql
-- Enum types
DO $$ BEGIN
  CREATE TYPE task_stage AS ENUM ('STRUCTURE','UNDERSTANDING','TRAINING','REFLECTION');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
  CREATE TYPE run_status AS ENUM ('PENDING','RUNNING','SUCCEEDED','FAILED','CANCELLED');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\resources\db\migration\V2__create_learning_session.sql
```sql
-- Learning session
CREATE TABLE IF NOT EXISTS learning_session (
  id BIGSERIAL PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  course_id VARCHAR(64) NOT NULL,
  chapter_id VARCHAR(64) NOT NULL,
  goal_text TEXT,
  current_node_id BIGINT,
  current_stage task_stage,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (user_id, chapter_id)
);

CREATE INDEX IF NOT EXISTS idx_session_user ON learning_session(user_id);
CREATE INDEX IF NOT EXISTS idx_session_course_chapter ON learning_session(course_id, chapter_id);
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\resources\db\migration\V3__create_concept_node_tables.sql
```sql
-- Concept node and prerequisites
CREATE TABLE IF NOT EXISTS concept_node (
  id BIGSERIAL PRIMARY KEY,
  chapter_id VARCHAR(64) NOT NULL,
  name VARCHAR(128) NOT NULL,
  outline TEXT,
  order_no INT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (chapter_id, name)
);

CREATE INDEX IF NOT EXISTS idx_concept_node_chapter_order ON concept_node(chapter_id, order_no);

CREATE TABLE IF NOT EXISTS concept_prerequisite (
  node_id BIGINT NOT NULL REFERENCES concept_node(id) ON DELETE CASCADE,
  prereq_node_id BIGINT NOT NULL REFERENCES concept_node(id) ON DELETE RESTRICT,
  PRIMARY KEY (node_id, prereq_node_id),
  CHECK (node_id <> prereq_node_id)
);

CREATE INDEX IF NOT EXISTS idx_prereq_prereq ON concept_prerequisite(prereq_node_id);
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\resources\db\migration\V4__add_session_current_node_fk.sql
```sql
-- Add foreign key to session.current_node_id after concept_node exists.
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'fk_session_current_node'
  ) THEN
    ALTER TABLE learning_session
      ADD CONSTRAINT fk_session_current_node
      FOREIGN KEY (current_node_id) REFERENCES concept_node(id)
      DEFERRABLE INITIALLY DEFERRED;
  END IF;
END $$;
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\resources\db\migration\V5__create_task_tables.sql
```sql
-- Task and task attempts
CREATE TABLE IF NOT EXISTS task (
  id BIGSERIAL PRIMARY KEY,
  session_id BIGINT NOT NULL REFERENCES learning_session(id) ON DELETE CASCADE,
  stage task_stage NOT NULL,
  node_id BIGINT NOT NULL REFERENCES concept_node(id) ON DELETE RESTRICT,
  objective TEXT NOT NULL,
  input_json JSONB,
  expected_output_schema JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_task_session_stage_created ON task(session_id, stage, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_task_node ON task(node_id);

CREATE TABLE IF NOT EXISTS task_attempt (
  id BIGSERIAL PRIMARY KEY,
  task_id BIGINT NOT NULL REFERENCES task(id) ON DELETE CASCADE,
  status run_status NOT NULL DEFAULT 'PENDING',
  run_input_json JSONB,
  output_json JSONB,
  score INT,
  error_tags JSONB NOT NULL DEFAULT '[]'::jsonb,
  feedback_json JSONB,
  started_at TIMESTAMPTZ,
  finished_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_attempt_task_created ON task_attempt(task_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_attempt_status_created ON task_attempt(status, created_at);
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\resources\db\migration\V6__create_mastery.sql
```sql
-- Mastery
CREATE TABLE IF NOT EXISTS mastery (
  user_id VARCHAR(64) NOT NULL,
  node_id BIGINT NOT NULL REFERENCES concept_node(id) ON DELETE CASCADE,
  mastery_value NUMERIC(4,3) NOT NULL DEFAULT 0.000,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, node_id)
);

CREATE INDEX IF NOT EXISTS idx_mastery_user_updated ON mastery(user_id, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_mastery_node ON mastery(node_id);
```

### D:\Panda_Code\AI_Learning_Workflow_Navigator\backend\src\main\resources\db\migration\V7__create_evidence.sql
```sql
-- Evidence records for evaluator outputs and decision traces
CREATE TABLE IF NOT EXISTS evidence (
  id BIGSERIAL PRIMARY KEY,
  task_id BIGINT NOT NULL REFERENCES task(id) ON DELETE CASCADE,
  evidence_type VARCHAR(64) NOT NULL,
  content_json JSONB NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_evidence_task_created ON evidence(task_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_evidence_type_created ON evidence(evidence_type, created_at DESC);
```

### 最终表结构视图（汇总）
- 枚举：`task_stage(STRUCTURE,UNDERSTANDING,TRAINING,REFLECTION)`、`run_status(PENDING,RUNNING,SUCCEEDED,FAILED,CANCELLED)`
- `learning_session`：主键 `id`，唯一 `(user_id, chapter_id)`，`current_node_id` 外键 -> `concept_node.id`
- `concept_node`：主键 `id`，唯一 `(chapter_id,name)`，索引 `(chapter_id,order_no)`
- `concept_prerequisite`：复合主键 `(node_id,prereq_node_id)`，双外键到 `concept_node`
- `task`：主键 `id`，外键 `session_id/node_id`，索引 `(session_id,stage,created_at)`、`(node_id)`
- `task_attempt`：主键 `id`，外键 `task_id`，字段 `status/output_json/score/error_tags/feedback_json`
- `mastery`：复合主键 `(user_id,node_id)`，外键 `node_id` -> `concept_node.id`
- `evidence`：主键 `id`，外键 `task_id` -> `task.id`，字段 `evidence_type/content_json`

### 业务对象映射
- Session -> `learning_session`
- ConceptNode -> `concept_node` (+ `concept_prerequisite`)
- Task -> `task`，Task运行状态/输出 -> `task_attempt` 最新记录
- Mastery -> `mastery`
- Evidence -> `evidence`

## 7) 前端页面草图 / 页面字段需求（基于现有后端能力）
### 页面清单
1. 会话创建页
2. 会话时间线总览页
3. 任务执行页（run）
4. 训练提交页（submit）
5. 评估反馈与下一步页
6. DB健康/迁移调试页（内部）

### 页面需求
| 页面 | 字段需求 | 用户动作 | 调用接口 | 成功态 | 错误态 |
|---|---|---|---|---|---|
| 会话创建 | user_id, course_id, chapter_id, goal_text | 点击创建 | POST /api/session/create | 返回 session_id 后跳转总览 | 400 参数错误, 409 已存在, 500 |
| 时间线总览 | session基本信息, timeline, next_task, mastery_summary | 刷新/进入任务 | GET /api/session/{id}/overview | 渲染时间线和下一任务 | 404 会话不存在, 500 |
| 任务执行 | task_id | 点击运行任务 | POST /api/task/{taskId}/run | 显示 output JSON sections/questions | 404/409/500 |
| 训练提交 | user_answer, task_id | 提交答案 | POST /api/task/{taskId}/submit | 显示 score/error_tags/feedback/mastery/next_task | 400/404/409/500 |
| 反馈页 | score, error_tags, feedback, mastery变化, next_action, next_task | 去做下一题/回总览 | 来自 submit 返回 + 可回调 overview | 展示闭环推进 | 若 next_task 为空则提示已到末节点/NOOP |
| 调试页 | db状态,migrations | 查看状态 | GET /debug/db + /health | 显示db/flyway正常 | DB连接异常 |

### 需后端补充
- 需后端补充：按 session 获取某 task 详情接口（当前仅 run/submit by taskId）。
- 需后端补充：任务列表筛选/分页接口（当前依赖 overview 聚合）。
- 需后端补充：鉴权与 user 绑定（当前 MVP 使用 request 传 user_id）。

## 8) 页面-接口对齐表（必须）
| 页面 | 用户动作 | 接口 | 请求字段 | 响应字段 | 状态码 | 是否可直接联调 | 阻塞点 |
|---|---|---|---|---|---|---|---|
| 会话创建 | 创建会话 | POST /api/session/create | user_id,course_id,chapter_id,goal_text | session_id | 200/400/409/500 | 是 | 无 |
| 总览 | 拉取时间线 | GET /api/session/{sessionId}/overview | path: sessionId | timeline,next_task,mastery_summary,... | 200/404/500 | 是 | 无 |
| 任务执行 | 运行任务 | POST /api/task/{taskId}/run | path: taskId | stage,node_id,status,output | 200/404/409/500 | 是 | 前端需知道taskId来源 |
| 训练提交 | 提交答案 | POST /api/task/{taskId}/submit | path taskId + body user_answer | score,error_tags,feedback,mastery,next_action,next_task | 200/400/404/409/500 | 是 | 前端需先运行TRAINING任务 |
| 反馈导航 | 跳转下一任务 | submit返回next_task 或 overview.next_task | - | next_task.task_id | - | 部分可 | next_task为空时分支处理需前端定义 |

## 9) 最小改造优先方案（P0/P1/P2）
### P0（必须）
| 改动点 | 涉及文件 | 风险 | 工作量 |
|---|---|---|---|
| 清理 domain/model 下占位枚举 Stage/TaskStatus，避免双定义混淆 | domain/model/Stage.java, TaskStatus.java + 全量引用核对 | 中 | S |
| 为 submit 增加 task_attempt 落库（score/error_tags/feedback_json）保证历史可追踪 | SubmitTrainingAnswerService, JdbcTaskRepository | 中 | M |
| 增加按 taskId 查询详情接口（供前端直接打开任务） | api/controller + usecase/service + repository | 低 | M |
### P1（提升稳定性）
| 改动点 | 涉及文件 | 风险 | 工作量 |
|---|---|---|---|
| 完整单测覆盖 submit 分支（低分补救/高分推进/NOOP） | SubmitTrainingAnswerServiceTest | 低 | M |
| NextActionPolicy 引入 errorTags 特判（当前主要按 score） | ScoreBasedNextActionPolicy | 低 | S |
| 统一 DTO 示例与编码（修正历史乱码注释） | api/dto/* | 低 | S |
### P2（优化）
| 改动点 | 涉及文件 | 风险 | 工作量 |
|---|---|---|---|
| 前端友好响应：增加 human_message 与 i18n code | dto + service mapping | 中 | M |
| 增加任务检索/分页接口 | controller/usecase/repository | 中 | M |
| 引入 EvaluationRule 插件式规则集合 | domain/policy + application/evaluator | 中 | M/L |

## 10) 给 Cursor/Codex 的工程任务清单
1. 任务：移除 domain/model 冗余枚举占位并统一引用
- 目标：仅保留 `domain/enums` 枚举
- 文件：`backend/src/main/java/com/pandanav/learning/domain/model/Stage.java`, `TaskStatus.java` 及全引用
- 验收：`mvn -q -DskipTests compile` 通过；`Select-String` 无 `domain.model.Stage/TaskStatus` 引用
- 测试：编译 + run/submit smoke
2. 任务：submit 写入 task_attempt 评估结果
- 目标：提交答案后 `task_attempt` 有 score/error_tags/feedback_json 记录
- 文件：`SubmitTrainingAnswerService`, `JdbcTaskRepository`
- 验收：DB 查询可见最新 attempt 评分字段
- 测试：集成测试一条 submit 请求
3. 任务：新增 task detail 接口
- 目标：前端可直接按 taskId 拉取详情（含状态和输出）
- 文件：`TaskController`, `application/usecase+service`, `TaskRepository/JdbcTaskRepository`
- 验收：OpenAPI 可见新接口，返回契约明确
- 测试：404/200/冲突分支
4. 任务：完善 submit 策略分支单测
- 目标：覆盖 `INSERT_REMEDIAL_UNDERSTANDING / INSERT_TRAINING_VARIANTS / INSERT_TRAINING_REINFORCEMENT / ADVANCE_TO_NEXT_NODE / NOOP`
- 文件：`SubmitTrainingAnswerServiceTest`
- 验收：分支覆盖通过
- 测试：`mvn -q -Dtest=SubmitTrainingAnswerServiceTest test`
5. 任务：前端联调契约文档固化
- 目标：生成页面字段与接口映射文档供前端执行
- 文件：`docs/STEP345_DELIVERY.md`（本文件）
- 验收：前端可按文档完成页面与接口绑定
- 测试：手动联调 checklist

## 风险与假设清单
- 风险：`task_attempt.run_status` 仍含 `CANCELLED`，而 domain `TaskStatus` 仅四态，当前通过 `fromDb` 兼容映射为 FAILED。
- 风险：`SubmitTrainingAnswerService` 当前未写 `task_attempt` 评分字段，仅写 `evidence`。若需完整可追踪，需补 P0。
- 假设：前端通过 `overview.next_task.task_id` 驱动进入 run/submit。
- 假设：MVP 阶段不做鉴权，`user_id` 由创建会话请求传入。
- 假设：`GenerateTasksService/GetSessionService/StartSessionService` 仍为骨架，不纳入本轮页面主链路。
