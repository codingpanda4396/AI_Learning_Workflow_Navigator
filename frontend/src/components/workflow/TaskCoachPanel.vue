<template>
  <div class="rounded-card border border-border bg-white p-4 md:p-5">
    <div class="flex items-center justify-between gap-2">
      <p class="text-sm font-medium text-text-primary">{{ title }}</p>
      <button
        v-if="collapsible"
        type="button"
        class="text-xs text-primary hover:underline"
        @click="expanded = !expanded"
      >
        {{ expanded ? '收起' : '展开' }}
      </button>
    </div>
    <p class="mt-1 text-xs text-text-secondary">
      {{ subtitle }}
    </p>

    <div v-show="!collapsible || expanded" class="mt-4">
      <div
        class="mb-4 max-h-60 space-y-3 overflow-y-auto rounded border border-border bg-gray-50/80 p-3"
      >
        <div
          v-for="(m, idx) in chatTurns"
          :key="idx"
          class="text-sm"
          :class="
            m.role === 'USER' ? 'text-right text-text-primary' : 'text-left text-text-secondary'
          "
        >
          <span
            class="inline-block max-w-[90%] rounded-lg px-3 py-2"
            :class="
              m.role === 'USER' ? 'bg-primary/15' : 'bg-white shadow-sm ring-1 ring-border'
            "
          >
            {{ m.content }}
          </span>
          <div
            v-if="showDetectedAction && m.detectedAction && m.role === 'USER'"
            class="mt-0.5 text-xs text-gray-500"
          >
            识别：{{ actionLabel(m.detectedAction) }}
          </div>
        </div>
      </div>
      <div class="flex gap-2">
        <textarea
          :value="messageInput"
          rows="2"
          class="min-h-[3rem] flex-1 rounded-input border border-border px-3 py-2 text-text-primary focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
          :placeholder="placeholder"
          :disabled="inputDisabled"
          @input="onInput"
        />
        <PrimaryButton
          :loading="sending"
          :disabled="!messageInput.trim() || inputDisabled"
          class="self-end"
          @click="$emit('send')"
        >
          发送
        </PrimaryButton>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import PrimaryButton from '@/components/ui/PrimaryButton.vue'

export interface CoachTurn {
  role: 'USER' | 'ASSISTANT'
  content: string
  detectedAction?: string
}

const props = withDefaults(
  defineProps<{
    title?: string
    subtitle?: string
    chatTurns: CoachTurn[]
    messageInput: string
    sending?: boolean
    inputDisabled?: boolean
    placeholder?: string
    collapsible?: boolean
    actionLabels?: Record<string, string>
    /** 是否展示用户消息下方的「识别：…」（偏调试向，默认关闭） */
    showDetectedAction?: boolean
  }>(),
  {
    title: '和导师一起想一想',
    subtitle: '你可以先说说现在的理解，我帮你一起梳理。',
    sending: false,
    inputDisabled: false,
    placeholder: '说说你的理解，或告诉我卡在哪里',
    collapsible: false,
    actionLabels: () => ({}),
    showDetectedAction: false,
  }
)

const emit = defineEmits<{
  'update:messageInput': [v: string]
  send: []
}>()

const expanded = ref(!props.collapsible)

const defaultActionLabels: Record<string, string> = {
  ASK_FOR_EXPLANATION: '求解释',
  ASK_FOR_EXAMPLE: '求举例',
  ASK_FOR_COMPARISON: '求对比',
  ASK_FOR_SIMPLIFICATION: '求简化',
  SELF_EXPLANATION: '自己复述',
  CONFUSION_SIGNAL: '表达困惑',
  SEEK_DIRECT_ANSWER: '直接要答案',
  OFF_TOPIC: '跑题',
  GENERIC: '一般',
  ANSWER_CHECK: '检查作答',
}

const mergedActionLabels = computed(() => ({
  ...defaultActionLabels,
  ...props.actionLabels,
}))

function onInput(e: Event) {
  emit('update:messageInput', (e.target as HTMLTextAreaElement).value)
}

function actionLabel(code: string) {
  return mergedActionLabels.value[code] ?? code
}
</script>
