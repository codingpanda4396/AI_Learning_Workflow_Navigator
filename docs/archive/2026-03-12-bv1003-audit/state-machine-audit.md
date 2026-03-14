# State Machine Audit

## Scope

- Focused targets: `session status`, `task status`, `practice status`
- Scan scope: `backend/src`, `frontend/src`, `spec`
- No code changes were made

## 1. Session Status

### 1.1 Current definitions

| Type | Values found | Definition location |
| --- | --- | --- |
| `LearningSession.status` | string, no enum constraint in code | `backend/src/main/java/com/pandanav/learning/domain/model/LearningSession.java:17` |
| DB default | `ACTIVE` | `backend/src/main/resources/db/migration/V11__add_user_and_session_history.sql:15` |
| Session create default | `ACTIVE` | `backend/src/main/java/com/pandanav/learning/application/service/CreateSessionService.java:55` |
| Learning plan confirm default | `ACTIVE` | `backend/src/main/java/com/pandanav/learning/application/service/learningplan/LearningPlanService.java:125` |
| Session path visual status | `COMPLETED`, `IN_PROGRESS`, `NOT_STARTED` | `backend/src/main/java/com/pandanav/learning/application/service/GetSessionPathService.java:20-22` |
| Session runtime reused from practice quiz status | `GENERATING`, `QUIZ_READY`, `ANSWERED`, `FEEDBACK_READY`, `REVIEWING`, `NEXT_ROUND`, `FAILED` | `backend/src/main/java/com/pandanav/learning/application/service/PracticeServiceImpl.java:133,171,334,339,402,412` |

### 1.2 Reference locations

| Purpose | Reference locations |
| --- | --- |
| Persistence read/write | `backend/src/main/java/com/pandanav/learning/infrastructure/persistence/JdbcLearningSessionRepository.java:32-45,117-124,137-169,173-187,209-219,236-247` |
| History list/filter by status | `backend/src/main/java/com/pandanav/learning/application/service/SessionHistoryService.java:43-69` |
| Practice flow updates session status | `backend/src/main/java/com/pandanav/learning/application/service/PracticeServiceImpl.java:133,171,334,339,402,412` |
| Session detail output | `backend/src/main/java/com/pandanav/learning/application/service/SessionHistoryService.java:61` |

### 1.3 Observed session status values

Current actual values written into `learning_session.status`:

- `ACTIVE`
- `GENERATING`
- `QUIZ_READY`
- `ANSWERED`
- `FEEDBACK_READY`
- `REVIEWING`
- `NEXT_ROUND`
- `FAILED`

Related but not actually session status:

- `COMPLETED`, `IN_PROGRESS`, `NOT_STARTED` for path node display

## 2. Task Status

### 2.1 Current definitions

| Type | Values found | Definition location |
| --- | --- | --- |
| DB enum `run_status` | `PENDING`, `RUNNING`, `SUCCEEDED`, `FAILED`, `CANCELLED` | `backend/src/main/resources/db/migration/V1__create_enum_types.sql:7-9` |
| `TaskStatus` enum | `PENDING`, `RUNNING`, `SUCCEEDED`, `FAILED` | `backend/src/main/java/com/pandanav/learning/domain/enums/TaskStatus.java:3-7` |
| Legacy mapping | DB `CANCELLED` -> app `FAILED` | `backend/src/main/java/com/pandanav/learning/domain/enums/TaskStatus.java:13-15` |
| Spec enum | `PENDING`, `RUNNING`, `SUCCEEDED`, `FAILED` | `spec/02_api_contract.md:19-23` |

### 2.2 Reference locations

| Purpose | Reference locations |
| --- | --- |
| Task domain field | `backend/src/main/java/com/pandanav/learning/domain/model/Task.java:15,61-65,86-107` |
| Task repository persistence | `backend/src/main/java/com/pandanav/learning/infrastructure/persistence/JdbcTaskRepository.java:52-53,388` |
| Session overview/history aggregation | `backend/src/main/java/com/pandanav/learning/application/service/GetSessionOverviewService.java:58,63,80`; `backend/src/main/java/com/pandanav/learning/application/service/SessionHistoryService.java:101` |
| Task planning / next-status summary | `backend/src/main/java/com/pandanav/learning/application/service/PlanSessionTasksService.java:91,148,182,198-207` |
| Training submit flow | `backend/src/main/java/com/pandanav/learning/application/service/SubmitTrainingAnswerService.java:259,291` |
| Task execution flow | `backend/src/main/java/com/pandanav/learning/application/service/TaskRunnerService.java:76,79,231` |
| Learning plan generated tasks | `backend/src/main/java/com/pandanav/learning/application/service/learningplan/LearningPlanService.java:284` |
| Tests | `backend/src/test/java/com/pandanav/learning/application/service/PlanSessionTasksServiceTest.java`; `backend/src/test/java/com/pandanav/learning/application/service/PracticeServiceImplTest.java`; `backend/src/test/java/com/pandanav/learning/application/service/SubmitTrainingAnswerServiceTest.java`; `backend/src/test/java/com/pandanav/learning/application/service/TaskRunnerServiceTest.java` |

### 2.3 Task-like reuse outside `Task`

The same `TaskStatus` / `run_status` is also reused for non-task runtime fields:

- `PracticeQuiz.generationStatus`
  - Definition: `backend/src/main/java/com/pandanav/learning/domain/model/PracticeQuiz.java:18`
  - Persistence: `backend/src/main/java/com/pandanav/learning/infrastructure/persistence/JdbcPracticeQuizRepository.java:50,133,158,201`
- `PracticeSubmission.judgingStatus`
  - Definition: `backend/src/main/java/com/pandanav/learning/domain/model/PracticeSubmission.java:20`
  - Persistence: `backend/src/main/java/com/pandanav/learning/infrastructure/persistence/JdbcPracticeSubmissionRepository.java:57,207`
- `PracticeFeedbackReport.reportStatus`
  - Definition: `backend/src/main/java/com/pandanav/learning/domain/model/PracticeFeedbackReport.java:23`
  - Persistence: `backend/src/main/java/com/pandanav/learning/infrastructure/persistence/JdbcPracticeFeedbackReportRepository.java:56,128`

## 3. Practice Status

### 3.1 Current definitions

| Type | Values found | Definition location |
| --- | --- | --- |
| `PracticeItemStatus` enum | `GENERATED`, `ACTIVE`, `ANSWERED`, `ARCHIVED` | `backend/src/main/java/com/pandanav/learning/domain/enums/PracticeItemStatus.java:3-7` |
| DB check `practice_item.status` | `GENERATED`, `ACTIVE`, `ANSWERED`, `ARCHIVED` | `backend/src/main/resources/db/migration/V13__create_practice_tables.sql:15,32-33` |
| `PracticeQuizStatus` enum | `GENERATING`, `QUIZ_READY`, `ANSWERED`, `FEEDBACK_READY`, `REVIEWING`, `NEXT_ROUND`, `FAILED` | `backend/src/main/java/com/pandanav/learning/domain/enums/PracticeQuizStatus.java:3-10` |
| DB check `practice_quiz.status` | `GENERATING`, `QUIZ_READY`, `ANSWERED`, `FEEDBACK_READY`, `REVIEWING`, `NEXT_ROUND`, `FAILED` | `backend/src/main/resources/db/migration/V16__create_practice_quiz_and_feedback.sql:7,23-24` |
| Practice generation runtime status | `PENDING`, `RUNNING`, `SUCCEEDED`, `FAILED`, legacy DB `CANCELLED` | `backend/src/main/java/com/pandanav/learning/domain/model/PracticeQuiz.java:18`; `backend/src/main/resources/db/migration/V17__align_practice_models_for_mvp.sql:2,12-25` |
| Practice judging runtime status | `PENDING`, `RUNNING`, `SUCCEEDED`, `FAILED`, legacy DB `CANCELLED` | `backend/src/main/java/com/pandanav/learning/domain/model/PracticeSubmission.java:20`; `backend/src/main/resources/db/migration/V17__align_practice_models_for_mvp.sql:28-40` |
| Practice feedback report runtime status | `PENDING`, `RUNNING`, `SUCCEEDED`, `FAILED`, legacy DB `CANCELLED` | `backend/src/main/java/com/pandanav/learning/domain/model/PracticeFeedbackReport.java:23`; `backend/src/main/resources/db/migration/V17__align_practice_models_for_mvp.sql:47-64` |

### 3.2 Reference locations

| Purpose | Reference locations |
| --- | --- |
| Practice quiz lifecycle transitions | `backend/src/main/java/com/pandanav/learning/application/service/PracticeServiceImpl.java:118,128-133,156-171,318-339,401-412` |
| Practice item lifecycle transitions | `backend/src/main/java/com/pandanav/learning/application/service/PracticeServiceImpl.java:261,366` |
| Practice controllers serialize status | `backend/src/main/java/com/pandanav/learning/api/controller/PracticeQuizController.java:110,127`; `backend/src/main/java/com/pandanav/learning/api/controller/PracticeItemController.java:120`; `backend/src/main/java/com/pandanav/learning/api/controller/SessionQuizController.java:72-73,113,118,141` |
| Practice repositories | `backend/src/main/java/com/pandanav/learning/domain/repository/PracticeQuizRepository.java:17,19`; `backend/src/main/java/com/pandanav/learning/domain/repository/PracticeRepository.java:23`; `backend/src/main/java/com/pandanav/learning/infrastructure/persistence/JdbcPracticeQuizRepository.java:44,50,113,129,133,158,195,201`; `backend/src/main/java/com/pandanav/learning/infrastructure/persistence/JdbcPracticeRepository.java:56,173,201`; `backend/src/main/java/com/pandanav/learning/infrastructure/persistence/JdbcPracticeSubmissionRepository.java:57,207`; `backend/src/main/java/com/pandanav/learning/infrastructure/persistence/JdbcPracticeFeedbackReportRepository.java:56,128` |
| Frontend quiz snapshot/status consumption | `frontend/src/types/quiz.ts:15-16`; `frontend/src/stores/quiz.ts:20,35-42,75`; `frontend/src/views/QuizView.vue:40,67,69` |

### 3.3 Observed practice status sets

Practice has at least 4 different status families:

1. `PracticeItemStatus`
   - `GENERATED`, `ACTIVE`, `ANSWERED`, `ARCHIVED`
2. `PracticeQuizStatus`
   - `GENERATING`, `QUIZ_READY`, `ANSWERED`, `FEEDBACK_READY`, `REVIEWING`, `NEXT_ROUND`, `FAILED`
3. Generation runtime status
   - `PENDING`, `RUNNING`, `SUCCEEDED`, `FAILED`
4. Judging/report runtime status
   - `PENDING`, `RUNNING`, `SUCCEEDED`, `FAILED`

## 4. Audit Findings

### 4.1 Duplicate definitions

- `TaskStatus` is defined in 3 places:
  - DB enum `run_status`
  - backend enum `TaskStatus`
  - API spec `TaskStatus`
- `PracticeItemStatus` is defined twice:
  - DB check constraint
  - backend enum
- `PracticeQuizStatus` is defined twice:
  - DB check constraint
  - backend enum
- Session status has no single enum source of truth.
  - It is a raw string in domain model and repository
  - Values are injected from service code and partially borrowed from `PracticeQuizStatus`

### 4.2 Naming conflicts

- `LearningSession.status` is a session field, but values are currently populated with practice quiz statuses such as `QUIZ_READY`, `FEEDBACK_READY`, `NEXT_ROUND`.
- `TaskStatus` is task-oriented by name, but also drives:
  - `generationStatus`
  - `judgingStatus`
  - `reportStatus`
- `ANSWERED` appears in both:
  - `PracticeItemStatus`
  - `PracticeQuizStatus`
  - but with different granularity
- Frontend quiz page expects alien values:
  - `READY`, `COMPLETED`, `SUBMITTED`
  - source: `frontend/src/stores/quiz.ts:41`; `frontend/src/views/QuizView.vue:40,67`
  - backend actually uses `QUIZ_READY`, `ANSWERED`, `FEEDBACK_READY`, `REVIEWING`, `NEXT_ROUND`

### 4.3 Historical / legacy states

- DB still preserves `CANCELLED` in `run_status`, but backend enum deleted it and maps it to `FAILED`.
  - `backend/src/main/resources/db/migration/V1__create_enum_types.sql:8`
  - `backend/src/main/java/com/pandanav/learning/domain/enums/TaskStatus.java:13-15`
- `PracticeItemStatus.GENERATED` exists as DB default, but current service directly creates items as `ACTIVE`.
  - likely historical transitional state
  - `backend/src/main/resources/db/migration/V13__create_practice_tables.sql:15`
  - `backend/src/main/java/com/pandanav/learning/application/service/PracticeServiceImpl.java:366`
- `GetSessionPathService` uses `COMPLETED / IN_PROGRESS / NOT_STARTED`, but these are path-node display states, not persisted session states.
  - easy to confuse with real session status
- `completed_at` exists on `learning_session`, but no scanned logic writes session `COMPLETED`.
  - suggests incomplete or abandoned session terminal state design

## 5. Recommended Unified Status Tables

### 5.1 Recommended `SessionStatus`

This is a proposed unified high-level session state machine for the whole learning journey:

| Status | Meaning |
| --- | --- |
| `ANALYZING` | diagnosis / capability analysis in progress |
| `PLANNING` | learning plan generation / confirmation in progress |
| `LEARNING` | normal task learning in progress |
| `PRACTICING` | quiz generation / answer / review loop in progress |
| `REPORT_READY` | practice feedback or report is ready |
| `COMPLETED` | chapter/session completed |
| `FAILED` | unrecoverable failure |

Suggestion:

- `learning_session.status` should only store this high-level enum
- practice sub-flow status should not be written back as session status verbatim

### 5.2 Recommended `TaskStatus`

Keep task runtime status narrow and technical:

| Status | Meaning |
| --- | --- |
| `PENDING` | waiting to run |
| `RUNNING` | currently executing |
| `SUCCEEDED` | finished successfully |
| `FAILED` | finished with failure |

Suggestion:

- Remove `CANCELLED` from DB if no longer supported, or restore it end-to-end
- Do not mix task runtime status with business workflow status

### 5.3 Recommended `PracticeQuizStatus`

Keep quiz business state focused on learner-visible phase:

| Status | Meaning |
| --- | --- |
| `GENERATING` | questions are being prepared |
| `READY` | questions ready for answering |
| `ANSWERING` | learner has started but not finished |
| `REVIEWING` | feedback/review decision in progress |
| `REPORT_READY` | feedback report ready |
| `NEXT_ROUND` | next round selected |
| `FAILED` | quiz flow failed |

Mapping from current values:

- `QUIZ_READY` -> `READY`
- `ANSWERED` -> split:
  - if learner is still inside answering flow: `ANSWERING`
  - if all answers submitted and waiting for feedback: consider `REVIEWING`
- `FEEDBACK_READY` -> `REPORT_READY`

### 5.4 Recommended `PracticeItemStatus`

| Status | Meaning |
| --- | --- |
| `READY` | available to answer |
| `ANSWERED` | answered |
| `ARCHIVED` | hidden / obsolete |

Suggestion:

- Merge `GENERATED` and `ACTIVE` unless they are truly different business phases

## 6. Bottom Line

- `task status` is closest to unified, but still has DB/app/spec duplication and legacy `CANCELLED`
- `practice status` is the most fragmented: one business status family plus multiple runtime status families
- `session status` is the most confused: no enum, raw string storage, and currently polluted by practice quiz statuses
- The first cleanup priority should be:
  1. define a real `SessionStatus` enum
  2. stop reusing `PracticeQuizStatus` values inside `learning_session.status`
  3. separate business status from runtime status consistently
