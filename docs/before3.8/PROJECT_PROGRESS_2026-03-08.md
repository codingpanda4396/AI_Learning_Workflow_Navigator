# AI Learning Workflow Navigator - Project Progress (2026-03-08)

## 1. Current Delivery Status

### Backend

- Authentication and user system delivered:
  - `POST /api/auth/register`
  - `POST /api/auth/login`
  - `GET /api/users/me`
- JWT interceptor is enabled for protected APIs:
  - `Authorization: Bearer <token>` required for `/api/**` (with explicit whitelist).
- User domain and persistence have been added:
  - `app_user` table and repositories/services.
- Session history capability delivered:
  - `GET /api/session/history` (pagination)
  - `GET /api/session/{sessionId}` (detail)
  - `POST /api/session/{sessionId}/resume` (resume and touch activity)

### Frontend

- Authentication flow is fully wired:
  - auth page (register/login)
  - token storage and bootstrap
  - axios auto-inject bearer token
  - route guard (guest-only and auth-required routes)
  - logout flow
- Historical records are visible in UI:
  - dedicated history page `/history`
  - quick entry from home/session pages
  - resume action to continue a past session
- Home page has been simplified:
  - removed duplicated “goal diagnose/path generate” actions
  - now creates session and enters workflow directly
  - shows latest 5 history items in the left panel
- Encoding issue fixed:
  - major view-layer garbled text has been cleaned and normalized to UTF-8.

## 2. Workflow Progress UX (Latest)

- Step progress has entered phase-1 implementation:
  - progress bar now shows `done/total`, percentage and status text.
  - status set includes: `未开始 / 进行中 / 已完成 / 受阻`.
- In `SessionView`, progress is now computed by **business step semantics**:
  - Step1: goal diagnosis available
  - Step2: path selected
  - Step3: tasks completed ratio from timeline
  - Step4: final completion by overall completion rate

## 3. Validation

- Frontend build passes:
  - `npm run build` (frontend)
- No blocking compile errors in current delivered frontend branch.

## 4. Known Gaps / Next Priority

1. Progress semantics still need product polish:
   - user feedback indicates current representation can still be confusing in some scenarios.
2. Additional progress UX refinement (phase-2) is recommended:
   - clearer step entry/exit conditions in UI copy
   - stronger visual distinction between “current step” and “overall completion”
   - optional tooltip/help text for status rules.
3. Backend/Frontend docs can be consolidated:
   - align older summary docs with the now-delivered auth/history/progress features.

## 5. Affected Core Files (This Stage)

- Backend
  - `backend/src/main/java/com/pandanav/learning/auth/*`
  - `backend/src/main/java/com/pandanav/learning/infrastructure/config/AuthWebMvcConfig.java`
  - `backend/src/main/java/com/pandanav/learning/api/controller/SessionHistoryController.java`
  - `backend/src/main/resources/db/migration/V11__add_user_and_session_history.sql`
- Frontend
  - `frontend/src/views/AuthView.vue`
  - `frontend/src/views/HomeView.vue`
  - `frontend/src/views/SessionView.vue`
  - `frontend/src/views/HistoryView.vue`
  - `frontend/src/components/StepProgress.vue`
  - `frontend/src/api/auth.ts`
  - `frontend/src/api/session.ts`
  - `frontend/src/stores/auth.ts`
  - `frontend/src/router/index.ts`
