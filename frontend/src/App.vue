<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import AppHeader from '@/components/common/AppHeader.vue';

type AppMessage = {
  type: 'error' | 'info';
  text: string;
};

const route = useRoute();
const message = ref<AppMessage | null>(null);
let timer: number | undefined;

const showHeader = computed(() => route.path !== '/login');

function showMessage(event: Event) {
  const detail = (event as CustomEvent<AppMessage>).detail;
  message.value = detail;
  if (timer) {
    window.clearTimeout(timer);
  }
  timer = window.setTimeout(() => {
    message.value = null;
  }, 3000);
}

onMounted(() => {
  window.addEventListener('app:message', showMessage as EventListener);
});

onBeforeUnmount(() => {
  window.removeEventListener('app:message', showMessage as EventListener);
  if (timer) {
    window.clearTimeout(timer);
  }
});
</script>

<template>
  <div class="min-h-screen text-slate-900">
    <div class="pointer-events-none fixed inset-x-0 top-0 z-0 h-80 bg-[radial-gradient(circle_at_top,rgba(150,190,214,0.16),transparent_55%)]" />
    <AppHeader v-if="showHeader" />
    <main class="relative z-10" :class="showHeader ? 'pt-22' : ''">
      <router-view />
    </main>
    <transition name="fade">
      <div
        v-if="message"
        class="fixed right-6 top-6 z-50 max-w-sm rounded-[18px] border px-4 py-3 text-sm shadow-[0_18px_48px_rgba(15,23,42,0.18)] backdrop-blur"
        :class="message.type === 'error' ? 'border-rose-200 bg-rose-50/95 text-rose-700' : 'border-slate-200 bg-white/95 text-slate-800'"
      >
        {{ message.text }}
      </div>
    </transition>
  </div>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
