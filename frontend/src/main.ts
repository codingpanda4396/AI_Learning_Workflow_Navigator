import { createApp } from 'vue';
import { createPinia } from 'pinia';
import 'uno.css';
import '@unocss/reset/tailwind.css';
import App from './App.vue';
import router from './router';
import { useAuthStore } from './stores/auth';

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);

const authStore = useAuthStore(pinia);
authStore.hydrateFromLocalStorage();

app.use(router);
app.mount('#app');
