<template>
  <div class="space-y-6">
    <section
      class="overflow-hidden rounded-[32px] border border-slate-200 bg-[radial-gradient(circle_at_top_right,_rgba(79,70,229,0.10),_transparent_38%),linear-gradient(180deg,_rgba(255,255,255,0.98),_rgba(248,250,252,0.98))] p-5 shadow-card md:p-7"
    >
      <div class="grid gap-5 md:grid-cols-3">
        <div class="rounded-[22px] border border-primary/20 bg-white/95 p-4 md:col-span-1">
          <p class="text-sm font-bold text-slate-950">现在先做这一件事</p>
          <p class="mt-2 text-base font-semibold leading-snug text-slate-900">
            {{ scaffoldDoNow }}
          </p>
        </div>
        <div class="rounded-[22px] border border-slate-200 bg-white/95 p-4 md:col-span-1">
          <p class="text-sm font-bold text-slate-950">这样开始最容易</p>
          <ul class="mt-2 space-y-1.5 text-sm font-medium leading-snug text-slate-800">
            <li v-for="(line, idx) in scaffoldStarterPhrases" :key="idx">· {{ line }}</li>
          </ul>
        </div>
        <div class="rounded-[22px] border border-emerald-200/80 bg-emerald-50/60 p-4 md:col-span-1">
          <p class="text-sm font-bold text-emerald-950">写到这一步就可以继续</p>
          <ul class="mt-2 list-disc space-y-1 pl-4 text-sm leading-snug text-emerald-950">
            <li v-for="(line, idx) in scaffoldPassBullets" :key="idx">{{ line }}</li>
          </ul>
        </div>
      </div>
    </section>

    <ScaffoldSectionCard
      v-if="showStructuredReply"
      id="guided-response"
      eyebrow="先动手写"
      :title="structuredReplyTitle"
      :description="structuredReplyDescription"
      :active="true"
      emphasis
      :badge="coachInputDisabled ? '暂不可写' : '写在这里'"
      :badge-variant="coachInputDisabled ? 'default' : 'warning'"
    >
      <div class="space-y-4">
        <div class="grid gap-4 md:grid-cols-3">
          <label class="block">
            <span class="text-sm font-semibold text-text-primary">我现在的理解</span>
            <textarea
              :value="structuredReply.understanding"
              rows="5"
              class="mt-2 w-full rounded-[18px] border border-border bg-white px-4 py-3 text-sm leading-6 text-text-primary focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/25"
              :disabled="coachInputDisabled"
              :placeholder="understandingPlaceholder"
              @input="updateField('understanding', $event)"
            />
          </label>

          <label class="block">
            <span class="text-sm font-semibold text-text-primary">我还不确定</span>
            <textarea
              :value="structuredReply.uncertainty"
              rows="5"
              class="mt-2 w-full rounded-[18px] border border-border bg-white px-4 py-3 text-sm leading-6 text-text-primary focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/25"
              :disabled="coachInputDisabled"
              :placeholder="uncertaintyPlaceholder"
              @input="updateField('uncertainty', $event)"
            />
          </label>

          <label class="block">
            <span class="text-sm font-semibold text-text-primary">我想先确认</span>
            <textarea
              :value="structuredReply.confirmation"
              rows="5"
              class="mt-2 w-full rounded-[18px] border border-border bg-white px-4 py-3 text-sm leading-6 text-text-primary focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/25"
              :disabled="coachInputDisabled"
              :placeholder="confirmationPlaceholder"
              @input="updateField('confirmation', $event)"
            />
          </label>
        </div>

        <div v-if="templatePool.length" class="rounded-[22px] border border-border bg-slate-50/80 p-4">
          <p class="text-sm font-semibold text-text-primary">点一句，填进上面某一栏</p>
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

        <div v-if="recommendedActionsLimited.length" class="rounded-[20px] border border-slate-200 bg-white p-4">
          <p class="text-sm font-semibold text-text-primary">也可以这样问</p>
          <div class="mt-3 flex flex-wrap gap-2">
            <StatusBadge
              v-for="action in recommendedActionsLimited"
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
        </div>
      </div>
    </ScaffoldSectionCard>

    <ScaffoldSectionCard
      v-if="showTrainingSupport"
      id="guided-training"
      eyebrow="自己讲一遍"
      title="把理解落成一段话"
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
          placeholder="用你自己的话写清「是什么、为什么」，三四句即可。"
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
      eyebrow="独立检查"
      title="不看提示，答这一题"
      description="用你自己的话简短作答即可。"
      :active="taskState === 'CHECK'"
      :completed="taskState === 'PASS'"
      :badge="checkpointBadge"
      :badge-variant="checkpointBadgeVariant"
    >
      <div v-if="taskState === 'CHECK'" class="space-y-4">
        <div class="rounded-[22px] border border-border bg-slate-50/80 p-4">
          <p class="text-sm font-semibold text-text-primary">题目</p>
          <p class="mt-2 text-sm leading-6 text-text-primary">
            {{ checkpointQuestion || '题目加载中…' }}
          </p>
        </div>
        <textarea
          :value="checkpointAnswer"
          rows="3"
          class="w-full rounded-[18px] border border-border px-4 py-3 text-sm leading-6 text-text-primary focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
          placeholder="一两句话写清你的判断或步骤。"
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
      <p v-else class="text-sm leading-6 text-text-secondary">通过后到下面写一句总结即可继续。</p>
    </ScaffoldSectionCard>

    <ScaffoldSectionCard
      v-if="showReflectionSummary"
      id="guided-wrap"
      eyebrow="收尾"
      title="收束这一轮学习"
      description="留一句结论和两个要点，方便下次接着练。"
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
        </div>
      </div>
    </ScaffoldSectionCard>

    <details
      v-if="chatTurns.length || completionCriteria.length"
      class="rounded-[24px] border border-border bg-white p-5 shadow-card"
    >
      <summary class="cursor-pointer text-sm font-semibold text-text-primary">引导记录与检查标准</summary>
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
  scaffoldDoNow: string
  scaffoldStarterPhrases: string[]
  scaffoldPassBullets: string[]
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

const recommendedActionsLimited = computed(() => props.recommendedActions.slice(0, 3))

const structuredReplyTitle = computed(() => {
  if (props.stageCode === 'STRUCTURE') return '先写出最小理解'
  if (props.stageCode === 'UNDERSTANDING') return '把机制说清楚'
  return '按脚手架做一小步'
})

const structuredReplyDescription = computed(() => {
  if (props.stageCode === 'STRUCTURE') {
    return '先说清它是什么、在解决什么问题，不必写得很完整。'
  }
  if (props.stageCode === 'UNDERSTANDING') {
    return '用因果把「为什么这样」串起来，点出你最不确定的一处。'
  }
  return '写你怎么做、为什么这样做；需要提示就写在第三栏。'
})

const understandingPlaceholder = computed(() => {
  if (props.stageCode === 'STRUCTURE') {
    return '它是什么、解决什么问题：我写「……是在处理……」'
  }
  if (props.stageCode === 'UNDERSTANDING') {
    return '关键步骤：先……再……所以结果是……'
  }
  return '我会先做：……（一步具体动作）'
})

const uncertaintyPlaceholder = computed(() => {
  if (props.stageCode === 'STRUCTURE') {
    return '和哪个概念最容易混、边界在哪：我写「我还不确定……」'
  }
  if (props.stageCode === 'UNDERSTANDING') {
    return '哪一步的「为什么」我还想不通：……'
  }
  return '做到哪一步我会卡住：……'
})

const confirmationPlaceholder = computed(() => {
  if (props.stageCode === 'STRUCTURE') {
    return '只想确认一件事：例如「它和……是什么关系？」'
  }
  if (props.stageCode === 'UNDERSTANDING') {
    return '想要一个最小例子或一句纠偏：例如「请用……举例」'
  }
  return '只要一句提示：例如「下一步我只该想……」'
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
