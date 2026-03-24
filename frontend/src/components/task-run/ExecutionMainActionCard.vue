<template>
  <section
    data-testid="execution-main-action"
    class="overflow-hidden rounded-[32px] border border-slate-200 bg-[linear-gradient(180deg,_rgba(255,255,255,1),_rgba(248,250,252,0.96))] p-5 shadow-card md:p-7"
  >
    <div class="max-w-3xl">
      <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">
        {{ model.eyebrow }}
      </p>
      <h2 class="mt-3 text-2xl font-semibold tracking-tight text-slate-950 md:text-3xl">
        {{ model.title }}
      </h2>
      <p class="mt-3 text-sm leading-6 text-slate-600 md:text-base">
        {{ model.description }}
      </p>
    </div>

    <div v-if="model.mode === 'guided-input'" class="mt-6 space-y-5">
      <label class="block">
        <span class="text-sm font-semibold text-slate-950">{{ model.inputLabel }}</span>
        <textarea
          data-testid="execution-main-input"
          :value="draftValue"
          rows="7"
          class="mt-3 w-full rounded-[24px] border border-slate-200 bg-white px-5 py-4 text-sm leading-7 text-slate-900 shadow-sm outline-none transition focus:border-primary focus:ring-4 focus:ring-primary/10"
          :placeholder="model.inputPlaceholder"
          :disabled="disabled"
          @input="$emit('update:draftValue', ($event.target as HTMLTextAreaElement).value)"
        />
      </label>

      <div v-if="model.chips.length" class="space-y-3">
        <p class="text-sm font-semibold text-slate-950">这样开始最轻松</p>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="chip in model.chips"
            :key="chip.id"
            type="button"
            :data-testid="`execution-chip-${chip.id}`"
            class="rounded-full border border-slate-200 bg-slate-50 px-4 py-2 text-sm text-slate-900 transition hover:border-primary/40 hover:bg-primary/5"
            :disabled="disabled"
            @click="$emit('use-chip', chip.fill)"
          >
            {{ chip.label }}
          </button>
        </div>
      </div>

      <div class="rounded-[22px] border border-slate-200 bg-slate-50/80 p-4">
        <p class="text-sm font-semibold text-slate-950">写到什么程度就可以继续</p>
        <p class="mt-2 text-sm leading-6 text-slate-700">
          {{ model.passHint || '先把最小理解写出来，系统会根据这句带你进入下一步。' }}
        </p>
      </div>

      <div class="flex flex-wrap items-center gap-4">
        <PrimaryButton data-testid="execution-primary-action" :loading="loading" :disabled="disabled || !canSubmit" @click="$emit('submit')">
          {{ model.primaryActionLabel }}
        </PrimaryButton>
        <p v-if="model.helperText" class="text-sm text-slate-500">{{ model.helperText }}</p>
      </div>
    </div>

    <div v-else class="mt-6 space-y-5">
      <label class="block">
        <span class="text-sm font-semibold text-slate-950">一句话总结你这一步真正学会了什么</span>
        <textarea
          data-testid="execution-closure-summary"
          :value="closureSummary"
          rows="4"
          class="mt-3 w-full rounded-[24px] border border-slate-200 bg-white px-5 py-4 text-sm leading-7 text-slate-900 shadow-sm outline-none transition focus:border-primary focus:ring-4 focus:ring-primary/10"
          placeholder="例如：我已经能说清这个机制为什么成立，以及它最容易错在哪里。"
          @input="$emit('update:closureSummary', ($event.target as HTMLTextAreaElement).value)"
        />
      </label>

      <div class="grid gap-4 md:grid-cols-2">
        <label class="block">
          <span class="text-sm font-semibold text-slate-950">带走的要点 1</span>
          <input
            data-testid="execution-closure-point1"
            :value="closurePoint1"
            type="text"
            class="mt-3 w-full rounded-[20px] border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 shadow-sm outline-none transition focus:border-primary focus:ring-4 focus:ring-primary/10"
            placeholder="这一步最关键的判断"
            @input="$emit('update:closurePoint1', ($event.target as HTMLInputElement).value)"
          />
        </label>
        <label class="block">
          <span class="text-sm font-semibold text-slate-950">带走的要点 2</span>
          <input
            data-testid="execution-closure-point2"
            :value="closurePoint2"
            type="text"
            class="mt-3 w-full rounded-[20px] border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 shadow-sm outline-none transition focus:border-primary focus:ring-4 focus:ring-primary/10"
            placeholder="最容易混淆或最该记住的地方"
            @input="$emit('update:closurePoint2', ($event.target as HTMLInputElement).value)"
          />
        </label>
      </div>

      <label class="block">
        <span class="text-sm font-semibold text-slate-950">下一步你准备怎么继续</span>
        <input
          data-testid="execution-closure-next"
          :value="closureNext"
          type="text"
          class="mt-3 w-full rounded-[20px] border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 shadow-sm outline-none transition focus:border-primary focus:ring-4 focus:ring-primary/10"
          placeholder="例如：换一个相近例子，再独立走一遍。"
          @input="$emit('update:closureNext', ($event.target as HTMLInputElement).value)"
        />
      </label>

      <details class="rounded-[22px] border border-slate-200 bg-slate-50/80 p-4">
        <summary class="cursor-pointer text-sm font-medium text-slate-900">更多完成信息</summary>
        <div class="mt-4 space-y-4 border-t border-slate-200 pt-4">
          <label class="block">
            <span class="text-sm font-semibold text-slate-950">完成状态</span>
            <select
              :value="completionStatus"
              class="mt-3 w-full rounded-[20px] border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 shadow-sm outline-none transition focus:border-primary focus:ring-4 focus:ring-primary/10"
              @change="$emit('update:completionStatus', ($event.target as HTMLSelectElement).value)"
            >
              <option v-for="(label, value) in taskCompletionStatusLabels" :key="value" :value="value">
                {{ label }}
              </option>
            </select>
          </label>
          <label class="block">
            <span class="text-sm font-semibold text-slate-950">补充反思</span>
            <textarea
              :value="learnerReflection"
              rows="3"
              class="mt-3 w-full rounded-[20px] border border-slate-200 bg-white px-4 py-3 text-sm leading-6 text-slate-900 shadow-sm outline-none transition focus:border-primary focus:ring-4 focus:ring-primary/10"
              placeholder="记录这一步里最有帮助的做法。"
              @input="$emit('update:learnerReflection', ($event.target as HTMLTextAreaElement).value)"
            />
          </label>
        </div>
      </details>

      <div class="flex flex-wrap items-center gap-4">
        <PrimaryButton data-testid="execution-primary-action" :loading="loading" @click="$emit('submit')">
          {{ model.primaryActionLabel }}
        </PrimaryButton>
        <p v-if="model.helperText" class="text-sm text-slate-500">{{ model.helperText }}</p>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import PrimaryButton from '@/components/ui/PrimaryButton.vue'
import { taskCompletionStatusLabels } from '@/types/labels'
import type { TaskCompletionStatusType } from '@/types/enums'
import type { ExecutionGuideActionModel } from '@/types/executionGuide'

defineProps<{
  model: ExecutionGuideActionModel
  draftValue: string
  loading?: boolean
  disabled?: boolean
  canSubmit?: boolean
  closureSummary: string
  closurePoint1: string
  closurePoint2: string
  closureNext: string
  learnerReflection: string
  completionStatus: TaskCompletionStatusType
}>()

defineEmits<{
  'update:draftValue': [value: string]
  'update:closureSummary': [value: string]
  'update:closurePoint1': [value: string]
  'update:closurePoint2': [value: string]
  'update:closureNext': [value: string]
  'update:learnerReflection': [value: string]
  'update:completionStatus': [value: string]
  'use-chip': [value: string]
  submit: []
}>()
</script>
