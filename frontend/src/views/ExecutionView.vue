<template>
  <PageContainer>
    <AppTopBar current="task" />
    <main class="mx-auto max-w-3xl px-6 py-12">
      <div class="rounded-[28px] border border-slate-200 bg-white p-8 text-center shadow-card">
        <p class="text-sm font-semibold uppercase tracking-[0.24em] text-primary">
          Redirecting
        </p>
        <h1 class="mt-3 text-2xl font-semibold text-text-primary">
          {{ EXECUTION_COPY.redirectTitle }}
        </h1>
        <p class="mt-3 text-sm leading-6 text-text-secondary">
          {{ EXECUTION_COPY.redirectSubtitle }}
        </p>
      </div>
    </main>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AppTopBar from '@/components/layout/AppTopBar.vue'
import PageContainer from '@/components/layout/PageContainer.vue'
import { showToast } from '@/stores/toast'
import { useWorkflowStore } from '@/stores/workflow'
import { EXECUTION_COPY } from '@/constants/uiCopy'

const router = useRouter()
const store = useWorkflowStore()

onMounted(() => {
  if (store.sessionId) {
    router.replace({ name: 'task' })
    return
  }

  showToast(EXECUTION_COPY.noTaskToast)
  router.replace({ name: 'plan' })
})
</script>


