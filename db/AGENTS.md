# Database Instructions

## Rules
- Use Flyway migrations for all schema changes.
- Keep naming consistent with existing tables and indexes.
- Prefer additive migrations over destructive changes.
- Do not modify unrelated schema objects.

## Output
- Return unified diff for touched migration files only.
- Mention migration id and purpose in 1-2 lines.