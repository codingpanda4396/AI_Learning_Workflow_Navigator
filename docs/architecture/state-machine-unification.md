# STATE_MACHINE_UNIFICATION

## 1. New Status Tables

### SessionStatus
| Status | Meaning |
| --- | --- |
| `ANALYZING` | Diagnosis / analysis phase before task plan is finalized |
| `PLANNING` | Session plan is being assembled |
| `LEARNING` | Session is in normal learning flow |
| `PRACTICING` | Session is in quiz / practice flow |
| `REPORT_READY` | Practice report is ready for the learner |
| `COMPLETED` | Session finished |
| `FAILED` | Session level failure |

### TaskStatus
| Status | Meaning |
| --- | --- |
| `PENDING` | Not started |
| `RUNNING` | Runtime executing |
| `SUCCEEDED` | Runtime finished successfully |
| `FAILED` | Runtime failed |

### PracticeQuizStatus
| Status | Meaning |
| --- | --- |
| `GENERATING` | Quiz business flow is preparing questions |
| `READY` | Questions are ready to answer |
| `ANSWERING` | Partial answers already exist, round still in progress |
| `REVIEWING` | Learner selected review after report |
| `REPORT_READY` | Report has been generated |
| `NEXT_ROUND` | Learner selected next round after report |
| `FAILED` | Quiz business flow failed |

### PracticeItemStatus
| Status | Meaning |
| --- | --- |
| `READY` | Item is ready to answer |
| `ANSWERED` | Item has at least one submission |
| `ARCHIVED` | Item is retired from active use |

### Runtime Status Separation
| Field | Status Set |
| --- | --- |
| `learning_session.status` | `SessionStatus` |
| `task_attempt.status` | `TaskStatus` |
| `practice_quiz.status` | `PracticeQuizStatus` |
| `practice_item.status` | `PracticeItemStatus` |
| `practice_quiz.generation_status` | `TaskStatus` |
| `practice_submission.judging_status` | `TaskStatus` |
| `practice_feedback_report.report_status` | `TaskStatus` |

## 2. Legacy Mapping

### Session status mapping
| Old | New |
| --- | --- |
| `ACTIVE` | `LEARNING` |
| `GENERATING` | `PRACTICING` |
| `QUIZ_READY` | `PRACTICING` |
| `ANSWERED` | `PRACTICING` |
| `FEEDBACK_READY` | `REPORT_READY` |
| `REVIEWING` | `LEARNING` |
| `NEXT_ROUND` | `PRACTICING` |
| `FAILED` | `FAILED` |

### Practice quiz mapping
| Old | New |
| --- | --- |
| `QUIZ_READY` | `READY` |
| `ANSWERED` | `ANSWERING` |
| `FEEDBACK_READY` | `REPORT_READY` |

### Practice item mapping
| Old | New |
| --- | --- |
| `GENERATED` | `READY` |
| `ACTIVE` | `READY` |

### Task runtime mapping
| Old | New |
| --- | --- |
| `CANCELLED` | `FAILED` |

## 3. Backend Modified Files

- `backend/src/main/java/com/pandanav/learning/domain/enums/SessionStatus.java`
- `backend/src/main/java/com/pandanav/learning/domain/enums/TaskStatus.java`
- `backend/src/main/java/com/pandanav/learning/domain/enums/PracticeQuizStatus.java`
- `backend/src/main/java/com/pandanav/learning/domain/enums/PracticeItemStatus.java`
- `backend/src/main/java/com/pandanav/learning/domain/model/LearningSession.java`
- `backend/src/main/java/com/pandanav/learning/domain/repository/SessionRepository.java`
- `backend/src/main/java/com/pandanav/learning/infrastructure/persistence/JdbcLearningSessionRepository.java`
- `backend/src/main/java/com/pandanav/learning/infrastructure/persistence/JdbcPracticeQuizRepository.java`
- `backend/src/main/java/com/pandanav/learning/application/service/CreateSessionService.java`
- `backend/src/main/java/com/pandanav/learning/application/service/PlanSessionTasksService.java`
- `backend/src/main/java/com/pandanav/learning/application/service/PracticeServiceImpl.java`
- `backend/src/main/java/com/pandanav/learning/application/service/GetCurrentSessionService.java`
- `backend/src/main/java/com/pandanav/learning/application/service/GetSessionOverviewService.java`
- `backend/src/main/java/com/pandanav/learning/application/service/SessionHistoryService.java`
- `backend/src/main/java/com/pandanav/learning/application/service/SubmitTrainingAnswerService.java`
- `backend/src/main/java/com/pandanav/learning/application/service/learningplan/LearningPlanService.java`
- `backend/src/main/java/com/pandanav/learning/api/dto/session/CurrentSessionInfoResponse.java`
- `backend/src/main/java/com/pandanav/learning/api/dto/session/SessionOverviewResponse.java`
- `backend/src/main/resources/db/migration/V21__unify_session_and_practice_statuses.sql`

## 4. Frontend Modified Files

- `frontend/src/types/quiz.ts`
- `frontend/src/types/session.ts`
- `frontend/src/api/normalizers.ts`
- `frontend/src/stores/quiz.ts`
- `frontend/src/utils/format.ts`
- `frontend/src/views/HomeView.vue`
- `frontend/src/views/QuizView.vue`
- `frontend/src/views/SessionView.vue`

## 5. Migration Added

- Added: `backend/src/main/resources/db/migration/V21__unify_session_and_practice_statuses.sql`

## 6. Compatibility Strategy

- Backend enum readers still accept legacy DB values and map them into the unified enums.
- `findLatestActiveByUserPk` still recognizes legacy active / polluted session statuses before migration runs.
- DB migration rewrites legacy session / quiz / item values into the new public status sets.
- `run_status` is rebuilt without `CANCELLED`; old `CANCELLED` rows are coerced to `FAILED` before type conversion.

## 7. Remaining Risks

- Existing external clients that still hard-code `QUIZ_READY` / `ANSWERED` / `FEEDBACK_READY` will need to switch to the new quiz status names.
- Historical API examples in older docs may still mention pre-unification values.
- `SessionStatus.COMPLETED` is defined, but current flow still does not mark a session completed automatically at the end of all learning paths.
