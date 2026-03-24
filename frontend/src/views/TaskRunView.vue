<template>
  <PageContainer>
    <AppTopBar current="task" />
    <main class="mx-auto max-w-[1600px] px-4 py-6 md:px-6 lg:px-8">
      <LoadingState v-if="loading && !task" message="加载任务脚手架中..." />
      <ErrorState v-else-if="error" :message="error">
        <template #action>
          <SecondaryButton @click="fetchTask">重试</SecondaryButton>
        </template>
      </ErrorState>
      <EmptyState v-else-if="!task && !loading" message="暂无任务">
        <template #action>
          <SecondaryButton @click="router.push('/report')">查看报告</SecondaryButton>
        </template>
      </EmptyState>

      <section v-else-if="task" class="space-y-6">
        <header class="overflow-hidden rounded-[32px] border border-slate-200 bg-[radial-gradient(circle_at_top_left,_rgba(42,157,143,0.18),_transparent_36%),radial-gradient(circle_at_bottom_right,_rgba(245,158,11,0.16),_transparent_36%),linear-gradient(180deg,_rgba(248,250,252,1),_rgba(255,255,255,1))] p-6 shadow-card md:p-8">
          <div class="flex flex-wrap items-start justify-between gap-4">
            <div class="max-w-4xl">
              <p class="text-xs font-semibold uppercase tracking-[0.28em] text-primary">Execution Scaffold</p>
              <h1 class="mt-3 text-3xl font-bold tracking-tight text-slate-900 md:text-4xl">
                {{ task.title }}
              </h1>
              <p v-if="cognitiveHeadlineBody && !legacyComplete" class="mt-3 text-base text-slate-700">
                {{ cognitiveHeadlineBody }}
              </p>
              <p class="mt-4 max-w-3xl text-sm leading-6 text-slate-600 md:text-base">
                {{ currentActionLine }}
              </p>
            </div>
            <div class="flex flex-wrap items-center gap-2">
              <StatusBadge v-if="progress" :label="`任务 ${progress.currentIndex}/${progress.totalTasks}`" />
              <StatusBadge :label="taskStateLabel" :variant="taskStateVariant" />
              <StatusBadge v-if="guidancePhaseLabel" :label="guidancePhaseLabel" />
            </div>
          </div>
        </header>

        <div class="grid gap-6 xl:grid-cols-[280px,minmax(0,1fr),300px]">
          <ExecutionContextSidebar
            :title="task.title"
            :current-stage-label="currentStepLabel"
            :current-step-label="currentStepLabel"
            :task-state-label="taskStateLabel"
            :state-variant="taskStateVariant"
            :progress-text="progressText"
            :guidance-phase-label="guidancePhaseLabel"
            :guidance-title="guidanceTitle"
            :guidance-bullets="guidanceBullets"
            :steps="guidedSteps"
            :current-step-id="currentGuidedStepId"
            :metrics="sidebarMetrics"
          />

          <div class="space-y-5">
            <TaskRunMainColumn
              :task-goal="task.goal"
              :completion-criteria="task.completionCriteria ?? []"
              :goal-section-description="goalSectionDescription"
              :current-guided-step-id="currentGuidedStepId"
              :task-state="taskState"
              :template-pool="templatePool"
              :latest-assistant-reply="latestAssistantReply"
              :message-input="messageInput"
              :coach-input-disabled="coachInputDisabled"
              :sending="sending"
              :chat-turns="chatTurns"
              :action-labels="actionLabels"
              :explore-section-badge="exploreSectionBadge"
              :self-explain-description="selfExplainDescription"
              :self-explain-badge="selfExplainBadge"
              :self-explain-badge-variant="selfExplainBadgeVariant"
              :self-explain-input="selfExplainInput"
              :can-submit-self-explanation="canSubmitSelfExplanation"
              :submitting-self="submittingSelf"
              :self-explain-missing-points="selfExplainMissingPoints"
              @update:message-input="messageInput = $event"
              @update:self-explain-input="selfExplainInput = $event"
              @fill-message="messageInput = $event"
              @send-message="sendMessage"
              @submit-self-explanation="submitSelfExplanation"
            />

            <ScaffoldSectionCard
              id="guided-check"
              eyebrow="Checkpoint"
              title="微检查点"
              description="最后用一小题快速确认你已经能独立回答。"
              :active="taskState === 'CHECK'"
              :completed="taskState === 'PASS'"
              :badge="checkpointBadge"
              :badge-variant="checkpointBadgeVariant"
            >
              <div v-if="taskState === 'CHECK'" class="space-y-4">
                <div class="rounded-[22px] border border-border bg-slate-50/80 p-4">
                  <p class="text-sm font-semibold text-text-primary">当前检查问题</p>
                  <p class="mt-2 text-sm leading-6 text-text-primary">{{ checkpointQuestion || '检查问题生成中...' }}</p>
                </div>
                <textarea
                  v-model="checkpointAnswer"
                  rows="3"
                  class="w-full rounded-[18px] border border-border px-4 py-3 text-sm leading-6 text-text-primary focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
                  placeholder="用 1-2 句话说出你的答案。"
                />
                <PrimaryButton :loading="submittingCheckpoint" :disabled="!checkpointAnswer.trim()" @click="submitCheckpoint">
                  提交检查答案
                </PrimaryButton>
              </div>
              <p v-else class="text-sm leading-6 text-text-secondary">
                {{ taskState === 'PASS' ? '检查已通过，可以进入总结区。' : '先完成探索和自我解释，系统会把你送到这里。' }}
              </p>
            </ScaffoldSectionCard>

            <ScaffoldSectionCard
              id="guided-remedial"
              eyebrow="Remedial"
              title="补救区"
              description="没过的时候不重开新对话，而是沿着薄弱点继续修补。"
              :active="taskState === 'REMEDIAL'"
              :completed="taskState === 'PASS'"
              :badge="taskState === 'REMEDIAL' ? '回到薄弱点' : '按需触发'"
              :badge-variant="taskState === 'REMEDIAL' ? 'warning' : 'default'"
            >
              <div class="space-y-4">
                <p class="text-sm leading-6 text-text-secondary">{{ remedialDescription }}</p>
                <ul v-if="remedialHints.length" class="list-disc space-y-2 pl-5 text-sm leading-6 text-text-primary">
                  <li v-for="(hint, index) in remedialHints" :key="index">{{ hint }}</li>
                </ul>
                <p v-else class="text-sm leading-6 text-text-secondary">
                  暂无额外补救提示，可以继续使用右侧 Tutor 动作来收窄卡点。
                </p>
              </div>
            </ScaffoldSectionCard>

            <ScaffoldSectionCard
              id="guided-wrap"
              eyebrow="Summary"
              title="总结区"
              description="用一句话收束，把本次任务沉淀成可复用的学习资产。"
              :active="legacyComplete || taskState === 'PASS'"
              badge="可提交"
              :badge-variant="summaryUnlocked ? 'success' : 'warning'"
            >
              <div v-if="!summaryUnlocked" class="rounded-[20px] border border-amber-200 bg-amber-50/90 p-4 text-sm leading-6 text-amber-900">
                还没解锁总结区。先完成探索、自我解释和微检查，状态机进入 PASS 后再来收尾。
              </div>
              <div v-else class="space-y-4">
                <div>
                  <label class="mb-2 block text-sm font-medium text-text-primary">一句话说清你学会了什么</label>
                  <textarea v-model="closureSummary" rows="3" class="w-full rounded-[18px] border border-border px-4 py-3 text-sm leading-6 text-text-primary focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary" placeholder="例如：我已经能自己解释这一步为什么这么做。" />
                </div>
                <div class="grid gap-4 md:grid-cols-2">
                  <div>
                    <label class="mb-2 block text-sm font-medium text-text-primary">收获要点 1</label>
                    <input v-model="closurePoint1" type="text" class="w-full rounded-[18px] border border-border px-4 py-3 text-sm text-text-primary focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary" placeholder="关键概念 / 关键判断" />
                  </div>
                  <div>
                    <label class="mb-2 block text-sm font-medium text-text-primary">收获要点 2</label>
                    <input v-model="closurePoint2" type="text" class="w-full rounded-[18px] border border-border px-4 py-3 text-sm text-text-primary focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary" placeholder="常见误区 / 最小方法" />
                  </div>
                </div>
                <div>
                  <label class="mb-2 block text-sm font-medium text-text-primary">下一个练习动作（可选）</label>
                  <input v-model="closureNext" type="text" class="w-full rounded-[18px] border border-border px-4 py-3 text-sm text-text-primary focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary" placeholder="例如：再用一个相似例子自己过一遍。" />
                </div>
                <details class="rounded-[22px] border border-border bg-slate-50/80 p-4">
                  <summary class="cursor-pointer text-sm font-medium text-text-primary">更多完成选项</summary>
                  <div class="mt-4 space-y-4 border-t border-border pt-4">
                    <div>
                      <label class="mb-2 block text-xs font-medium uppercase tracking-[0.18em] text-text-secondary">完成状态</label>
                      <select v-model="completeForm.completionStatus" class="w-full rounded-[18px] border border-border bg-white px-4 py-3 text-sm text-text-primary focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary">
                        <option v-for="(label, value) in taskCompletionStatusLabels" :key="value" :value="value">{{ label }}</option>
                      </select>
                    </div>
                    <div>
                      <label class="mb-2 block text-xs font-medium uppercase tracking-[0.18em] text-text-secondary">补充反思</label>
                      <textarea v-model="completeForm.learnerReflection" rows="3" class="w-full rounded-[18px] border border-border bg-white px-4 py-3 text-sm leading-6 text-text-primary focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary" placeholder="记录这次任务对你最有帮助的做法。" />
                    </div>
                  </div>
                </details>
                <div class="flex flex-wrap items-center gap-3">
                  <PrimaryButton :loading="completing" @click="onComplete">进入下一步</PrimaryButton>
                  <p class="text-xs text-text-secondary">保持现有完成校验和 PASS 门禁，不绕开状态机。</p>
                </div>
              </div>
            </ScaffoldSectionCard>
          </div>

          <TutorActionPanel
            :guidance-phase-label="guidancePhaseLabel"
            :guidance-title="guidanceTitle"
            :guidance-bullets="guidanceBullets"
            :actions="tutorActions"
            :recommended-actions="recommendedUserActions"
            :disabled="coachInputDisabled"
            @select="applyTutorAction"
          />
        </div>

        <details v-if="task.completionCriteria?.length || (scaffold?.whyThisTask && !legacyComplete) || scaffold?.antiPatterns?.length || scaffold?.completionSignals?.length" class="rounded-[28px] border border-border bg-white shadow-card">
          <summary class="cursor-pointer px-5 py-4 text-sm font-medium text-text-primary">补充说明</summary>
          <div class="grid gap-5 border-t border-border px-5 py-5 md:grid-cols-3">
            <div v-if="scaffold?.whyThisTask && !legacyComplete">
              <p class="text-sm font-semibold text-text-primary">为什么是这一步</p>
              <p class="mt-2 text-sm leading-6 text-text-secondary">{{ scaffold.whyThisTask }}</p>
            </div>
            <div v-if="scaffold?.completionSignals?.length">
              <p class="text-sm font-semibold text-text-primary">达标信号</p>
              <ul class="mt-2 list-disc space-y-2 pl-5 text-sm leading-6 text-text-secondary">
                <li v-for="(item, index) in scaffold.completionSignals" :key="index">{{ item }}</li>
              </ul>
            </div>
            <div v-if="scaffold?.antiPatterns?.length">
              <p class="text-sm font-semibold text-text-primary">避免这些行为</p>
              <ul class="mt-2 list-disc space-y-2 pl-5 text-sm leading-6 text-text-secondary">
                <li v-for="(item, index) in scaffold.antiPatterns" :key="index">{{ item }}</li>
              </ul>
            </div>
          </div>
        </details>
      </section>
    </main>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppTopBar from '@/components/layout/AppTopBar.vue'
import PageContainer from '@/components/layout/PageContainer.vue'
import ExecutionContextSidebar from '@/components/task-run/ExecutionContextSidebar.vue'
import ScaffoldSectionCard from '@/components/task-run/ScaffoldSectionCard.vue'
import TaskRunMainColumn from '@/components/task-run/TaskRunMainColumn.vue'
import TutorActionPanel from '@/components/task-run/TutorActionPanel.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import LoadingState from '@/components/ui/LoadingState.vue'
import PrimaryButton from '@/components/ui/PrimaryButton.vue'
import SecondaryButton from '@/components/ui/SecondaryButton.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import { getErrorMessage } from '@/api/request'
import { completeTask, getCurrentTask, getCurrentTaskGuidance, getTaskScaffold, postCheckpoint, postSelfExplanation, postTaskMessage } from '@/api/task'
import { fallbackGuidanceForState, GUIDANCE_PHASE_LABELS, TASK_STATE_META, TUTOR_ACTIONS, tutorPromptFor } from '@/constants/taskRunUi'
import { showToast } from '@/stores/toast'
import { useWorkflowStore } from '@/stores/workflow'
import type { CurrentGuidanceBlock, CurrentTaskItem, ProgressItem, RecommendedUserActionItem, TaskScaffoldResponse } from '@/types/dto'
import { TaskCompletionStatus } from '@/types/enums'
import { taskCompletionStatusLabels } from '@/types/labels'
import { buildCompleteTaskPayload } from '@/utils/buildCompleteTaskPayload'
import { buildTaskGuidedSteps, getCurrentActionInstruction, getCurrentGuidedStepId, getTaskCognitiveHeadlineBody } from '@/utils/taskGuidedSteps'

const route = useRoute()
const router = useRouter()
const store = useWorkflowStore()
const loading = ref(true)
const completing = ref(false)
const sending = ref(false)
const submittingSelf = ref(false)
const submittingCheckpoint = ref(false)
const error = ref<string | null>(null)
const task = ref<CurrentTaskItem | null>(null)
const progress = ref<ProgressItem | null>(null)
const scaffold = ref<TaskScaffoldResponse | null>(null)
const taskState = ref('ORIENT')
const exploreRoundCount = ref(0)
const messageInput = ref('')
const selfExplainInput = ref('')
const checkpointAnswer = ref('')
const checkpointQuestion = ref('')
const legacyComplete = ref(false)
const taskStartedAt = ref(Date.now())
const canComplete = ref(false)
const closureSummary = ref('')
const closurePoint1 = ref('')
const closurePoint2 = ref('')
const closureNext = ref('')
const selfExplainMissingPoints = ref<string[]>([])
const guidancePhase = ref('')
const currentGuidance = ref<CurrentGuidanceBlock | null>(null)
const recommendedUserActions = ref<RecommendedUserActionItem[]>([])
const chatTurns = ref<{ role: 'USER' | 'ASSISTANT'; content: string; detectedAction?: string }[]>([])
const completeForm = ref({ completionStatus: TaskCompletionStatus.COMPLETED, learnerReflection: '' })
const actionLabels: Record<string, string> = { ASK_FOR_EXPLANATION: '解释概念', ASK_FOR_EXAMPLE: '最小例子', ASK_FOR_COMPARISON: '概念对比', ASK_FOR_SIMPLIFICATION: '简化说明', SELF_EXPLANATION: '自我表述', CONFUSION_SIGNAL: '表达困惑', SEEK_DIRECT_ANSWER: '直接要答案', OFF_TOPIC: '偏题', GENERIC: '一般提问', ANSWER_CHECK: '检查答案' }

const guidedSteps = computed(() => buildTaskGuidedSteps(legacyComplete.value, scaffold.value))
const cognitiveHeadlineBody = computed(() => (legacyComplete.value ? '' : getTaskCognitiveHeadlineBody(scaffold.value)))
const exploreUnitPrompts = computed(() => scaffold.value?.cognitiveUnits?.find((item) => item.unitId === 'explore')?.prompts ?? [])
const requiredScaffoldPrompts = computed(() => exploreUnitPrompts.value.filter((item) => item.required))
const optionalScaffoldPrompts = computed(() => exploreUnitPrompts.value.filter((item) => !item.required))
const fallbackQuickPrompts = computed(() => exploreUnitPrompts.value.length ? [] : [ ...(scaffold.value?.recommendedAskTemplates ?? []), ...(scaffold.value?.recommendedFollowupTemplates ?? []), ...((scaffold.value?.selfCheckTemplates ?? []).slice(0, 2)) ].filter(Boolean).slice(0, 6))
const templatePool = computed(() => [ ...requiredScaffoldPrompts.value.map((item) => ({ key: `required-${item.promptId ?? item.prompt}`, prompt: item.prompt, required: true })), ...optionalScaffoldPrompts.value.map((item) => ({ key: `optional-${item.promptId ?? item.prompt}`, prompt: item.prompt, required: false })), ...fallbackQuickPrompts.value.map((prompt, index) => ({ key: `fallback-${index}`, prompt, required: false })) ])
const currentGuidedStepId = computed(() => getCurrentGuidedStepId(taskState.value, exploreRoundCount.value, legacyComplete.value))
const currentStepLabel = computed(() => guidedSteps.value.find((step) => step.id === currentGuidedStepId.value)?.label ?? '推进任务')
const currentActionLine = computed(() => getCurrentActionInstruction(taskState.value, exploreRoundCount.value, scaffold.value, task.value?.goal ?? '', legacyComplete.value))
const taskStateLabel = computed(() => TASK_STATE_META[taskState.value]?.label ?? taskState.value)
const taskStateVariant = computed(() => TASK_STATE_META[taskState.value]?.variant ?? 'default')
const guidancePhaseLabel = computed(() => (guidancePhase.value ? GUIDANCE_PHASE_LABELS[guidancePhase.value] ?? guidancePhase.value : ''))
const guidanceTitle = computed(() => currentGuidance.value?.title ?? '')
const guidanceBullets = computed(() => currentGuidance.value?.bullets ?? [])
const progressText = computed(() => progress.value ? `当前是第 ${progress.value.currentIndex} / ${progress.value.totalTasks} 个任务` : '')
const latestAssistantReply = computed(() => [...chatTurns.value].reverse().find((turn) => turn.role === 'ASSISTANT')?.content ?? '')
const sidebarMetrics = computed(() => [{ label: '状态节点', value: taskState.value }, { label: '探索轮次', value: `${exploreRoundCount.value}` }, { label: '检查问题', value: checkpointQuestion.value ? '已生成' : '待生成' }, { label: '完成门禁', value: canComplete.value || taskState.value === 'PASS' ? '已打开' : '未打开' }])
const coachInputDisabled = computed(() => sending.value || taskState.value === 'CHECK' || taskState.value === 'PASS' || legacyComplete.value)
const canSubmitSelfExplanation = computed(() => !legacyComplete.value && ['EXPLORE', 'SELF_EXPLAIN', 'REMEDIAL'].includes(taskState.value) && exploreRoundCount.value >= 2)
const summaryUnlocked = computed(() => legacyComplete.value || taskState.value === 'PASS')
const goalSectionDescription = computed(() => legacyComplete.value ? '当前任务已切回简化完成模式，保留总结入口。' : '先看清这一步要学会什么，再决定如何向 Tutor 取帮助。')
const exploreSectionBadge = computed(() => taskState.value === 'REMEDIAL' ? '补救中' : chatTurns.value.length ? `${chatTurns.value.length} 条过程记录` : '从一条问题开始')
const selfExplainDescription = computed(() => canSubmitSelfExplanation.value ? '现在轮到你自己讲清楚。把它说顺，状态机才会继续往后走。' : '至少完成两轮探索后，这里才会成为主舞台。')
const selfExplainBadge = computed(() => taskState.value === 'CHECK' || taskState.value === 'PASS' ? '已完成' : canSubmitSelfExplanation.value ? '现在就讲' : '等待解锁')
const selfExplainBadgeVariant = computed(() => taskState.value === 'CHECK' || taskState.value === 'PASS' ? 'success' : canSubmitSelfExplanation.value ? 'warning' : 'default')
const checkpointBadge = computed(() => taskState.value === 'PASS' ? '已通过' : taskState.value === 'CHECK' ? '待回答' : '未开始')
const checkpointBadgeVariant = computed(() => taskState.value === 'PASS' ? 'success' : taskState.value === 'CHECK' ? 'warning' : 'default')
const remedialHints = computed(() => selfExplainMissingPoints.value.length ? selfExplainMissingPoints.value : scaffold.value?.fallbackHints ?? [])
const remedialDescription = computed(() => taskState.value === 'REMEDIAL' ? '系统判断还有薄弱点，先补一小段，再回到探索区或自我解释区继续推进。' : '补救区会在自我解释或微检查没有通过时出现。')
const tutorActions = TUTOR_ACTIONS

function isUiChatMessage(message: { role: 'USER' | 'ASSISTANT' | 'SYSTEM'; content: string; detectedAction?: string }): message is { role: 'USER' | 'ASSISTANT'; content: string; detectedAction?: string } { return message.role === 'USER' || message.role === 'ASSISTANT' }
function resetClosureFields() { closureSummary.value = ''; closurePoint1.value = ''; closurePoint2.value = ''; closureNext.value = ''; completeForm.value = { completionStatus: TaskCompletionStatus.COMPLETED, learnerReflection: '' } }
function applyFallbackGuidance() { const fallback = fallbackGuidanceForState(taskState.value, canSubmitSelfExplanation.value); guidancePhase.value = fallback.phase; currentGuidance.value = fallback.guidance; recommendedUserActions.value = fallback.actions }
async function loadGuidance() { if (!store.sessionId) return; try { const data = await getCurrentTaskGuidance(store.sessionId); guidancePhase.value = data.guidancePhase ?? ''; currentGuidance.value = data.currentGuidance ?? null; recommendedUserActions.value = data.recommendedUserActions ?? [] } catch { applyFallbackGuidance() } }
function syncRuntimeFromScaffold(data: TaskScaffoldResponse) { taskState.value = data.executionSnapshot?.currentState || data.currentExecutionState || 'ORIENT'; exploreRoundCount.value = data.executionSnapshot?.exploreTurnCount || 0; checkpointQuestion.value = data.executionSnapshot?.checkpointQuestion || ''; canComplete.value = data.executionSnapshot?.canComplete || false }

async function fetchTask() { if (!store.sessionId) return; loading.value = true; error.value = null; try { const data = await getCurrentTask(store.sessionId); store.currentTask = data.currentTask; store.progress = data.progress; task.value = data.currentTask; progress.value = data.progress; if (!data.currentTask) { store.currentTaskId = null; router.push('/report'); return } store.currentTaskId = data.currentTask.taskId; const routeTaskId = typeof route.params.taskId === 'string' ? route.params.taskId : ''; if (route.name === 'task' || (route.name === 'taskRun' && routeTaskId && routeTaskId !== data.currentTask.taskId)) router.replace({ name: 'taskRun', params: { taskId: data.currentTask.taskId } }); await loadScaffold(data.currentTask.taskId) } catch (err) { error.value = getErrorMessage(err) } finally { loading.value = false } }
async function loadScaffold(taskId: string) { if (!store.sessionId) return; resetClosureFields(); taskStartedAt.value = Date.now(); try { const data = await getTaskScaffold(taskId, store.sessionId); scaffold.value = data; legacyComplete.value = false; syncRuntimeFromScaffold(data); chatTurns.value = (data.recentMessages || []).filter(isUiChatMessage).map((message) => ({ role: message.role, content: message.content, detectedAction: message.detectedAction || undefined })); selfExplainInput.value = ''; checkpointAnswer.value = ''; selfExplainMissingPoints.value = []; await loadGuidance() } catch { scaffold.value = null; legacyComplete.value = true; taskState.value = 'PASS'; canComplete.value = true; applyFallbackGuidance(); showToast('任务脚手架暂不可用，已切换为简化完成模式。') } }
async function sendMessage() { if (!store.sessionId || !task.value || !messageInput.value.trim()) return; sending.value = true; const content = messageInput.value.trim(); messageInput.value = ''; try { const response = await postTaskMessage(task.value.taskId, store.sessionId, content); chatTurns.value.push({ role: 'USER', content, detectedAction: response.detectedAction }); chatTurns.value.push({ role: 'ASSISTANT', content: response.assistantReply }); taskState.value = response.taskState; if (response.taskState === 'EXPLORE') exploreRoundCount.value++; if (response.guidancePhase) guidancePhase.value = response.guidancePhase; if (response.recommendedUserActions) recommendedUserActions.value = response.recommendedUserActions; if (response.whetherCanComplete != null) canComplete.value = response.whetherCanComplete; await loadGuidance() } catch (err) { showToast(getErrorMessage(err)); messageInput.value = content } finally { sending.value = false } }
async function submitSelfExplanation() { if (!store.sessionId || !task.value) return; submittingSelf.value = true; try { const response = await postSelfExplanation(task.value.taskId, store.sessionId, selfExplainInput.value.trim()); taskState.value = response.taskState; if (response.checkpointQuestion) checkpointQuestion.value = response.checkpointQuestion; if (response.evaluation === 'WEAK') { showToast('还差一点，把提示里的缺口补上再试一次。'); selfExplainMissingPoints.value = response.missingPoints ?? [] } else { selfExplainMissingPoints.value = []; showToast('这段自我解释通过了，继续进入下一步。') } await loadGuidance() } catch (err) { showToast(getErrorMessage(err)) } finally { submittingSelf.value = false } }
async function submitCheckpoint() { if (!store.sessionId || !task.value) return; submittingCheckpoint.value = true; try { const response = await postCheckpoint(task.value.taskId, store.sessionId, checkpointAnswer.value.trim()); taskState.value = response.taskState; canComplete.value = response.taskState === 'PASS'; if (response.result === 'FAIL') { showToast(response.reason || '还差一点，回去补一小段再来。'); checkpointAnswer.value = '' } else { showToast('检查通过，可以进入总结区。') } await loadGuidance() } catch (err) { showToast(getErrorMessage(err)) } finally { submittingCheckpoint.value = false } }
async function applyTutorAction(actionId: string) { messageInput.value = tutorPromptFor(actionId, task.value?.title || scaffold.value?.learningObjective || '当前任务'); await nextTick(); document.getElementById('task-explore-input')?.scrollIntoView({ behavior: 'smooth', block: 'center' }) }
async function onComplete() { if (!store.sessionId || !task.value) return; if (!legacyComplete.value) { if (closureSummary.value.trim().length < 10) { showToast('请至少用 10 个字总结这次任务收获。'); return } if (!closurePoint1.value.trim() || !closurePoint2.value.trim()) { showToast('请填写两个收获要点。'); return } } completing.value = true; try { const payload = buildCompleteTaskPayload({ sessionId: store.sessionId, completionStatus: completeForm.value.completionStatus, legacyComplete: legacyComplete.value, summaryText: closureSummary.value, learnedPoint1: closurePoint1.value, learnedPoint2: closurePoint2.value, nextPracticeIntent: closureNext.value, learnerReflection: completeForm.value.learnerReflection, taskStartedAt: taskStartedAt.value, userMessageCount: chatTurns.value.filter((turn) => turn.role === 'USER').length }); const data = await completeTask(task.value.taskId, payload); store.currentTask = null; resetClosureFields(); if (data.nextTaskAvailable && data.nextTaskId) { store.currentTaskId = data.nextTaskId; await fetchTask() } else { router.push('/report') } } catch (err) { showToast(getErrorMessage(err)) } finally { completing.value = false } }

onMounted(() => { fetchTask() })
</script>
