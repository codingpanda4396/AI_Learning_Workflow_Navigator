<template>
  <div class="space-y-5">
    <ScaffoldSectionCard
      id="guided-orient"
      eyebrow="Task Goal"
      title="任务目标"
      :description="goalSectionDescription"
      :active="currentGuidedStepId === 'orient'"
      :completed="currentGuidedStepId !== 'orient'"
      badge="当前目标"
    >
      <div class="space-y-4">
        <p class="text-base leading-7 text-text-primary">{{ taskGoal }}</p>
        <div
          v-if="completionCriteria.length"
          class="rounded-[22px] border border-border bg-slate-50/80 p-4"
        >
          <p class="text-sm font-semibold text-text-primary">完成信号</p>
          <ul class="mt-3 list-disc space-y-2 pl-5 text-sm leading-6 text-text-secondary">
            <li v-for="(criterion, index) in completionCriteria" :key="index">
              {{ criterion }}
            </li>
          </ul>
        </div>
      </div>
    </ScaffoldSectionCard>

    <ScaffoldSectionCard
      id="guided-templates"
      eyebrow="Recommended Ask"
      title="推荐提问模板"
      description="先选一句最接近当前卡点的话，再进入探索。"
      :active="taskState === 'ORIENT' || taskState === 'EXPLORE'"
      :completed="templatePool.length > 0 && taskState !== 'ORIENT' && taskState !== 'EXPLORE'"
      :badge="templatePool.length ? `${templatePool.length} 条可用` : '等待输入'"
    >
      <div v-if="templatePool.length" class="flex flex-wrap gap-2">
        <button
          v-for="item in templatePool"
          :key="item.key"
          type="button"
          class="rounded-full border px-3 py-2 text-left text-sm transition"
          :class="item.required ? 'border-primary/35 bg-primary/5 text-primary hover:bg-primary/10' : 'border-border bg-white text-text-primary hover:bg-slate-50'"
          @click="$emit('fill-message', item.prompt)"
        >
          {{ item.prompt }}
        </button>
      </div>
      <p v-else class="text-sm leading-6 text-text-secondary">
        暂无推荐模板，直接在下面的探索区说明你的理解或卡点即可。
      </p>
    </ScaffoldSectionCard>

    <ScaffoldSectionCard
      id="guided-explore"
      eyebrow="Explore"
      title="探索区"
      description="这里是任务推进区，不是自由聊天区。把你的理解、疑问或当前思路放进来。"
      :active="taskState === 'ORIENT' || taskState === 'EXPLORE' || taskState === 'REMEDIAL'"
      :completed="chatTurns.length > 0 && (taskState === 'CHECK' || taskState === 'PASS')"
      :badge="exploreSectionBadge"
      :badge-variant="taskState === 'REMEDIAL' ? 'warning' : 'default'"
    >
      <div class="space-y-4">
        <div
          v-if="latestAssistantReply"
          class="rounded-[22px] border border-primary/20 bg-primary/5 p-4"
        >
          <p class="text-xs font-semibold uppercase tracking-[0.22em] text-primary">
            Latest Tutor Push
          </p>
          <p class="mt-2 whitespace-pre-line text-sm leading-6 text-text-primary">
            {{ latestAssistantReply }}
          </p>
        </div>

        <div class="rounded-[22px] border border-border bg-slate-50/80 p-4">
          <label for="task-explore-input" class="text-sm font-semibold text-text-primary">
            当前任务输入
          </label>
          <textarea
            id="task-explore-input"
            :value="messageInput"
            rows="4"
            class="mt-3 w-full rounded-[18px] border border-border bg-white px-4 py-3 text-sm leading-6 text-text-primary focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
            :disabled="coachInputDisabled"
            placeholder="例如：我现在的理解是… / 我卡在… / 请给我一个最小例子。"
            @input="onMessageInput"
          />
          <div class="mt-3 flex flex-wrap items-center gap-3">
            <PrimaryButton
              :loading="sending"
              :disabled="!messageInput.trim() || coachInputDisabled"
              @click="$emit('send-message')"
            >
              推进当前任务
            </PrimaryButton>
            <p class="text-xs text-text-secondary">
              目标是让任务继续往前，不是积累对话历史。
            </p>
          </div>
        </div>

        <details class="rounded-[22px] border border-border bg-white/90">
          <summary class="cursor-pointer px-4 py-3 text-sm font-medium text-text-primary">
            过程记录 / 对话
          </summary>
          <div class="border-t border-border px-4 py-4">
            <div v-if="chatTurns.length" class="space-y-3">
              <div
                v-for="(turn, index) in chatTurns"
                :key="index"
                class="rounded-[18px] px-4 py-3 text-sm leading-6"
                :class="turn.role === 'USER' ? 'bg-slate-100 text-slate-900' : 'border border-primary/15 bg-primary/5 text-text-primary'"
              >
                <div class="flex items-center justify-between gap-3">
                  <p class="font-semibold">{{ turn.role === 'USER' ? '我' : 'AI Tutor' }}</p>
                  <span
                    v-if="turn.detectedAction && turn.role === 'USER'"
                    class="text-xs text-text-secondary"
                  >
                    {{ actionLabels[turn.detectedAction] ?? turn.detectedAction }}
                  </span>
                </div>
                <p class="mt-2 whitespace-pre-line">{{ turn.content }}</p>
              </div>
            </div>
            <p v-else class="text-sm text-text-secondary">
              还没有过程记录，先从上面的推荐模板或 Tutor 动作开始。
            </p>
          </div>
        </details>
      </div>
    </ScaffoldSectionCard>

    <ScaffoldSectionCard
      id="guided-explain"
      eyebrow="Self Explain"
      title="自我解释区"
      :description="selfExplainDescription"
      :active="currentGuidedStepId === 'explain'"
      :completed="taskState === 'CHECK' || taskState === 'PASS'"
      :badge="selfExplainBadge"
      :badge-variant="selfExplainBadgeVariant"
    >
      <div class="space-y-4">
        <p class="text-sm leading-6 text-text-secondary">
          用你自己的话讲清楚当前任务在做什么、为什么这样做，以及关键判断点。
        </p>
        <textarea
          :value="selfExplainInput"
          rows="4"
          class="w-full rounded-[18px] border border-border px-4 py-3 text-sm leading-6 text-text-primary focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
          :disabled="!canSubmitSelfExplanation || submittingSelf"
          placeholder="我现在的理解是：……"
          @input="onSelfExplainInput"
        />
        <div class="flex flex-wrap items-center gap-3">
          <PrimaryButton
            :loading="submittingSelf"
            :disabled="!selfExplainInput.trim() || !canSubmitSelfExplanation"
            @click="$emit('submit-self-explanation')"
          >
            提交自我解释
          </PrimaryButton>
          <p class="text-xs text-text-secondary">
            目标不是背标准答案，而是证明你已经能自己说清楚。
          </p>
        </div>
        <div
          v-if="selfExplainMissingPoints.length"
          class="rounded-[20px] border border-amber-200 bg-amber-50/90 p-4"
        >
          <p class="text-sm font-semibold text-amber-900">还需要补的点</p>
          <ul class="mt-3 list-disc space-y-2 pl-5 text-sm leading-6 text-amber-900">
            <li v-for="(item, index) in selfExplainMissingPoints" :key="index">
              {{ item }}
            </li>
          </ul>
        </div>
      </div>
    </ScaffoldSectionCard>
  </div>
</template>

<script setup lang="ts">
import PrimaryButton from '@/components/ui/PrimaryButton.vue'
import ScaffoldSectionCard from '@/components/task-run/ScaffoldSectionCard.vue'

interface TemplateItem {
  key: string
  prompt: string
  required: boolean
}

interface ChatTurn {
  role: 'USER' | 'ASSISTANT'
  content: string
  detectedAction?: string
}

const props = defineProps<{
  taskGoal: string
  completionCriteria: string[]
  goalSectionDescription: string
  currentGuidedStepId: string
  taskState: string
  templatePool: TemplateItem[]
  latestAssistantReply: string
  messageInput: string
  coachInputDisabled: boolean
  sending: boolean
  chatTurns: ChatTurn[]
  actionLabels: Record<string, string>
  exploreSectionBadge: string
  selfExplainDescription: string
  selfExplainBadge: string
  selfExplainBadgeVariant: 'default' | 'success' | 'warning' | 'error'
  selfExplainInput: string
  canSubmitSelfExplanation: boolean
  submittingSelf: boolean
  selfExplainMissingPoints: string[]
}>()

const emit = defineEmits<{
  'update:message-input': [value: string]
  'update:self-explain-input': [value: string]
  'fill-message': [prompt: string]
  'send-message': []
  'submit-self-explanation': []
}>()

function onMessageInput(event: Event) {
  emit('update:message-input', (event.target as HTMLTextAreaElement).value)
}

function onSelfExplainInput(event: Event) {
  emit('update:self-explain-input', (event.target as HTMLTextAreaElement).value)
}
</script>
