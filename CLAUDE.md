# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**AI Learning Workflow Navigator** - An AI-powered learning guidance system that helps users set goals, diagnose their current state, create personalized learning plans, execute tasks, and get feedback for continuous improvement.

The core workflow is fixed:
```
目标输入 → 用户诊断 → 学习规划 → 任务执行 → 反馈与 Next Action
```

## Build & Run Commands

```bash
# Backend (Spring Boot, Java 17)
cd backend
mvn spring-boot:run    # Run development server on port 8080
mvn clean package       # Build JAR
mvn test                # Run all tests
mvn test -Dtest=XXXTest # Run single test class
```

## Architecture

### Backend Structure

```
backend/
├── src/main/java/navigator/
│   ├── api/              # Controllers, DTOs, GlobalResponse
│   ├── application/      # Application services, guards, task/scaffold/execution/session subpackages
│   ├── domain/           # Domain models, enums (e.g. LearningSessionStatusSupport)
│   └── infrastructure/   # InMemoryStore, JPA, Flyway
```

**Execution-related helpers (non-exhaustive):** `SessionReadFacade` merges in-memory plan status with persisted plans; `ExecutionSessionStateService` hydrates session state and resolves task blueprints; `TaskCompletionApplicationService` implements the `completeTask` pipeline; `ExternalIdSupport` parses prefixed string ids (`learn_session_1`, etc.) to numeric keys.

### Frontend types & API

- API base URL: `frontend/src/api/apiBaseUrl.ts` (shared by Axios and SSE); optional env override `VITE_API_BASE`.
- DTO types: `frontend/src/types/dto/*.ts` by domain, with `types/dto.ts` re-exporting for stable `@/types/dto` imports.

### Key Controllers

- **GoalController**: `POST /api/goals` - Goal creation
- **DiagnosisController**: `POST /api/diagnosis/sessions`, `POST /api/diagnosis/submissions` - User diagnosis
- **LearningPlanController**: `POST /api/learning-plans/preview`, `POST /api/learning-plans/commit` - Plan generation
- **TaskController**: `GET /api/sessions/{sessionId}/current-task`, `POST /api/tasks/{taskId}/complete` - Task execution
- **SessionController**: `GET /api/sessions/{sessionId}/report`, `POST /api/sessions/{sessionId}/next-action` - Feedback

### Core Domain Models

- `LearningGoalInput` / `StructuredLearningGoal` - Goal definition
- `LearnerProfileSnapshot` - User profile from diagnosis
- `LearningPlanPreview` - Generated learning plan
- `TaskBlueprint` / `TaskExecutionRecord` - Task structure and execution
- `NextActionDecision` - Feedback and next steps

## Design Principles

1. **Rules-driven, LLM-assisted**: Core decisions use rules; LLM provides explanations and text enhancement
2. **Structured input/output**: All major operations use structured data, not free-form text
3. **Decision/Display separation**: System decision objects vs. user-facing display objects
4. **Backward compatibility**: Maintain API contracts, make incremental changes

## Cursor Rules (Core Principles)

The project includes `.cursor/rules/` with project-specific guidelines. Key principles:

### Backend Architecture (`10-backend-architecture.mdc`)

- **Rules & LLM Separation**:
  - Rules system: candidate generation, risk judgment, enum decisions, state machine transitions
  - LLM: explanation enhancement, readability optimization, tutor phrasing generation, report summarization
  - Key business results must work without LLM

- **Domain Modeling**:
  - Prefer business semantics over page field aggregation
  - DTO, Domain, ViewModel semantic layering must be clear
  - Enums over free text
  - Same semantics should have only one primary expression

### Project Core (`00-project-core.mdc`)

- **Product Boundaries**:
  - Not a general-purpose AI Q&A tool
  - Not a free chat learning product
  - Focus on computer learning scenarios only

- **Core Objects** (do not reinvent):
  - `LearningGoalInput`, `StructuredLearningGoal`, `GoalContextSnapshot`
  - `LearnerProfileSnapshot`, `DiagnosisEvidenceSummary`
  - `LearningPlanPreview`, `TaskBlueprint`, `TaskExecutionRecord`
  - `ExecutionEvidenceSummary`, `LearningReport`, `NextActionDecision`

### Module Requirements

- **Diagnosis**: Context-aware state recognizer, outputs `LearnerProfileSnapshot`
- **Planning**: Constrained personalized decision maker, outputs executable tasks
- **Execution**: Learning behavior orchestration, not free chat
- **Feedback**: Result compilation + attribution + next step scheduling

## Important Notes

- Runtime uses `InMemoryStore` for fast session/task state; many flows also read/write JPA entities (H2 in tests). Guards and services may resolve via `SessionReadFacade` to avoid duplicating the two paths.
- Server runs on port 8080 by default
- CORS is configured for `/api` endpoints in `WebConfig.java`
- All API responses follow `GlobalResponse` format: `{ code, message, data }`
