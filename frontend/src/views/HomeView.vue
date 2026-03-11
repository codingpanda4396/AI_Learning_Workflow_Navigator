<script setup lang="ts">
import { computed, onMounted, reactive } from 'vue';
import { useRouter } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import EmptyState from '@/components/common/EmptyState.vue';
import PageSection from '@/components/common/PageSection.vue';
import { DEFAULT_SESSION_FORM } from '@/constants/app';
import { useSessionStore } from '@/stores/session';

const router = useRouter();
const sessionStore = useSessionStore();

const form = reactive({
  courseId: DEFAULT_SESSION_FORM.courseId,
  chapterId: DEFAULT_SESSION_FORM.chapterId,
  goalText: DEFAULT_SESSION_FORM.goalText,
});

const recentSessionId = computed(() => sessionStore.currentSession?.sessionId || Number(sessionStore.currentSessionId || 0) || null);

async function createAndPlan() {
  const sessionId = await sessionStore.createSession(form);
  await sessionStore.planSession(sessionId);
  await router.push(`/sessions/${sessionId}`);
}

async function continueLearning() {
  if (!recentSessionId.value) {
    return;
  }
  await router.push(`/sessions/${recentSessionId.value}`);
}

onMounted(async () => {
  try {
    await sessionStore.fetchCurrentSession();
  } catch {
    // Keep local fallback only.
  }
});
</script>

<template>
  <AppShell>
    <div class="space-y-6">
      <PageSection
        eyebrow="开始学习"
        title="从这里开始你的这轮学习"
        description="先告诉我你想学什么、正在学到哪一章、这轮最希望解决的问题。系统会为你生成一条可以顺着完成的学习路径。"
      />

      <PageSection compact>
        <div class="mx-auto max-w-3xl rounded-[2rem] bg-slate-950 p-7 text-white shadow-[0_28px_90px_rgba(15,23,42,0.28)] md:p-9">
          <p class="text-xs font-semibold uppercase tracking-[0.28em] text-slate-400">新的学习</p>
          <h2 class="mt-3 text-3xl font-semibold tracking-tight md:text-4xl">开始新的学习</h2>
          <p class="mt-4 text-sm leading-7 text-slate-300 md:text-base">
            填好这三个信息后，就能直接进入当前学习总览，并看到系统建议先完成哪一步。
          </p>

          <form class="mt-8 space-y-5" @submit.prevent="createAndPlan">
            <label class="block">
              <span class="text-sm font-medium text-slate-200">学习课程</span>
              <input
                v-model="form.courseId"
                class="mt-2 w-full rounded-2xl border border-white/12 bg-white/8 px-4 py-3 text-white outline-none placeholder:text-slate-500 focus:border-amber-300"
                placeholder="例如：计算机网络"
              />
            </label>
            <label class="block">
              <span class="text-sm font-medium text-slate-200">当前章节</span>
              <input
                v-model="form.chapterId"
                class="mt-2 w-full rounded-2xl border border-white/12 bg-white/8 px-4 py-3 text-white outline-none placeholder:text-slate-500 focus:border-amber-300"
                placeholder="例如：TCP"
              />
            </label>
            <label class="block">
              <span class="text-sm font-medium text-slate-200">你的目标</span>
              <textarea
                v-model="form.goalText"
                class="mt-2 min-h-32 w-full rounded-2xl border border-white/12 bg-white/8 px-4 py-3 text-white outline-none placeholder:text-slate-500 focus:border-amber-300"
                placeholder="例如：我想真正理解 TCP 为什么能可靠传输"
              />
            </label>
            <button
              class="w-full rounded-2xl bg-amber-400 px-5 py-3 text-sm font-semibold text-slate-950 transition hover:bg-amber-300 disabled:cursor-not-allowed disabled:opacity-60"
              :disabled="sessionStore.loading"
            >
              开始新的学习
            </button>
          </form>
        </div>
      </PageSection>

      <PageSection compact>
        <div class="mx-auto max-w-3xl">
          <div v-if="recentSessionId" class="rounded-[1.6rem] border border-slate-200 bg-white p-5 shadow-sm">
            <div class="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
              <div>
                <p class="text-sm font-semibold text-slate-900">继续最近学习</p>
                <p class="mt-2 text-sm leading-7 text-slate-600">如果你还没完成上一轮学习，可以直接回到当前总览，继续这一步。</p>
              </div>
              <button class="rounded-2xl border border-slate-200 px-5 py-3 text-sm font-medium text-slate-700 transition hover:bg-slate-50" @click="continueLearning">
                继续最近学习
              </button>
            </div>
          </div>
          <EmptyState
            v-else
            title="还没有可继续的学习记录"
            description="先开始一轮新的学习，系统会在你下次回来时帮你接上进度。"
          />
        </div>
      </PageSection>
    </div>
  </AppShell>
</template>
