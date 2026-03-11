import { defineConfig, presetAttributify, presetUno, presetTypography } from 'unocss';

export default defineConfig({
  presets: [presetUno(), presetAttributify(), presetTypography()],
  theme: {
    fontFamily: {
      sans: 'Segoe UI, PingFang SC, Hiragino Sans GB, Microsoft YaHei, sans-serif',
    },
  },
});
