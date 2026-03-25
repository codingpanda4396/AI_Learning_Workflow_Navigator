<template>
  <section data-testid="execution-driving-seat" class="space-y-8">
    <div>
      <h2 class="text-lg font-semibold tracking-tight text-slate-950">先选一种开始方式</h2>
      <p class="mt-1 text-sm leading-6 text-slate-600">
        不用自己想第一句话，点一下就会把这段话发给导师。
      </p>
      <div class="mt-5 grid gap-4 md:grid-cols-3">
        <article
          v-for="card in cards"
          :key="card.id"
          class="flex flex-col rounded-[24px] border-2 border-slate-200 bg-[linear-gradient(180deg,_#fff,_#f8fafc)] p-5 shadow-sm transition hover:border-primary/45 hover:shadow-md"
        >
          <h3 class="text-base font-semibold text-slate-950">{{ card.title }}</h3>
          <p class="mt-2 flex-1 text-sm leading-6 text-slate-600">{{ card.hint }}</p>
          <PrimaryButton
            class="mt-5 w-full"
            :disabled="sending"
            :loading="false"
            @click="$emit('send-card', card.prompt)"
          >
            用这个开始
          </PrimaryButton>
        </article>
      </div>
    </div>

    <div
      class="rounded-[28px] border border-slate-200 bg-[linear-gradient(180deg,_rgba(255,255,255,1),_rgba(248,250,252,0.96))] p-5 shadow-card md:p-7"
    >
      <h2 class="text-lg font-semibold text-slate-950">和导师接着聊</h2>
      <p class="mt-1 text-sm text-slate-600">对话记录在下面，输入框始终可用。</p>

      <div
        v-if="chatTurns.length"
        class="mt-5 max-h-72 space-y-3 overflow-y-auto rounded-[20px] border border-slate-100 bg-slate-50/50 p-4"
      >
        <div
          v-for="(t, i) in chatTurns"
          :key="i"
          class="rounded-[18px] border px-4 py-3"
          :class="
            t.role === 'ASSISTANT'
              ? 'border-primary/20 bg-primary/5'
              : 'border-slate-200 bg-white'
          "
        >
          <p class="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">
            {{ t.role === 'USER' ? '你' : '导师' }}
          </p>
          <p class="mt-2 whitespace-pre-wrap text-sm leading-7 text-slate-800">
            {{ t.content }}
          </p>
        </div>
      </div>
      <p v-else class="mt-5 rounded-[20px] border border-dashed border-slate-200 bg-slate-50/80 px-4 py-8 text-center text-sm text-slate-500">
        还没有消息。先点上面一张卡片，或直接在下面输入。
      </p>

      <label class="mt-5 block">
        <span class="text-sm font-semibold text-slate-950">{{ inputLabel }}</span>
        <textarea
          data-testid="driving-seat-input"
          :value="draftValue"
          rows="4"
          class="mt-2 w-full rounded-[22px] border border-slate-200 bg-white px-4 py-3 text-sm leading-7 text-slate-900 shadow-sm outline-none transition focus:border-primary focus:ring-4 focus:ring-primary/10"
          :placeholder="inputPlaceholder"
          :disabled="sending"
          @input="$emit('update:draftValue', ($event.target as HTMLTextAreaElement).value)"
        />
      </label>
      <div class="mt-4 flex flex-wrap items-center gap-3">
        <PrimaryButton
          data-testid="driving-seat-send"
          :loading="sending"
          :disabled="!canSubmitChat"
          @click="$emit('submit-chat')"
        >
          {{ primaryActionLabel }}
        </PrimaryButton>
      </div>
    </div>

    <div
      v-if="showRestate"
      class="rounded-[28px] border border-amber-200/80 bg-amber-50/35 p-5 shadow-sm md:p-7"
    >
      <h2 class="text-lg font-semibold text-slate-950">现在轮到你：用自己的话说一遍</h2>
      <p class="mt-2 text-sm leading-6 text-slate-600">
        分开写三行就够。不需要一次说对，先把你的理解写出来。
      </p>
      <div class="mt-5 space-y-4">
        <label class="block">
          <span class="text-sm font-medium text-slate-800">它是什么</span>
          <input
            :value="restateWhat"
            type="text"
            class="mt-2 w-full rounded-[18px] border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-900 outline-none focus:border-primary focus:ring-2 focus:ring-primary/15"
            placeholder="一句话定位"
            @input="$emit('update:restateWhat', ($event.target as HTMLInputElement).value)"
          />
        </label>
        <label class="block">
          <span class="text-sm font-medium text-slate-800">它解决什么问题</span>
          <input
            :value="restateProblem"
            type="text"
            class="mt-2 w-full rounded-[18px] border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-900 outline-none focus:border-primary focus:ring-2 focus:ring-primary/15"
            placeholder="它在帮什么忙"
            @input="$emit('update:restateProblem', ($event.target as HTMLInputElement).value)"
          />
        </label>
        <label class="block">
          <span class="text-sm font-medium text-slate-800">它和谁有关</span>
          <input
            :value="restateRelate"
            type="text"
            class="mt-2 w-full rounded-[18px] border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-900 outline-none focus:border-primary focus:ring-2 focus:ring-primary/15"
            placeholder="相关概念或相邻知识点"
            @input="$emit('update:restateRelate', ($event.target as HTMLInputElement).value)"
          />
        </label>
      </div>
    </div>

    <div
      v-if="showAdvance"
      class="rounded-[28px] border border-emerald-200/90 bg-emerald-50/40 p-5 md:p-7"
    >
      <p class="text-sm font-semibold text-slate-900">微检查</p>
      <p class="mt-1 text-sm text-slate-600">勾一下，确认你真的过关了。</p>
      <div class="mt-4 space-y-3">
        <label
          v-for="(label, i) in microCheckLabels"
          :key="i"
          class="flex cursor-pointer items-start gap-3 text-sm leading-6 text-slate-800"
        >
          <input
            type="checkbox"
            class="mt-1 h-4 w-4 rounded border-slate-300 text-primary focus:ring-primary/30"
            :checked="checks[i]"
            @change="toggleCheck(i)"
          />
          <span>{{ label }}</span>
        </label>
      </div>
      <PrimaryButton
        class="mt-6"
        :disabled="!canAdvance"
        :loading="advancing"
        @click="$emit('advance')"
      >
        {{ advanceLabel }}
      </PrimaryButton>
    </div>
  </section>
</template>

<script setup lang="ts">
import PrimaryButton from '@/components/ui/PrimaryButton.vue'
import type { ExecutionScaffoldCardModel } from '@/types/executionGuide'

interface Turn {
  role: 'USER' | 'ASSISTANT'
  content: string
}

const props = defineProps<{
  cards: ExecutionScaffoldCardModel[]
  chatTurns: Turn[]
  draftValue: string
  inputLabel: string
  inputPlaceholder: string
  primaryActionLabel: string
  sending: boolean
  canSubmitChat: boolean
  showRestate: boolean
  showAdvance: boolean
  microCheckLabels: string[]
  checks: boolean[]
  canAdvance: boolean
  advancing?: boolean
  advanceLabel: string
  restateWhat: string
  restateProblem: string
  restateRelate: string
}>()

const emit = defineEmits<{
  'send-card': [prompt: string]
  'submit-chat': []
  advance: []
  'update:draftValue': [value: string]
  'update:restateWhat': [value: string]
  'update:restateProblem': [value: string]
  'update:restateRelate': [value: string]
  'update:checks': [value: boolean[]]
}>()

function toggleCheck(index: number) {
  const next = [...props.checks]
  next[index] = !next[index]
  emit('update:checks', next)
}
</script>
