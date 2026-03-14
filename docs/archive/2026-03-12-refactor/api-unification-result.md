# API Unification Result

## 1. Unified APIs

| Domain | Standard API |
| --- | --- |
| Learning plan preview | `POST /api/learning-plans/preview` |
| Learning plan confirm | `POST /api/learning-plans/{planId}/confirm` |
| Learning plan detail | `GET /api/learning-plans/{planId}` |
| Session create | `POST /api/sessions/create` |
| Session plan | `POST /api/sessions/{sessionId}/plan?mode=auto` |
| Session overview | `GET /api/sessions/{sessionId}/overview` |
| Session current | `GET /api/sessions/current` |
| Session report | `GET /api/sessions/{sessionId}/feedback` |
| Session next action | `POST /api/sessions/{sessionId}/next-action` |
| Session feedback detail | `GET /api/sessions/{sessionId}/learning-feedback/report` |
| Session weak points | `GET /api/sessions/{sessionId}/learning-feedback/weak-points` |
| Session growth | `GET /api/sessions/{sessionId}/growth-dashboard` |
| Session quiz | `POST/GET /api/sessions/{sessionId}/quiz/*` |
| Task detail | `GET /api/tasks/{taskId}` |
| Task run | `POST /api/tasks/{taskId}/run` |
| Nested practice APIs | `/api/sessions/{sessionId}/tasks/{taskId}/...` |

## 2. Old -> New Mapping

| Old | New | Status |
| --- | --- | --- |
| `/api/learning-plan/preview` | `/api/learning-plans/preview` | Frontend migrated |
| `/api/learning-plan/regenerate` | `/api/learning-plans/preview` | Frontend migrated to real preview flow |
| `/api/learning-plan/confirm` | `/api/learning-plans/{planId}/confirm` | Old path removed from frontend |
| `/api/session/create` | `/api/sessions/create` | Kept backend compatibility |
| `/api/session/{sessionId}/plan` | `/api/sessions/{sessionId}/plan` | Kept backend compatibility |
| `/api/session/{sessionId}/overview` | `/api/sessions/{sessionId}/overview` | Kept backend compatibility |
| `/api/session/current` | `/api/sessions/current` | Kept backend compatibility |
| `/api/session/{sessionId}/learning-feedback/*` | `/api/sessions/{sessionId}/learning-feedback/*` | Kept backend compatibility |
| `/api/session/{sessionId}/growth-dashboard` | `/api/sessions/{sessionId}/growth-dashboard` | Kept backend compatibility |
| `/api/task/{taskId}` | `/api/tasks/{taskId}` | Kept backend compatibility |
| `/api/task/{taskId}/run` | `/api/tasks/{taskId}/run` | Kept backend compatibility |

## 3. Frontend Modified Files

- `frontend/src/api/modules/learningPlan.ts`
- `frontend/src/api/modules/session.ts`
- `frontend/src/api/modules/feedback.ts`
- `frontend/src/api/modules/task.ts`
- `frontend/src/api/normalizers.ts`
- `frontend/src/types/learningPlan.ts`
- `frontend/src/stores/learningPlan.ts`
- `frontend/src/views/DiagnosisView.vue`
- `frontend/src/views/LearningPlanView.vue`
- `frontend/src/views/HomeView.vue`
- `frontend/src/components/home/CurrentSessionPanel.vue`
- `frontend/src/components/home/GrowthSummaryPanel.vue`
- `frontend/src/mocks/learningPlan.ts`

## 4. Backend Modified Files

- `backend/src/main/java/com/pandanav/learning/api/controller/SessionController.java`
- `backend/src/main/java/com/pandanav/learning/api/controller/SessionHistoryController.java`
- `backend/src/main/java/com/pandanav/learning/api/controller/LearningFeedbackController.java`
- `backend/src/main/java/com/pandanav/learning/api/controller/LearningInsightController.java`
- `backend/src/main/java/com/pandanav/learning/api/controller/TaskController.java`
- `backend/src/main/java/com/pandanav/learning/api/controller/PracticeItemController.java`
- `backend/src/main/java/com/pandanav/learning/api/controller/PracticeQuizController.java`
- `backend/src/main/java/com/pandanav/learning/api/controller/PracticeSubmissionController.java`
- `backend/src/main/java/com/pandanav/learning/api/controller/TutorMessageController.java`

## 5. Compatibility Layer

- Retained for legacy single-resource session routes: `/api/session/**`
- Retained for legacy single-resource task routes: `/api/task/**`
- Retained for nested legacy session-task routes: `/api/session/{sessionId}/tasks/{taskId}/**`
- Not retained for broken learning-plan confirm body route `/api/learning-plan/confirm` because backend requires `planId` path semantics

## 6. Fixed Page Route Jumps

- Home current session: `/session/:id` -> `/sessions/:sessionId`
- Home growth entry: `/growth` -> `/sessions/:sessionId/growth`
- Learning plan confirm destination aligned to `/sessions/:sessionId`
- Diagnosis -> plan now carries `goalId` and `diagnosisId`
- Session / TaskRun / Report routes verified against router real paths

## 7. Remaining API Risks

- Learning plan preview currently depends on `goalId` and `diagnosisId` coming from diagnosis flow; direct open of `/plan` without those query params will fail fast
- `task` standard is now plural at the API edge, but internal Java class names remain singular `TaskController`; this is naming-only debt, not contract debt
- Home module cards without an active session still point at placeholder-style routes from existing local data and are not converted into new features in this round
