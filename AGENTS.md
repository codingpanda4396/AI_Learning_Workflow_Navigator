# Panda Project Instructions

## Objective
- Prioritize minimal token usage and fast delivery.
- Make only scoped, local edits.

## General
- Prefer minimal, local edits over broad refactors.
- Reuse existing repository/service/controller patterns.
- Search existing code before introducing new abstractions.
- Do not modify unrelated files.
- Keep naming and domain terminology consistent with existing code.

## Output
- Default output: unified diff.
- Mention only touched files.
- Keep explanations brief unless explicitly requested.
- Do not restate user requirements.
- Do not paste large code blocks unless explicitly requested.
- Unless the user says "Õ¹¿ª", keep explanations within 5 lines.

## Execution
- Implement directly; do not provide long proposals first.
- Stop once acceptance criteria are met.
- If blocked, provide only blocker + smallest next action.

## Verify
- Backend: `mvn -q -DskipTests compile`
- Frontend: `pnpm build`