<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import ErrorState from '@/components/common/ErrorState.vue';
import LoadingState from '@/components/common/LoadingState.vue';
import AppButton from '@/components/ui/AppButton.vue';
import { useSessionStore } from '@/stores/session';
import { useLearningFlowStore } from '@/stores/learningFlow';

const route = useRoute();
const sessionStore = useSessionStore();
const flowStore = useLearningFlowStore();

const sessionId = computed(() => Number(route.params.sessionId));
const overview = computed(() => sessionStore.overview);
const hint = computed(() => overview.value?.summary?.nextStepHint || '先完成当前步骤，系统会自动推进。');

async function openPrimaryAction() {
  const cta = flowStore.snapshot?.primaryCTA;
  if (cta) await flowStore.goToStage(cta.stage);
}

async function loadOverview() {
  if (!sessionId.value) return;
  await flowStore.loadSessionFlow(sessionId.value);
}
</script>

<template>
  <AppShell>
    <LoadingState v-if="sessionStore.loading && !overview" />

    <div v-else-if="sessionStore.error && !overview" class="space-y-4">
      <ErrorState :message="sessionStore.error" />
      <AppButton variant="secondary" @click="loadOverview">重新加载</AppButton>
    </div>

    <div v-else-if="overview" class="mx-auto max-w-[640px] space-y-8">
      <section class="app-hero">
        <p class="app-eyebrow">当前学习进度</p>
        <h1 class="app-title-lg mt-4">{{ overview.summary?.currentTaskTitle || '继续当前学习' }}</h1>
        <p class="app-text-lead mt-4">{{ hint }}</p>
      </section>

      <div class="rounded-2xl border border-slate-200/80 bg-slate-50/50 p-5">
        <dl class="grid gap-4 sm:grid-cols-2">
          <div>
            <dt class="text-xs font-medium uppercase tracking-wider text-slate-500">当前目标</dt>
            <dd class="mt-1 text-sm font-medium text-slate-900">{{ overview.goalText || '—' }}</dd>
          </div>
          <div>
            <dt class="text-xs font-medium uppercase tracking-wider text-slate-500">进度</dt>
            <dd class="mt-1 text-sm font-medium text-slate-900">
              {{ overview.progress.completedTaskCount }} / {{ overview.progress.totalTaskCount }}
            </dd>
          </div>
        </dl>
      </div>

      <div class="flex flex-col gap-3 sm:flex-row sm:items-center">
        <AppButton size="lg" class="w-full sm:w-auto" @click="openPrimaryAction">
          {{ flowStore.snapshot?.primaryCTA?.label ?? '继续当前步骤' }}
        </AppButton>
        <AppButton
          variant="secondary"
          class="w-full sm:w-auto"
          @click="$router.push(`/sessions/${sessionId}/growth`)"
        >
          查看学习记录
        </AppButton>
      </div>
    </div>
  </AppShell>
</template>
