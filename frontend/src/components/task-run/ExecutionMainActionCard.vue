<template>
  <section
    data-testid="execution-main-action"
    class="overflow-hidden rounded-[32px] border border-slate-200 bg-[linear-gradient(180deg,_rgba(255,255,255,1),_rgba(248,250,252,0.96))] p-5 shadow-card md:p-7"
  >
    <div class="flex flex-wrap items-center gap-2">
      <span
        v-if="model.phaseCode"
        class="rounded-full bg-primary/10 px-3 py-1 text-xs font-bold uppercase tracking-[0.12em] text-primary"
      >
        {{ model.phaseCode }}
      </span>
      <span v-if="model.phaseDisplayZh" class="text-sm font-semibold text-slate-800">
        {{ model.phaseDisplayZh }}
      </span>
    </div>

    <div class="mt-4 max-w-3xl">
      <h2 class="text-xl font-semibold tracking-tight text-slate-950 md:text-2xl">
        {{ model.title }}
      </h2>
      <p v-if="model.description?.trim()" class="mt-2 text-sm leading-6 text-slate-600">
        {{ model.description }}
      </p>
    </div>

    <div v-if="model.mode === 'guided-input'" class="mt-6 space-y-5">
      <div
        v-if="model.directive"
        class="rounded-[20px] border border-sky-100 bg-sky-50/60 px-4 py-3 text-sm font-medium leading-7 text-slate-900"
        data-testid="execution-main-directive"
      >
        {{ model.directive }}
      </div>

      <div v-if="model.chips.length" class="space-y-2">
        <p class="text-sm font-semibold text-slate-950">一键填入开头</p>
        <div class="flex flex-col gap-2">
          <button
            v-for="chip in model.chips"
            :key="chip.id"
            type="button"
            :data-testid="`execution-chip-${chip.id}`"
            class="w-full rounded-[20px] border-2 border-slate-200 bg-white px-4 py-3 text-left text-sm font-medium text-slate-900 transition hover:border-primary hover:shadow-sm disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="disabled"
            @click="$emit('use-chip', chip.fill)"
          >
            {{ chip.label }}
          </button>
        </div>
      </div>

      <label class="block">
        <span class="text-sm font-semibold text-slate-950">{{ model.inputLabel }}</span>
        <textarea
          data-testid="execution-main-input"
          :value="draftValue"
          rows="5"
          class="mt-3 w-full rounded-[24px] border border-slate-200 bg-white px-5 py-4 text-sm leading-7 text-slate-900 shadow-sm outline-none transition focus:border-primary focus:ring-4 focus:ring-primary/10"
          :placeholder="model.inputPlaceholder"
          :disabled="disabled"
          @input="$emit('update:draftValue', ($event.target as HTMLTextAreaElement).value)"
        />
      </label>

      <details v-if="model.passHint" class="rounded-[20px] border border-slate-200 bg-slate-50/80 px-4 py-3 text-sm text-slate-700">
        <summary class="cursor-pointer font-medium text-slate-900">写到哪算过关</summary>
        <p class="mt-2 leading-6">{{ model.passHint }}</p>
      </details>

      <div class="flex flex-wrap items-center gap-4">
        <PrimaryButton
          data-testid="execution-primary-action"
          :loading="loading"
          :disabled="disabled || !canSubmit"
          @click="$emit('submit')"
        >
          {{ model.primaryActionLabel }}
        </PrimaryButton>
        <p v-if="model.helperText" class="text-sm text-slate-500">{{ model.helperText }}</p>
      </div>
    </div>

    <div v-else class="mt-6 space-y-5">
      <label class="block">
        <span class="text-sm font-semibold text-slate-950">一句话收住这个知识点</span>
        <textarea
          data-testid="execution-closure-summary"
          :value="closureSummary"
          rows="4"
          class="mt-3 w-full rounded-[24px] border border-slate-200 bg-white px-5 py-4 text-sm leading-7 text-slate-900 shadow-sm outline-none transition focus:border-primary focus:ring-4 focus:ring-primary/10"
          placeholder="例如：我已经能说清这个知识点为什么要这样做。"
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
            placeholder="这一点最关键的判断"
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
            placeholder="最容易漏掉但最该记住的地方"
            @input="$emit('update:closurePoint2', ($event.target as HTMLInputElement).value)"
          />
        </label>
      </div>

      <label class="block">
        <span class="text-sm font-semibold text-slate-950">下一个知识点你想怎么接</span>
        <input
          data-testid="execution-closure-next"
          :value="closureNext"
          type="text"
          class="mt-3 w-full rounded-[20px] border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 shadow-sm outline-none transition focus:border-primary focus:ring-4 focus:ring-primary/10"
          placeholder="例如：换一个相近例子，再独立做一遍。"
          @input="$emit('update:closureNext', ($event.target as HTMLInputElement).value)"
        />
      </label>

      <details class="rounded-[22px] border border-slate-200 bg-slate-50/80 p-4">
        <summary class="cursor-pointer text-sm font-medium text-slate-900">需要时再补充</summary>
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
            <span class="text-sm font-semibold text-slate-950">补充记录</span>
            <textarea
              :value="learnerReflection"
              rows="3"
              class="mt-3 w-full rounded-[20px] border border-slate-200 bg-white px-4 py-3 text-sm leading-6 text-slate-900 shadow-sm outline-none transition focus:border-primary focus:ring-4 focus:ring-primary/10"
              placeholder="记下这一步里最有帮助的做法。"
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
