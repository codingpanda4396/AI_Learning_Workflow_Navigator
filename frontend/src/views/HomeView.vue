<script setup lang="ts">
import { computed, onMounted, reactive } from 'vue';
import { useRouter } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import EmptyState from '@/components/common/EmptyState.vue';
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
  const target = recentSessionId.value;
  if (!target) {
    return;
  }
  await router.push(`/sessions/${target}`);
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
    <div class="grid gap-8 lg:grid-cols-[1.15fr_0.85fr]">
      <section class="rounded-[2rem] bg-white p-8 shadow-sm ring-1 ring-slate-200">
        <p class="text-xs uppercase tracking-[0.24em] text-slate-400">Step 1</p>
        <h2 class="mt-3 text-3xl font-semibold text-slate-900">新建学习会话</h2>
        <p class="mt-3 text-sm leading-7 text-slate-600">
          首页只做一件事：把新的学习闭环启动起来。创建成功后会自动调用 plan，并进入 session 总览。
        </p>

        <form class="mt-8 space-y-5" @submit.prevent="createAndPlan">
          <label class="block">
            <span class="text-sm text-slate-600">course_id</span>
            <input v-model="form.courseId" class="mt-2 w-full rounded-2xl border border-slate-200 px-4 py-3 outline-none focus:border-slate-400" />
          </label>
          <label class="block">
            <span class="text-sm text-slate-600">chapter_id</span>
            <input v-model="form.chapterId" class="mt-2 w-full rounded-2xl border border-slate-200 px-4 py-3 outline-none focus:border-slate-400" />
          </label>
          <label class="block">
            <span class="text-sm text-slate-600">goal_text</span>
            <textarea v-model="form.goalText" class="mt-2 min-h-30 w-full rounded-2xl border border-slate-200 px-4 py-3 outline-none focus:border-slate-400" />
          </label>
          <button class="rounded-2xl bg-slate-900 px-5 py-3 text-sm font-medium text-white disabled:opacity-60" :disabled="sessionStore.loading">
            创建并规划任务
          </button>
        </form>
      </section>

      <section class="space-y-6">
        <div class="rounded-[2rem] bg-slate-900 p-8 text-white">
          <p class="text-xs uppercase tracking-[0.24em] text-slate-400">Step 2</p>
          <h3 class="mt-3 text-2xl font-semibold">继续最近学习</h3>
          <p class="mt-3 text-sm leading-7 text-slate-300">
            如果本地或后端存在最近的 session，可直接回到总览继续当前链路。
          </p>
          <button
            class="mt-6 rounded-2xl bg-white px-5 py-3 text-sm font-medium text-slate-900 disabled:opacity-50"
            :disabled="!recentSessionId"
            @click="continueLearning"
          >
            {{ recentSessionId ? `继续 Session #${recentSessionId}` : '暂无最近学习' }}
          </button>
        </div>

        <EmptyState
          title="当前首页职责很单一"
          description="新建学习，或者继续最近学习。其它导航全部放进 SessionView。"
        />
      </section>
    </div>
  </AppShell>
</template>
