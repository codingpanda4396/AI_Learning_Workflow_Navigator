<template>
  <PageContainer>
    <AppTopBar />
    <main class="relative overflow-hidden">
      <div class="absolute inset-x-0 top-0 h-80 bg-[radial-gradient(circle_at_top,rgba(15,23,42,0.10),transparent_60%)]" />
      <div class="relative mx-auto flex min-h-[calc(100vh-56px)] max-w-5xl items-center px-6 py-10">
        <section class="grid w-full gap-8 lg:grid-cols-[1.1fr_0.9fr]">
          <div class="space-y-6">
            <p class="text-xs font-semibold uppercase tracking-[0.32em] text-slate-400">{{ AUTH_COPY.eyebrow }}</p>
            <div class="space-y-4">
              <h1 class="max-w-xl text-4xl font-semibold leading-tight text-slate-950">
                {{ AUTH_COPY.heroTitle }}
              </h1>
              <p class="max-w-lg text-base leading-7 text-slate-600">
                {{ AUTH_COPY.heroSubtitle }}
              </p>
            </div>
            <div class="grid gap-3 sm:grid-cols-3">
              <article
                v-for="item in highlights"
                :key="item.title"
                class="rounded-2xl border border-slate-200/80 bg-white/85 p-4 shadow-[0_12px_32px_rgba(15,23,42,0.05)]"
              >
                <p class="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">{{ item.kicker }}</p>
                <p class="mt-3 text-base font-semibold text-slate-900">{{ item.title }}</p>
                <p class="mt-2 text-sm leading-6 text-slate-600">{{ item.body }}</p>
              </article>
            </div>
          </div>

          <section class="rounded-[28px] border border-slate-200/80 bg-white p-6 shadow-[0_24px_80px_rgba(15,23,42,0.08)]">
            <div class="flex rounded-full bg-slate-100 p-1">
              <router-link
                :to="{ name: 'login', query: route.query }"
                class="flex-1 rounded-full px-4 py-2 text-center text-sm font-medium transition"
                :class="mode === 'login' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500'"
              >
                {{ AUTH_COPY.tabLogin }}
              </router-link>
              <router-link
                :to="{ name: 'register', query: route.query }"
                class="flex-1 rounded-full px-4 py-2 text-center text-sm font-medium transition"
                :class="mode === 'register' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500'"
              >
                {{ AUTH_COPY.tabRegister }}
              </router-link>
            </div>

            <form class="mt-6 space-y-4" @submit.prevent="submit">
              <div class="space-y-2">
                <label class="text-sm font-medium text-slate-700">{{ AUTH_COPY.labelUsername }}</label>
                <input
                  v-model.trim="form.username"
                  type="text"
                  autocomplete="username"
                  class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none transition focus:border-slate-900"
                  :placeholder="AUTH_COPY.placeholderUsername"
                />
              </div>
              <div class="space-y-2">
                <label class="text-sm font-medium text-slate-700">{{ AUTH_COPY.labelPassword }}</label>
                <input
                  v-model="form.password"
                  type="password"
                  autocomplete="current-password"
                  class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm outline-none transition focus:border-slate-900"
                  :placeholder="AUTH_COPY.placeholderPassword"
                />
              </div>

              <div class="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600">
                {{ AUTH_COPY.helper }}
              </div>

              <button
                type="submit"
                class="w-full rounded-2xl bg-slate-950 px-4 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400"
                :disabled="loading"
              >
                {{ loading ? AUTH_COPY.processing : mode === 'login' ? AUTH_COPY.submitLogin : AUTH_COPY.submitRegister }}
              </button>
            </form>
          </section>
        </section>
      </div>
    </main>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppTopBar from '@/components/layout/AppTopBar.vue'
import PageContainer from '@/components/layout/PageContainer.vue'
import { useAuthStore } from '@/stores/auth'
import { useWorkflowStore } from '@/stores/workflow'
import { showToast } from '@/stores/toast'
import { getErrorMessage } from '@/api/request'
import { AUTH_COPY } from '@/constants/uiCopy'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const workflow = useWorkflowStore()

const mode = computed(() => (route.name === 'register' ? 'register' : 'login'))
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const highlights = AUTH_COPY.highlights

async function submit() {
  if (!form.username || !form.password) {
    showToast(AUTH_COPY.toastMissingCredentials)
    return
  }
  try {
    loading.value = true
    if (mode.value === 'register') {
      await auth.register(form.username, form.password)
    } else {
      await auth.login(form.username, form.password)
    }
    const redirect = auth.consumePendingRedirect() ?? '/goal'
    if (redirect === '/goal' && !workflow.goalId) {
      await router.push('/goal')
      return
    }
    await router.push(redirect)
  } catch (err) {
    showToast(getErrorMessage(err))
  } finally {
    loading.value = false
  }
}
</script>
