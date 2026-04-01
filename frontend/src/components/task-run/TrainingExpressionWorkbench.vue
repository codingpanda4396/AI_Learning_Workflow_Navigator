<template>
  <section class="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
    <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">表达任务</p>
    <p class="mt-1 text-base font-semibold text-slate-900">{{ taskTitle }}</p>
    <p class="mt-1 text-sm text-slate-700">{{ taskRequirement }}</p>
    <p class="mt-2 text-xs text-slate-500">{{ prompt }}</p>
    <textarea
      class="mt-3 min-h-[150px] w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-primary/50 focus:bg-white"
      :value="draft"
      :disabled="busy"
      placeholder="写一版你的表达"
      @input="$emit('update:draft', ($event.target as HTMLTextAreaElement).value)"
    />
    <div class="mt-3 flex justify-end">
      <button
        type="button"
        class="rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-60"
        :disabled="busy || !draft.trim()"
        @click="$emit('submit')"
      >
        提交表达
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    taskTitle: string
    taskRequirement: string
    prompt: string
    draft: string
    busy?: boolean
  }>(),
  { busy: false }
)

defineEmits<{
  'update:draft': [value: string]
  submit: []
}>()
</script>
