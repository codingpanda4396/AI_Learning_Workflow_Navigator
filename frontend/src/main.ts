import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import './style.css'
import './styles/animations.css'
import App from './App.vue'
import { applyUiTokens } from '@/constants/ui'
import { useAuthStore } from '@/stores/auth'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

applyUiTokens()
useAuthStore(pinia).bootstrap()

app.mount('#app')
