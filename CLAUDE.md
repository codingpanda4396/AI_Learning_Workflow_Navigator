# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AI Learning Workflow Navigator is a full-stack application with:
- **Backend**: Spring Boot 3.4.3 + Java 17 + PostgreSQL
- **Frontend**: Vue 3 + TypeScript + Pinia + Vite

The system manages learning sessions with adaptive task planning, tracking user mastery across concept nodes.

## Tech Stack

### Backend
- Spring Boot 3.4.3
- Java 17
- PostgreSQL with Flyway migrations
- Maven

### Frontend
- Vue 3 (Composition API + `<script setup>`)
- TypeScript
- Pinia (state management)
- Vue Router 4
- Axios
- Vite

## Commands

### Backend

```bash
# Run locally (requires PostgreSQL)
cd backend && mvn spring-boot:run

# Run all tests
mvn clean test

# Run single test class
mvn test -Dtest=SessionControllerTest

# Run single test method
mvn test -Dtest=SessionControllerTest#testCreateSession

# Build JAR (skip tests)
mvn clean package -DskipTests

# Docker build (run from repository root)
docker build -t ai-learning-backend .
```

### Frontend

```bash
# Install dependencies
cd frontend && npm install

# Run dev server (with API proxy to localhost:8080)
cd frontend && npm run dev

# Build for production
cd frontend && npm run build

# Preview production build
cd frontend && npm run preview
```

## Git Workflow

- **Branches**: `main` (production), `dev` (development)
- **CI triggers**: Push to `dev` or any PR triggers Backend CI workflow
- **CD triggers**: Push to `main` triggers Backend CD workflow (deploys to ECS)
- **Commit format**: `<type>: <description>` (types: feat, fix, refactor, docs, test, chore)

## Architecture

### Backend (Domain-Driven Design)

```
backend/src/main/java/com/pandanav/learning/
├── api/
│   ├── controller/    # REST endpoints (SessionController, TaskController)
│   └── dto/           # Request/Response DTOs
├── application/
│   ├── service/       # Application services
│   ├── usecase/       # Use case implementations
│   ├── command/       # Command objects
│   └── query/         # Query objects
├── domain/
│   ├── model/         # Domain entities (LearningSession, Task, Mastery, ConceptNode)
│   ├── enums/         # Stage, TaskStatus, NextAction, ErrorTag
│   ├── repository/    # Repository interfaces
│   ├── policy/        # Policy interfaces (NextActionPolicy, EvaluationRule)
│   └── service/       # Domain services
└── infrastructure/
    ├── persistence/   # JDBC repository implementations
    ├── exception/     # Exception handling (GlobalExceptionHandler)
    └── config/        # Configuration
```

### Frontend

```
frontend/src/
├── api/           # Axios API client
├── components/   # Reusable components (LoadingSpinner, ErrorMessage, SkeletonBox)
├── router/       # Vue Router configuration
├── stores/       # Pinia stores (session store)
├── types/        # TypeScript interfaces matching API DTOs
└── views/        # Page components (HomeView, SessionView, TaskRunView, TaskSubmitView)
```

## Database

- Migrations in `backend/src/main/resources/db/migration/`
- Environment variables: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- Local config: Copy `application-local.yml.example` to `application-local.yml`

## API Endpoints

### Session API
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/session/create` | Create new learning session |
| POST | `/api/session/{id}/plan` | Plan tasks for session |
| POST | `/api/session/{id}/start` | Start session |
| GET | `/api/session/{id}` | Get session details |
| GET | `/api/session/{id}/overview` | Get session overview with mastery summary |
| GET | `/api/session/{id}/next` | Get next recommended action |

### Task API
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/task/{id}/run` | Run/execute a task |
| POST | `/api/task/{id}/submit` | Submit task answer for evaluation |

### System
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/actuator/health` | Health check |
| GET | `/swagger-ui.html` | API documentation |

## Key Concepts

- **Stages**: STRUCTURE → UNDERSTANDING → TRAINING → REFLECTION
- **Task Status**: PENDING, RUNNING, SUCCEEDED, FAILED
- **Next Actions**: INSERT_REMEDIAL_UNDERSTANDING, INSERT_TRAINING_VARIANTS, INSERT_TRAINING_REINFORCEMENT, ADVANCE_TO_NEXT_NODE, NOOP

## Testing

- Backend tests: `backend/src/test/java/` - Run with `mvn clean test`
- Frontend: Manual testing via `npm run dev`
