# Lumina AI 前端设计规范

风格关键词：理性、专业、可信、克制、现代、AI 教育科技感、轻生产力工具感。

## 色彩系统

| 用途 | 色值 | 变量 |
|------|------|------|
| 主色 | #4F46E5 | --color-primary |
| 主色悬停 | #4338CA | --color-primary-hover |
| 辅助色 | #6366F1 | --color-secondary |
| 背景 | #F8FAFC | --color-background |
| 卡片 | #FFFFFF | --color-card-bg |
| 主文案 | #0F172A | --color-text-primary |
| 辅助文案 | #475569 | --color-text-secondary |
| 边框 | #E2E8F0 | --color-border |
| 成功 | #059669 | --color-success |
| 警告 | #D97706 | --color-warning |
| 错误 | #DC2626 | --color-error |

## 排版

- 大标题：32-40px，font-weight 700
- 模块标题：20-24px，font-weight 600
- 正文：14-16px
- 辅助文案：12-14px
- 行高：1.5-1.6

## 间距

- 页面主区块：24px / 32px
- 卡片内边距：20px / 24px

## 圆角与阴影

- 卡片圆角：16px
- 输入框/按钮圆角：12px
- 阴影：0 1px 3px rgba(0,0,0,.08)

## 组件规范

- PageContainer：页面容器
- AppTopBar：顶栏
- FormCard：表单卡片
- PrimaryButton / SecondaryButton
- StatusBadge、MetricCard
- EmptyState、ErrorState、LoadingState
- WorkflowStepper：步骤指示器

## 交互

- Hover/active 动效轻量
- 过渡时长不超过 200ms
