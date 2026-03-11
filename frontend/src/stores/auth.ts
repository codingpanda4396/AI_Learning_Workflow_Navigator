import { defineStore } from 'pinia';
import { fetchMeApi, loginApi, registerApi } from '@/api/modules/auth';
import type { LoginPayload, RegisterPayload } from '@/types/auth';
import { clearStoredAuth, getStoredToken, getStoredUsername, setStoredAuth } from '@/utils/storage';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: '',
    username: '',
    loading: false,
    error: '',
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
  },
  actions: {
    hydrateFromLocalStorage() {
      this.token = getStoredToken();
      this.username = getStoredUsername();
    },
    async login(payload: LoginPayload) {
      this.loading = true;
      this.error = '';
      try {
        const response = await loginApi(payload);
        this.token = response.token;
        this.username = response.user?.username ?? payload.username;
        setStoredAuth(this.token, this.username);
      } catch (error) {
        this.error = error instanceof Error ? error.message : '登录失败';
        throw error;
      } finally {
        this.loading = false;
      }
    },
    async register(payload: RegisterPayload) {
      this.loading = true;
      this.error = '';
      try {
        await registerApi(payload);
      } catch (error) {
        this.error = error instanceof Error ? error.message : '注册失败';
        throw error;
      } finally {
        this.loading = false;
      }
    },
    async refreshUser() {
      if (!this.token) {
        return;
      }
      try {
        const user = await fetchMeApi();
        if (user.username) {
          this.username = user.username;
          setStoredAuth(this.token, this.username);
        }
      } catch {
        this.logout();
      }
    },
    logout() {
      this.token = '';
      this.username = '';
      this.error = '';
      clearStoredAuth();
    },
  },
});
