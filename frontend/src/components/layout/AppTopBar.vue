<template>
  <header
    class="sticky top-0 z-10 border-b border-border bg-white/95 backdrop-blur supports-[backdrop-filter]:bg-white/80"
  >
    <div class="mx-auto flex min-h-14 max-w-5xl items-center justify-between gap-4 px-6 py-3">
      <router-link
        to="/goal"
        class="text-lg font-semibold text-text-primary transition-colors hover:text-primary"
      >
        {{ APP_COPY.brand }}
      </router-link>

      <WorkflowStepper v-if="current" :current="current" />

      <div class="flex items-center gap-3">
        <template v-if="auth.isAuthenticated && auth.user">
          <div class="hidden text-right md:block">
            <p class="text-sm font-medium text-text-primary">{{ auth.user.displayName }}</p>
            <p class="text-xs text-slate-400">{{ APP_COPY.appBarSignedInHint }}</p>
          </div>
          <button
            type="button"
            class="rounded-full border border-slate-200 px-3 py-1.5 text-sm text-text-secondary transition hover:border-slate-300 hover:text-text-primary"
            @click="handleLogout"
          >
            {{ APP_COPY.logoutCta }}
          </button>
        </template>
        <router-link
          v-else
          to="/auth/login"
          class="rounded-full bg-slate-900 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-800"
        >
          {{ APP_COPY.loginCta }}
        </router-link>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import WorkflowStepper from './WorkflowStepper.vue'
import { useAuthStore } from '@/stores/auth'
import { useWorkflowStore } from '@/stores/workflow'
import { APP_COPY } from '@/constants/uiCopy'

defineProps<{
  current?: 'goal' | 'diagnosis' | 'plan' | 'task' | 'report'
}>()
const router = useRouter()
const auth = useAuthStore()
const workflow = useWorkflowStore()

async function handleLogout() {
  await auth.logout()
  workflow.reset()
  await router.push('/goal')
}
</script>
