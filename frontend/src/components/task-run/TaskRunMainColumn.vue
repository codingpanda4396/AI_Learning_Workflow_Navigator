<template>
  <div class="space-y-6">
    <section class="overflow-hidden rounded-[32px] border border-slate-200 bg-[radial-gradient(circle_at_top_right,_rgba(79,70,229,0.12),_transparent_36%),radial-gradient(circle_at_bottom_left,_rgba(16,185,129,0.10),_transparent_38%),linear-gradient(180deg,_rgba(255,255,255,0.98),_rgba(248,250,252,0.98))] p-6 shadow-card md:p-8">
      <div class="flex flex-wrap items-start justify-between gap-4">
        <div class="max-w-4xl">
          <p class="text-xs font-semibold uppercase tracking-[0.28em] text-primary">
            {{ stageTitle }} / {{ stageLabel }}
          </p>
          <h2 class="mt-3 text-3xl font-semibold tracking-tight text-slate-950">
            {{ primaryActionTitle }}
          </h2>
          <p class="mt-3 max-w-3xl text-base leading-7 text-slate-700">
            {{ stageObjective }}
          </p>
        </div>
        <StatusBadge :label="stageTitle" variant="warning" />
      </div>

      <div class="mt-6 grid gap-4 lg:grid-cols-[minmax(0,1.1fr),minmax(280px,0.9fr)]">
        <article class="rounded-[24px] border border-white/70 bg-white/80 p-5 backdrop-blur">
          <p class="text-xs font-semibold uppercase tracking-[0.18em] text-text-secondary">
            为什么现在停在这里
          </p>
          <p class="mt-3 text-sm leading-6 text-text-primary">
            {{ whyNow }}
          </p>
          <div class="mt-5 rounded-[20px] border border-primary/15 bg-primary/5 p-4">
            <p class="text-xs font-semibold uppercase tracking-[0.18em] text-primary">
              当前唯一主动作
            </p>
            <p class="mt-2 text-sm font-semibold text-slate-950">
              {{ primaryActionTitle }}
            </p>
            <p class="mt-2 text-sm leading-6 text-slate-700">
              {{ primaryActionDescription }}
            </p>
          </div>
        </article>

        <article class="rounded-[24px] border border-slate-200 bg-slate-50/88 p-5">
          <p class="text-xs font-semibold uppercase tracking-[0.18em] text-text-secondary">
            当前阶段规则
          </p>
          <div class="mt-4 space-y-4">
            <div>
              <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">
                阶段目标
              </p>
              <p class="mt-2 text-sm leading-6 text-text-primary">{{ stageDeliverable }}</p>
            </div>
            <div>
              <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">
                通过标准
              </p>
              <p class="mt-2 text-sm leading-6 text-text-primary">{{ passCondition }}</p>
            </div>
            <div>
              <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">
                Tutor 角色
              </p>
              <p class="mt-2 text-sm leading-6 text-text-primary">{{ tutorRole }}</p>
            </div>
            <div class="rounded-[18px] border border-emerald-200 bg-emerald-50/80 p-4">
              <p class="text-xs font-semibold uppercase tracking-[0.18em] text-emerald-700">
                系统摘要
              </p>
              <p class="mt-2 text-sm leading-6 text-emerald-950">
                {{ systemSummary }}
              </p>
            </div>
          </div>
        </article>
      </div>
    </section>

    <ScaffoldSectionCard
      v-if="showStructuredReply"
      id="guided-response"
      eyebrow="Scaffolded Action"
      :title="structuredReplyTitle"
      :description="structuredReplyDescription"
      :active="true"
      :badge="coachInputDisabled ? '当前锁定' : '等待你的输入'"
      :badge-variant="coachInputDisabled ? 'default' : 'warning'"
    >
      <div class="space-y-4">
        <div class="grid gap-4 md:grid-cols-3">
          <label class="block">
            <span class="text-sm font-semibold text-text-primary">我现在看到的结构</span>
            <textarea
              :value="structuredReply.understanding"
              rows="5"
              class="mt-2 w-full rounded-[18px] border border-border bg-white px-4 py-3 text-sm leading-6 text-text-primary focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
              :disabled="coachInputDisabled"
              :placeholder="understandingPlaceholder"
              @input="updateField('understanding', $event)"
            />
          </label>

          <label class="block">
            <span class="text-sm font-semibold text-text-primary">我还没讲清的点</span>
            <textarea
              :value="structuredReply.uncertainty"
              rows="5"
              class="mt-2 w-full rounded-[18px] border border-border bg-white px-4 py-3 text-sm leading-6 text-text-primary focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
              :disabled="coachInputDisabled"
              :placeholder="uncertaintyPlaceholder"
              @input="updateField('uncertainty', $event)"
            />
          </label>

          <label class="block">
            <span class="text-sm font-semibold text-text-primary">我希望系统怎么帮我</span>
            <textarea
              :value="structuredReply.confirmation"
              rows="5"
              class="mt-2 w-full rounded-[18px] border border-border bg-white px-4 py-3 text-sm leading-6 text-text-primary focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
              :disabled="coachInputDisabled"
              :placeholder="confirmationPlaceholder"
              @input="updateField('confirmation', $event)"
            />
          </label>
        </div>

        <div v-if="templatePool.length" class="rounded-[22px] border border-border bg-slate-50/80 p-4">
          <div class="flex flex-wrap items-center justify-between gap-3">
            <p class="text-sm font-semibold text-text-primary">可直接套用的脚手架句式</p>
            <p class="text-xs text-text-secondary">只保留对当前阶段最有帮助的最小提示</p>
          </div>
          <div class="mt-3 flex flex-wrap gap-2">
            <button
              v-for="item in templatePool"
              :key="item.key"
              type="button"
              class="rounded-full border px-3 py-2 text-left text-sm transition"
              :class="
                item.required
                  ? 'border-primary/35 bg-primary/5 text-primary hover:bg-primary/10'
                  : 'border-border bg-white text-text-primary hover:bg-slate-100'
              "
              :disabled="coachInputDisabled"
              @click="$emit('fill-message', item.prompt)"
            >
              {{ item.prompt }}
            </button>
          </div>
        </div>

        <div v-if="recommendedActions.length" class="rounded-[20px] border border-slate-200 bg-white p-4">
          <p class="text-sm font-semibold text-text-primary">系统建议你这样答</p>
          <div class="mt-3 flex flex-wrap gap-2">
            <StatusBadge
              v-for="action in recommendedActions"
              :key="action.code"
              :label="action.label"
            />
          </div>
        </div>

        <div class="flex flex-wrap items-center gap-3">
          <PrimaryButton
            :loading="sending"
            :disabled="!canSendStructuredMessage || coachInputDisabled"
            @click="$emit('send-message')"
          >
            提交这一步
          </PrimaryButton>
          <p class="text-xs text-text-secondary">
            系统会沿着当前阶段继续追问、最小提示或纠偏，不会切回开放式聊天。
          </p>
        </div>
      </div>
    </ScaffoldSectionCard>

    <ScaffoldSectionCard
      v-if="showTrainingSupport"
      id="guided-training"
      eyebrow="Training Check"
      title="把理解变成动作"
      :description="selfExplainDescription"
      :active="stageCode === 'TRAINING'"
      :badge="selfExplainBadge"
      :badge-variant="selfExplainBadgeVariant"
    >
      <div class="space-y-4">
        <textarea
          :value="selfExplainInput"
          rows="4"
          class="w-full rounded-[18px] border border-border px-4 py-3 text-sm leading-6 text-text-primary focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
          :disabled="!canSubmitSelfExplanation || submittingSelf"
          placeholder="先用自己的话解释机制，再说明你会如何把它用出来。"
          @input="onSelfExplainInput"
        />
        <div class="flex flex-wrap items-center gap-3">
          <PrimaryButton
            :loading="submittingSelf"
            :disabled="!selfExplainInput.trim() || !canSubmitSelfExplanation"
            @click="$emit('submit-self-explanation')"
          >
            提交训练结果
          </PrimaryButton>
          <p class="text-xs text-text-secondary">
            这里优先看你能不能独立讲清和独立完成，而不是复述系统的话。
          </p>
        </div>
        <div
          v-if="selfExplainMissingPoints.length"
          class="rounded-[20px] border border-amber-200 bg-amber-50/90 p-4"
        >
          <p class="text-sm font-semibold text-amber-900">还没补上的缺口</p>
          <ul class="mt-3 list-disc space-y-2 pl-5 text-sm leading-6 text-amber-900">
            <li v-for="(item, index) in selfExplainMissingPoints" :key="index">
              {{ item }}
            </li>
          </ul>
        </div>
      </div>
    </ScaffoldSectionCard>

    <ScaffoldSectionCard
      v-if="showReflectionCheck"
      id="guided-check"
      eyebrow="Reflection Check"
      title="先独立检查，再决定是否放行"
      description="反思阶段先看你能否脱离提示独立作答，只有通过才进入最终收束。"
      :active="taskState === 'CHECK'"
      :completed="taskState === 'PASS'"
      :badge="checkpointBadge"
      :badge-variant="checkpointBadgeVariant"
    >
      <div v-if="taskState === 'CHECK'" class="space-y-4">
        <div class="rounded-[22px] border border-border bg-slate-50/80 p-4">
          <p class="text-sm font-semibold text-text-primary">当前检查题</p>
          <p class="mt-2 text-sm leading-6 text-text-primary">
            {{ checkpointQuestion || '系统正在生成检查题...' }}
          </p>
        </div>
        <textarea
          :value="checkpointAnswer"
          rows="3"
          class="w-full rounded-[18px] border border-border px-4 py-3 text-sm leading-6 text-text-primary focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
          placeholder="用 1-2 句话回答这道检查题。"
          @input="onCheckpointInput"
        />
        <PrimaryButton
          :loading="submittingCheckpoint"
          :disabled="!checkpointAnswer.trim()"
          @click="$emit('submit-checkpoint')"
        >
          提交检查答案
        </PrimaryButton>
      </div>
      <p v-else class="text-sm leading-6 text-text-secondary">
        检查通过后，系统会让你把这轮学习收束成一份可复用的结论。
      </p>
    </ScaffoldSectionCard>

    <ScaffoldSectionCard
      v-if="showReflectionSummary"
      id="guided-wrap"
      eyebrow="Reflection Summary"
      title="收束这一轮学习"
      description="最后留下可复用的学习结果，而不是只显示任务完成。"
      :active="summaryUnlocked"
      badge="可提交"
      :badge-variant="summaryUnlocked ? 'success' : 'warning'"
    >
      <div class="space-y-4">
        <div>
          <label class="mb-2 block text-sm font-medium text-text-primary">一句话说清你学会了什么</label>
          <textarea
            :value="closureSummary"
            rows="3"
            class="w-full rounded-[18px] border border-border px-4 py-3 text-sm leading-6 text-text-primary focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
            placeholder="例如：我已经能说清这一机制为什么成立，以及最容易错在哪里。"
            @input="onClosureInput('summary', $event)"
          />
        </div>
        <div class="grid gap-4 md:grid-cols-2">
          <div>
            <label class="mb-2 block text-sm font-medium text-text-primary">收获要点 1</label>
            <input
              :value="closurePoint1"
              type="text"
              class="w-full rounded-[18px] border border-border px-4 py-3 text-sm text-text-primary focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
              placeholder="关键结构 / 判断依据"
              @input="onClosureInput('point1', $event)"
            />
          </div>
          <div>
            <label class="mb-2 block text-sm font-medium text-text-primary">收获要点 2</label>
            <input
              :value="closurePoint2"
              type="text"
              class="w-full rounded-[18px] border border-border px-4 py-3 text-sm text-text-primary focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
              placeholder="常见误区 / 最小方法"
              @input="onClosureInput('point2', $event)"
            />
          </div>
        </div>
        <div>
          <label class="mb-2 block text-sm font-medium text-text-primary">下一步练习动作（可选）</label>
          <input
            :value="closureNext"
            type="text"
            class="w-full rounded-[18px] border border-border px-4 py-3 text-sm text-text-primary focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
            placeholder="例如：换一个相近例子，再自己走一遍。"
            @input="onClosureInput('next', $event)"
          />
        </div>
        <details class="rounded-[22px] border border-border bg-slate-50/80 p-4">
          <summary class="cursor-pointer text-sm font-medium text-text-primary">更多完成选项</summary>
          <div class="mt-4 space-y-4 border-t border-border pt-4">
            <div>
              <label class="mb-2 block text-xs font-medium uppercase tracking-[0.18em] text-text-secondary">完成状态</label>
              <select
                :value="completionStatus"
                class="w-full rounded-[18px] border border-border bg-white px-4 py-3 text-sm text-text-primary focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
                @change="onCompletionStatusChange"
              >
                <option v-for="(label, value) in taskCompletionStatusLabels" :key="value" :value="value">
                  {{ label }}
                </option>
              </select>
            </div>
            <div>
              <label class="mb-2 block text-xs font-medium uppercase tracking-[0.18em] text-text-secondary">补充反思</label>
              <textarea
                :value="learnerReflection"
                rows="3"
                class="w-full rounded-[18px] border border-border bg-white px-4 py-3 text-sm leading-6 text-text-primary focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
                placeholder="记录这次任务里最有帮助的做法。"
                @input="onLearnerReflectionInput"
              />
            </div>
          </div>
        </details>
        <div class="flex flex-wrap items-center gap-3">
          <PrimaryButton :loading="completing" @click="$emit('complete')">进入下一步</PrimaryButton>
          <p class="text-xs text-text-secondary">
            这里仍然保留原有完成校验和 PASS 门槛，不绕开状态机。
          </p>
        </div>
      </div>
    </ScaffoldSectionCard>

    <details
      v-if="chatTurns.length || completionCriteria.length"
      class="rounded-[24px] border border-border bg-white p-5 shadow-card"
    >
      <summary class="cursor-pointer text-sm font-semibold text-text-primary">
        查看这一步的引导记录与检查标准
      </summary>
      <div class="mt-4 space-y-5 border-t border-border pt-4">
        <div v-if="completionCriteria.length">
          <p class="text-sm font-semibold text-text-primary">这一轮重点检查</p>
          <ul class="mt-3 list-disc space-y-2 pl-5 text-sm leading-6 text-text-secondary">
            <li v-for="(criterion, index) in completionCriteria" :key="index">
              {{ criterion }}
            </li>
          </ul>
        </div>

        <div v-if="chatTurns.length" class="space-y-3">
          <article
            v-for="(turn, index) in chatTurns"
            :key="index"
            class="rounded-[22px] border px-4 py-4"
            :class="
              turn.role === 'ASSISTANT'
                ? 'border-primary/20 bg-primary/5'
                : 'border-slate-200 bg-slate-50/90'
            "
          >
            <div class="flex flex-wrap items-center gap-2">
              <StatusBadge :label="turn.actionLabel" :variant="turn.actionVariant" />
              <p class="text-sm font-semibold text-text-primary">{{ turn.speaker }}</p>
            </div>
            <p class="mt-3 whitespace-pre-line text-sm leading-6 text-text-primary">
              {{ turn.content }}
            </p>
          </article>
        </div>
      </div>
    </details>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { RecommendedUserActionItem } from '@/types/dto'
import PrimaryButton from '@/components/ui/PrimaryButton.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import ScaffoldSectionCard from '@/components/task-run/ScaffoldSectionCard.vue'
import { taskCompletionStatusLabels } from '@/types/labels'
import type { TaskCompletionStatusType } from '@/types/enums'
import type { PlanStageCode } from '@/utils/planPresentationModel'

interface TemplateItem {
  key: string
  prompt: string
  required: boolean
}

interface StructuredReplyDraft {
  understanding: string
  uncertainty: string
  confirmation: string
}

interface DisplayChatTurn {
  role: 'USER' | 'ASSISTANT'
  speaker: string
  content: string
  actionLabel: string
  actionVariant: 'default' | 'success' | 'warning' | 'error'
}

const props = defineProps<{
  stageCode: PlanStageCode
  stageTitle: string
  stageLabel: string
  stageObjective: string
  stageDeliverable: string
  tutorRole: string
  whyNow: string
  passCondition: string
  systemSummary: string
  primaryActionTitle: string
  primaryActionDescription: string
  taskState: string
  completionCriteria: string[]
  templatePool: TemplateItem[]
  structuredReply: StructuredReplyDraft
  canSendStructuredMessage: boolean
  coachInputDisabled: boolean
  sending: boolean
  chatTurns: DisplayChatTurn[]
  recommendedActions: RecommendedUserActionItem[]
  selfExplainDescription: string
  selfExplainBadge: string
  selfExplainBadgeVariant: 'default' | 'success' | 'warning' | 'error'
  selfExplainInput: string
  canSubmitSelfExplanation: boolean
  submittingSelf: boolean
  selfExplainMissingPoints: string[]
  checkpointBadge: string
  checkpointBadgeVariant: 'default' | 'success' | 'warning' | 'error'
  checkpointQuestion: string
  checkpointAnswer: string
  submittingCheckpoint: boolean
  summaryUnlocked: boolean
  closureSummary: string
  closurePoint1: string
  closurePoint2: string
  closureNext: string
  learnerReflection: string
  completionStatus: TaskCompletionStatusType
  completing: boolean
}>()

const emit = defineEmits<{
  'update:structured-reply': [value: StructuredReplyDraft]
  'update:self-explain-input': [value: string]
  'update:checkpoint-answer': [value: string]
  'update:closure-summary': [value: string]
  'update:closure-point1': [value: string]
  'update:closure-point2': [value: string]
  'update:closure-next': [value: string]
  'update:learner-reflection': [value: string]
  'update:completion-status': [value: TaskCompletionStatusType]
  'fill-message': [prompt: string]
  'send-message': []
  'submit-self-explanation': []
  'submit-checkpoint': []
  complete: []
}>()

const showStructuredReply = computed(() => props.stageCode !== 'REFLECTION')
const showTrainingSupport = computed(
  () => props.stageCode === 'TRAINING' || props.selfExplainMissingPoints.length > 0
)
const showReflectionCheck = computed(
  () => props.stageCode === 'REFLECTION' && props.taskState === 'CHECK'
)
const showReflectionSummary = computed(
  () => props.stageCode === 'REFLECTION' && props.summaryUnlocked
)

const structuredReplyTitle = computed(() => {
  if (props.stageCode === 'STRUCTURE') return '先搭出你的最小结构图'
  if (props.stageCode === 'UNDERSTANDING') return '先讲清关键机制和因果'
  return '先完成这一步的最小训练动作'
})

const structuredReplyDescription = computed(() => {
  if (props.stageCode === 'STRUCTURE') {
    return '不要直接展开细节，先说明它是什么、在整体里哪里、和什么最相关。'
  }
  if (props.stageCode === 'UNDERSTANDING') {
    return '不要只贴定义，先说清它为什么这样工作、最容易和什么混淆。'
  }
  return '训练阶段先让你自己动手，再根据你的表现给最小提示或纠偏。'
})

const understandingPlaceholder = computed(() => {
  if (props.stageCode === 'STRUCTURE') {
    return '例如：它属于哪一层、解决什么问题、和前后概念怎么接上。'
  }
  if (props.stageCode === 'UNDERSTANDING') {
    return '例如：关键步骤是怎样发生的，前后因果怎么串起来。'
  }
  return '例如：我会先怎么做，为什么这样做。'
})

const uncertaintyPlaceholder = computed(() => {
  if (props.stageCode === 'STRUCTURE') {
    return '例如：我还不清楚它和相邻概念的边界。'
  }
  if (props.stageCode === 'UNDERSTANDING') {
    return '例如：我还不明白为什么这里会这样变化。'
  }
  return '例如：我会做第一步，但不确定后面怎么判断。'
})

const confirmationPlaceholder = computed(() => {
  if (props.stageCode === 'STRUCTURE') {
    return '例如：请帮我补一个最关键的关系，不要展开太多。'
  }
  if (props.stageCode === 'UNDERSTANDING') {
    return '例如：请给我一个最小例子，只验证这条机制。'
  }
  return '例如：请只给一个最小提示，让我自己走完。'
})

function updateField(field: keyof StructuredReplyDraft, event: Event) {
  emit('update:structured-reply', {
    ...props.structuredReply,
    [field]: (event.target as HTMLTextAreaElement).value,
  })
}

function onSelfExplainInput(event: Event) {
  emit('update:self-explain-input', (event.target as HTMLTextAreaElement).value)
}

function onCheckpointInput(event: Event) {
  emit('update:checkpoint-answer', (event.target as HTMLTextAreaElement).value)
}

function onClosureInput(
  key: 'summary' | 'point1' | 'point2' | 'next',
  event: Event
) {
  const target = event.target as HTMLInputElement | HTMLTextAreaElement
  if (key === 'summary') emit('update:closure-summary', target.value)
  if (key === 'point1') emit('update:closure-point1', target.value)
  if (key === 'point2') emit('update:closure-point2', target.value)
  if (key === 'next') emit('update:closure-next', target.value)
}

function onLearnerReflectionInput(event: Event) {
  emit('update:learner-reflection', (event.target as HTMLTextAreaElement).value)
}

function onCompletionStatusChange(event: Event) {
  emit('update:completion-status', (event.target as HTMLSelectElement).value as TaskCompletionStatusType)
}
</script>
