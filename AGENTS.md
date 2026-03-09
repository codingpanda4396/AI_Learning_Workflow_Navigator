# Panda Project Instructions

## General
- Prefer minimal, local edits over broad refactors
- Reuse existing repository/service/controller patterns
- Search existing code before introducing new abstractions
- Do not modify unrelated files

## Backend
- Stack: Spring Boot 3, JDK 17, MyBatis-Plus, MySQL
- Keep API response style consistent with existing controllers
- Do not introduce broad try/catch or silent fallback
- Preserve domain terminology already used in code

## Database
- Use Flyway migrations for schema changes
- Keep naming consistent with existing tables and indexes
- Prefer additive migrations over destructive changes

## Frontend
- Stack: Vue 3 + TypeScript
- Keep API field names aligned with backend contracts
- Do not redesign unrelated pages during feature edits

## Output
- Default output: unified diff
- Mention only touched files
- Keep explanations brief unless explicitly requested

## Verify
- Backend: mvn -q -DskipTests compile
- Frontend: pnpm build