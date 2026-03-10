# Domain Layer Refactor (Learning Workflow)

## Package Structure
- `domain/model`: entities and aggregate behavior (`Task`, `LearningSession`, `ConceptNode`, ...)
- `domain/enums`: shared business enums (`Stage`, `TaskStatus`, `ErrorTag`, `NextAction`)
- `domain/policy`: business policy contracts (`NextActionPolicy`, `TaskObjectiveTemplateStrategy`, `EvaluationRule`)
- `domain/service`: pure-domain strategy implementations (`ScoreBasedNextActionPolicy`, `DefaultTaskObjectiveTemplateStrategy`)
- `domain/repository`: repository ports

## Unified Enums
- `Stage`: `STRUCTURE`, `UNDERSTANDING`, `TRAINING`, `REFLECTION`
- `TaskStatus`: `PENDING`, `RUNNING`, `SUCCEEDED`, `FAILED`
- `ErrorTag`: `CONCEPT_CONFUSION`, `MISSING_STEPS`, `BOUNDARY_CASE`, `TERMINOLOGY`, `SHALLOW_REASONING`, `MEMORY_GAP`
- `NextAction`: `INSERT_REMEDIAL_UNDERSTANDING`, `INSERT_TRAINING_VARIANTS`, `INSERT_TRAINING_REINFORCEMENT`, `ADVANCE_TO_NEXT_NODE`, `NOOP`

## Task Domain Behavior Example
`Task` now owns lifecycle checks and state transitions:
- `canRun()`
- `canSubmit()`
- `markRunning()`
- `markSucceeded(output)`
- `markFailed(reason)`

This keeps workflow invariants close to the entity and reduces duplicated checks in application services.

## Strategy Interfaces (Domain)
- `TaskObjectiveTemplateStrategy`
- `EvaluationRule`
- `NextActionPolicy`

The interfaces live in domain and contain only business contracts.

## Domain vs Application Boundary
- Domain:
  - enums, entity behavior, policy contracts, pure rule implementation
  - no external IO dependencies
- Application:
  - orchestration/use cases (`submit`, `run`, `plan`)
  - coordinates repositories, serialization, transaction boundaries, API DTO mapping
  - calls domain policies/behaviors to enforce rules

## Notes
- Spring bean wiring for pure domain strategies is done in infrastructure config (`DomainPolicyConfig`), so domain classes stay framework-agnostic.
