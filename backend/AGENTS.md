# Backend Instructions

## Stack
- Spring Boot 3, JDK 17, MyBatis-Plus, MySQL

## Rules
- Keep API response style consistent with existing controllers.
- Preserve existing service/repository/controller layering.
- Do not introduce broad try/catch or silent fallback.
- Preserve domain terminology already used in code.
- Prefer small, local changes over architecture changes.

## Database
- Use Flyway migrations for schema changes.
- Keep naming consistent with existing tables and indexes.
- Prefer additive migrations over destructive changes.

## Output
- Return unified diff for touched backend files only.
- Include compile result only; avoid long narrative.