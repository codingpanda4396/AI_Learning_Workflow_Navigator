# Flyway Migration Instructions

## Scope
- Apply to files in this migration directory only.

## Rules
- Use additive migrations by default.
- Keep migration naming and SQL style consistent with existing files.
- Avoid destructive data/schema operations unless explicitly requested.
- Keep statements deterministic and environment-safe.

## Output
- unified diff + migration file name + 1-line purpose.