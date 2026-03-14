# Frontend diagnosis and learning-plan contract notes

## Diagnosis generate

Request:

```json
{
  "sessionId": "88"
}
```

Response:

```json
{
  "diagnosisId": "101",
  "sessionId": "88",
  "status": "GENERATED",
  "questions": [
    {
      "questionId": "foundation",
      "dimension": "FOUNDATION",
      "type": "SINGLE_CHOICE",
      "required": true,
      "title": "How solid is your current foundation?",
      "options": [
        { "code": "BEGINNER", "label": "Beginner", "order": 1 },
        { "code": "BASIC", "label": "Basic", "order": 2 }
      ]
    }
  ],
  "nextAction": {
    "code": "PATH_PLAN",
    "label": "Open plan preview",
    "target": {
      "route": "/plan",
      "params": {
        "sessionId": 88,
        "diagnosisId": "101"
      }
    }
  },
  "fallback": {
    "applied": false,
    "reasons": [],
    "contentSource": { "code": "LLM", "label": "LLM" }
  },
  "metadata": {
    "questionCount": 1
  }
}
```

Frontend mapping:

- Render question options with `option.label`.
- Submit stable business values with `option.code`.
- Read title/description/placeholder directly from flattened fields, not from nested `copy`.

## Diagnosis submit

Request:

```json
{
  "answers": [
    {
      "questionId": "foundation",
      "selectedOptionCode": "BEGINNER"
    }
  ]
}
```

Response:

```json
{
  "diagnosisId": "101",
  "sessionId": "88",
  "status": "SUBMITTED",
  "capabilityProfile": {
    "currentLevel": { "code": "BEGINNER", "label": "Beginner" },
    "strengths": ["Keeps a stable rhythm"],
    "weaknesses": ["Needs concept framing before practice"],
    "learningPreference": { "code": "CONCEPT_FIRST", "label": "Concept first" },
    "timeBudget": { "code": "STANDARD", "label": "4-6 hours / week" },
    "goalOrientation": { "code": "PROJECT", "label": "Project delivery" }
  },
  "insights": {
    "summary": "You need prerequisite framing before practice.",
    "planExplanation": "The plan should start from the core prerequisite cluster."
  },
  "nextAction": {
    "code": "PATH_PLAN",
    "label": "Open plan preview",
    "target": {
      "route": "/plan",
      "params": {
        "sessionId": 88,
        "diagnosisId": "101"
      }
    }
  },
  "fallback": {
    "applied": true,
    "reasons": ["PROFILE_SUMMARY_TIMEOUT"],
    "contentSource": { "code": "RULE_TEMPLATE", "label": "Rule template" }
  },
  "metadata": {
    "answerCount": 1,
    "profileVersion": 2
  }
}
```

Frontend mapping:

- `capabilityProfile` is rendered directly from the new top-level object.
- `nextAction.target.route + params` drives navigation to plan preview.
- `fallback.applied / reasons / contentSource` are cached in store and shown with one unified UI slot.

## Learning plan preview

Request:

```json
{
  "diagnosisId": "101",
  "sessionId": 88,
  "goalText": "Build a working chapter-level study plan",
  "courseName": "Learning Architecture",
  "chapterName": "Unified Diagnosis Flow",
  "adjustments": {
    "intensity": "STANDARD",
    "learningMode": "LEARN_THEN_PRACTICE",
    "prioritizeFoundation": true
  }
}
```

Response:

```json
{
  "id": "preview-501",
  "status": { "code": "PREVIEW_READY", "label": "Preview ready" },
  "previewOnly": true,
  "committed": false,
  "planSource": { "code": "RULE_ENGINE", "label": "Rule engine" },
  "contentSource": { "code": "LLM", "label": "LLM" },
  "fallbackApplied": false,
  "fallbackReasons": [],
  "focuses": ["Stabilize prerequisite concepts"],
  "summary": {
    "headline": "Start from the prerequisite concept cluster first",
    "recommendedStartNode": {
      "id": "node-1",
      "nodeKey": "node-1",
      "displayName": "Prerequisite cluster",
      "nodeName": "Prerequisite cluster"
    },
    "recommendedPace": { "code": "STANDARD", "label": "Standard" },
    "estimatedTotalMinutes": 180,
    "estimatedNodeCount": 3,
    "estimatedStageCount": 4
  },
  "pathPreview": [
    {
      "node": {
        "id": "node-1",
        "nodeKey": "node-1",
        "displayName": "Prerequisite cluster",
        "nodeName": "Prerequisite cluster"
      },
      "difficulty": { "code": "FOUNDATION", "label": "Foundation" },
      "mastery": 32,
      "status": { "code": "WEAK", "label": "Weak" },
      "isRecommendedStart": true,
      "estimatedNodeMinutes": 45,
      "reasonTag": "Prerequisite gap"
    }
  ],
  "taskPreview": [
    {
      "stage": { "code": "STRUCTURE", "label": "Structure" },
      "title": "Frame the chapter",
      "learningGoal": "Understand the map before drilling details",
      "learnerAction": "Review the guided concept map",
      "aiSupport": "Generate a concise scaffold and checkpoints",
      "estimatedTaskMinutes": 25
    }
  ],
  "metadata": {
    "schemaVersion": "2026-03-14",
    "persistedPreview": true,
    "estimatedTotalMinutesScope": "path_preview_total",
    "estimatedNodeMinutesScope": "per_path_node",
    "estimatedTaskMinutesScope": "per_stage_task"
  }
}
```

Frontend mapping:

- Normalize preview id into `preview.id` and display `status.label` plus explicit `previewOnly/committed`.
- Prefer `displayName`, then `nodeName`, for all node titles.
- Show timing scopes from `metadata` so preview durations are not ambiguous.

## Local validation

- Mock samples are in `frontend/src/api/contractMocks.ts`.
- Build verification: `pnpm build`
- Manual flow: generate diagnosis -> submit diagnosis -> open plan preview -> regenerate preview -> confirm plan
