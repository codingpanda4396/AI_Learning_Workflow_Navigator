import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import './style.css'
import App from './App.vue'
import { applyUiTokens } from '@/constants/ui'

const app = createApp(App)

app.use(createPinia())
app.use(router)

applyUiTokens()

app.mount('#app')
