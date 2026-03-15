<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import LearningActionBar from '@/components/learning/LearningActionBar.vue';
import LearningPageHeader from '@/components/learning/LearningPageHeader.vue';
import LearningStatePanel from '@/components/learning/LearningStatePanel.vue';
import AppButton from '@/components/ui/AppButton.vue';
import { PRIMARY_CTA, SECONDARY_LABELS } from '@/constants/learningFlow';
import { useSessionStore } from '@/stores/session';
import { useLearningFlowStore } from '@/stores/learningFlow';

const route = useRoute();
const sessionStore = useSessionStore();
const flowStore = useLearningFlowStore();

const sessionId = computed(() => Number(route.params.sessionId));
const overview = computed(() => sessionStore.overview);
const hint = computed(() => overview.value?.summary?.nextStepHint || '');

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
    <LearningStatePanel
      v-if="sessionStore.loading && !overview"
      state="loading"
    />
    <LearningStatePanel
      v-else-if="sessionStore.error && !overview"
      state="error"
      :message="sessionStore.error"
      :action-label="SECONDARY_LABELS.RETRY"
      @action="loadOverview"
    />

    <div v-else-if="overview" class="mx-auto max-w-[640px] space-y-8">
      <LearningPageHeader
        eyebrow="当前学习进度"
        :title="overview.summary?.currentTaskTitle || '继续当前学习'"
        :lead="hint"
      />

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

      <LearningActionBar>
        <template #primary>
          <AppButton size="lg" class="w-full sm:w-auto" @click="openPrimaryAction">
            {{ PRIMARY_CTA.CONTINUE_TASK }}
          </AppButton>
        </template>
        <template #secondary>
          <AppButton
            variant="secondary"
            class="w-full sm:w-auto"
            @click="$router.push(`/sessions/${sessionId}/growth`)"
          >
            {{ SECONDARY_LABELS.VIEW_RECORDS }}
          </AppButton>
        </template>
      </LearningActionBar>
    </div>
  </AppShell>
</template>
