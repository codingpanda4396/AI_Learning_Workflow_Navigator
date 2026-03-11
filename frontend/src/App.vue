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
  <div class="min-h-screen bg-[#f7f8fa] text-slate-900">
    <AppHeader v-if="showHeader" />
    <main :class="showHeader ? 'pt-20' : ''">
      <router-view />
    </main>
    <transition name="fade">
      <div
        v-if="message"
        class="fixed right-6 top-6 z-50 max-w-sm rounded-2xl px-4 py-3 text-sm shadow-lg"
        :class="message.type === 'error' ? 'bg-rose-600 text-white' : 'bg-slate-900 text-white'"
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
