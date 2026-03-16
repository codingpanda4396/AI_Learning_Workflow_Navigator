import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useLearningFlowStore = defineStore('learningFlow', () => {
  // Key IDs
  const goalId = ref(null)
  const diagnosisId = ref(null)
  const planId = ref(null)
  const sessionId = ref(null)

  // Domain snapshots
  const structuredGoal = ref(null)
  const goalContextSnapshot = ref(null)
  const learnerProfileSnapshot = ref(null)
  const diagnosisEvidenceSummary = ref(null)
  const planPreview = ref(null)
  const currentTask = ref(null)
  const report = ref(null)
  const nextAction = ref(null)

  // Session progress - 初始值为 0，等待 getCurrentTask 从后端获取
  const currentTaskIndex = ref(0)
  const totalTasks = ref(0)
  const taskSequence = ref([])

  // Setters
  function setGoalResult(data) {
    goalId.value = data.goalId
    structuredGoal.value = data.structuredGoal || null
    goalContextSnapshot.value = data.goalContextSnapshot || null
  }

  function setDiagnosisResult(data) {
    diagnosisId.value = data.diagnosisId
    learnerProfileSnapshot.value = data.learnerProfileSnapshot || null
    diagnosisEvidenceSummary.value = data.diagnosisEvidenceSummary || null
  }

  function setPlanPreview(data) {
    planId.value = data.planId
    planPreview.value = data
    taskSequence.value = data.taskSequence || []
  }

  function setCommittedSession(data) {
    sessionId.value = data.sessionId
    planId.value = data.planId
    taskSequence.value = data.taskSequence || []
    // 不在这里设置 currentTaskIndex 和 totalTasks
    // 等待 loadCurrentTask 从后端获取最新值
    totalTasks.value = data.taskSequence?.length || 0
  }

  function setCurrentTask(data) {
    currentTask.value = data.currentTask
    // 始终使用后端返回的 progress 数据
    if (data.progress) {
      currentTaskIndex.value = data.progress.currentIndex ?? 0
      totalTasks.value = data.progress.totalTasks ?? 0
    }
  }

  function setReport(data) {
    report.value = data
  }

  function setNextAction(data) {
    nextAction.value = data
  }

  function advanceToNextTask() {
    currentTaskIndex.value++
  }

  function resetAll() {
    goalId.value = null
    diagnosisId.value = null
    planId.value = null
    sessionId.value = null
    structuredGoal.value = null
    goalContextSnapshot.value = null
    learnerProfileSnapshot.value = null
    diagnosisEvidenceSummary.value = null
    planPreview.value = null
    currentTask.value = null
    report.value = null
    nextAction.value = null
    currentTaskIndex.value = 0
    totalTasks.value = 0
    taskSequence.value = []
  }

  // Computed
  const hasGoal = computed(() => !!goalId.value)
  const hasDiagnosis = computed(() => !!diagnosisId.value)
  const hasPlan = computed(() => !!planId.value)
  const hasSession = computed(() => !!sessionId.value)
  const isLastTask = computed(() => currentTaskIndex.value >= totalTasks.value - 1)

  return {
    // State
    goalId,
    diagnosisId,
    planId,
    sessionId,
    structuredGoal,
    goalContextSnapshot,
    learnerProfileSnapshot,
    diagnosisEvidenceSummary,
    planPreview,
    currentTask,
    report,
    nextAction,
    currentTaskIndex,
    totalTasks,
    taskSequence,
    // Setters
    setGoalResult,
    setDiagnosisResult,
    setPlanPreview,
    setCommittedSession,
    setCurrentTask,
    setReport,
    setNextAction,
    advanceToNextTask,
    resetAll,
    // Computed
    hasGoal,
    hasDiagnosis,
    hasPlan,
    hasSession,
    isLastTask
  }
})
