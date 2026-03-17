import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import type {
  StructuredLearningGoal,
  GoalContextSnapshot,
  LearnerProfileSnapshot,
  DiagnosisEvidenceSummary,
  PlanPreviewData,
  CurrentTaskItem,
  ProgressItem,
  LearningReport,
  NextActionDecision,
} from '@/types/dto'

const STORAGE_KEYS = {
  goalId: 'workflow_goalId',
  diagnosisId: 'workflow_diagnosisId',
  sessionId: 'workflow_sessionId',
  planId: 'workflow_planId',
} as const

function getStored(key: string): string | null {
  try {
    return sessionStorage.getItem(key)
  } catch {
    return null
  }
}

function setStored(key: string, value: string | null) {
  if (value) {
    sessionStorage.setItem(key, value)
  } else {
    sessionStorage.removeItem(key)
  }
}

export const useWorkflowStore = defineStore('workflow', () => {
  const goalId = ref<string | null>(getStored(STORAGE_KEYS.goalId))
  const structuredGoal = ref<StructuredLearningGoal | null>(null)
  const goalContextSnapshot = ref<GoalContextSnapshot | null>(null)

  const diagnosisId = ref<string | null>(getStored(STORAGE_KEYS.diagnosisId))
  const learnerProfileSnapshot = ref<LearnerProfileSnapshot | null>(null)
  const diagnosisEvidenceSummary = ref<DiagnosisEvidenceSummary | null>(null)

  const planId = ref<string | null>(getStored(STORAGE_KEYS.planId))
  const planPreview = ref<PlanPreviewData | null>(null)

  const sessionId = ref<string | null>(getStored(STORAGE_KEYS.sessionId))
  const currentTaskId = ref<string | null>(null)
  const taskSequence = ref<string[]>([])
  const currentTask = ref<CurrentTaskItem | null>(null)
  const progress = ref<ProgressItem | null>(null)

  const report = ref<LearningReport | null>(null)
  const nextActionDecision = ref<NextActionDecision | null>(null)

  watch(goalId, (v) => setStored(STORAGE_KEYS.goalId, v))
  watch(diagnosisId, (v) => setStored(STORAGE_KEYS.diagnosisId, v))
  watch(sessionId, (v) => setStored(STORAGE_KEYS.sessionId, v))
  watch(planId, (v) => setStored(STORAGE_KEYS.planId, v))

  function reset() {
    goalId.value = null
    structuredGoal.value = null
    goalContextSnapshot.value = null
    diagnosisId.value = null
    learnerProfileSnapshot.value = null
    diagnosisEvidenceSummary.value = null
    planId.value = null
    planPreview.value = null
    sessionId.value = null
    currentTaskId.value = null
    taskSequence.value = []
    currentTask.value = null
    progress.value = null
    report.value = null
    nextActionDecision.value = null
  }

  return {
    goalId,
    structuredGoal,
    goalContextSnapshot,
    diagnosisId,
    learnerProfileSnapshot,
    diagnosisEvidenceSummary,
    planId,
    planPreview,
    sessionId,
    currentTaskId,
    taskSequence,
    currentTask,
    progress,
    report,
    nextActionDecision,
    reset,
  }
})
