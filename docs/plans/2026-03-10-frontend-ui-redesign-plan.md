# Frontend UI Redesign Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Redesign the AI Learning Workflow Navigator frontend with warm and friendly visual style, improved color contrast, enhanced component styles, and smooth animations.

**Architecture:** Update CSS variables first as the foundation, then global styles, then components, then add animations. All changes are CSS-only with minimal Vue template changes.

**Tech Stack:** Vue 3, TypeScript, CSS Variables

---

## Task 1: Update CSS Variables in style.css

**Files:**
- Modify: `frontend/src/style.css:1-42`

**Step 1: Replace CSS variables**

Replace the entire `:root` block with new warm-friendly color palette:

```css
:root {
  /* Primary - Warm Blue */
  --color-primary: #6B9FFF;
  --color-primary-hover: #8AB6FF;
  --color-primary-active: #5A8AE8;
  --color-primary-alpha: rgba(107, 159, 255, 0.18);

  /* Accent Colors */
  --color-accent-coral: #FF8A7A;
  --color-accent-lavender: #B8A9FF;
  --color-accent-mint: #7EDFC5;
  --color-accent-peach: #FFB88C;

  /* Background */
  --color-bg: #0D1117;
  --color-bg-elevated: #151D2B;
  --color-bg-surface: #1C2636;
  --color-bg-hover: #252F42;

  /* Text - Improved contrast */
  --color-text: #F0F4F8;
  --color-text-secondary: #A3B4CC;
  --color-text-muted: #6B7A94;

  /* Border */
  --color-border: #2A3A52;
  --color-border-hover: #3D5068;

  /* Status */
  --color-success: #5DD4A6;
  --color-warning: #FFB88C;
  --color-error: #FF7A8A;
  --color-info: #7EB8FF;

  /* Radius */
  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 16px;
  --radius-xl: 20px;
  --radius-full: 9999px;

  /* Spacing */
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 12px;
  --space-lg: 16px;
  --space-xl: 24px;
  --space-xxl: 32px;

  /* Shadows */
  --shadow-sm: 0 4px 16px rgba(0, 0, 0, 0.25);
  --shadow-md: 0 8px 32px rgba(0, 0, 0, 0.35);
  --shadow-lg: 0 16px 48px rgba(0, 0, 0, 0.45);

  /* Typography */
  --font-display: 'Space Grotesk', 'Segoe UI', sans-serif;
  --font-body: 'Manrope', 'Segoe UI', sans-serif;

  --font-size-xs: 12px;
  --font-size-sm: 14px;
  --font-size-md: 16px;
  --font-size-lg: 20px;
  --font-size-xl: 28px;
  --font-size-2xl: 36px;

  /* Animation */
  --ease-smooth: cubic-bezier(0.4, 0, 0.2, 1);
  --ease-bounce: cubic-bezier(0.68, -0.55, 0.265, 1.55);
  --duration-fast: 150ms;
  --duration-normal: 250ms;
  --duration-slow: 400ms;
}
```

**Step 2: Run verification**

Check: `grep -c "color-primary" frontend/src/style.css`
Expected: Multiple matches showing variables are defined

**Step 3: Commit**

```bash
git add frontend/src/style.css
git commit -m "feat(ui): update CSS variables with warm-friendly color palette"
```

---

## Task 2: Update Global Styles and Background

**Files:**
- Modify: `frontend/src/style.css:55-70`

**Step 1: Update body background**

Replace body styles with enhanced background:

```css
body {
  margin: 0;
  font-family: var(--font-body);
  font-size: var(--font-size-md);
  line-height: 1.55;
  color: var(--color-text);
  background-color: var(--color-bg);
  background-image:
    radial-gradient(ellipse at 20% 30%, rgba(107, 159, 255, 0.08) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 70%, rgba(255, 138, 122, 0.06) 0%, transparent 50%),
    radial-gradient(ellipse at 50% 100%, rgba(184, 169, 255, 0.05) 0%, transparent 40%),
    linear-gradient(180deg, #0D1117 0%, #0A0E14 100%);
  background-attachment: fixed;
}
```

**Step 2: Add reduced motion support**

Add at end of style.css:

```css
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
```

**Step 3: Commit**

```bash
git add frontend/src/style.css
git commit -m "feat(ui): enhance background with warm gradients and add reduced motion support"
```

---

## Task 3: Create Animation CSS Module

**Files:**
- Create: `frontend/src/styles/animations.css`
- Modify: `frontend/src/main.ts:1` (import)

**Step 1: Create animations.css**

```css
/* Keyframes */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes scaleIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  10%, 30%, 50%, 70%, 90% { transform: translateX(-4px); }
  20%, 40%, 60%, 80% { transform: translateX(4px); }
}

@keyframes borderGlow {
  0%, 100% { border-color: var(--color-border); }
  50% { border-color: var(--color-primary); }
}

/* Animation classes */
.animate-fade-in-up {
  animation: fadeInUp var(--duration-slow) var(--ease-smooth) forwards;
}

.animate-fade-in {
  animation: fadeIn var(--duration-normal) var(--ease-smooth) forwards;
}

.animate-scale-in {
  animation: scaleIn var(--duration-normal) var(--ease-smooth) forwards;
}

.animate-pulse {
  animation: pulse 2s ease-in-out infinite;
}

.animate-shimmer {
  background: linear-gradient(
    90deg,
    var(--color-bg-surface) 25%,
    var(--color-bg-hover) 50%,
    var(--color-bg-surface) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.animate-shake {
  animation: shake 0.5s ease-in-out;
}

/* Stagger delays */
.stagger-1 { animation-delay: 0ms; }
.stagger-2 { animation-delay: 80ms; }
.stagger-3 { animation-delay: 160ms; }
.stagger-4 { animation-delay: 240ms; }
.stagger-5 { animation-delay: 320ms; }
```

**Step 2: Import in main.ts**

Add to imports in `frontend/src/main.ts`:
```ts
import './styles/animations.css'
```

**Step 3: Commit**

```bash
git add frontend/src/styles/animations.css frontend/src/main.ts
git commit -m "feat(ui): add animation CSS module with keyframes and utility classes"
```

---

## Task 4: Update PrimaryButton Component

**Files:**
- Modify: `frontend/src/components/PrimaryButton.vue`

**Step 1: Update button styles**

Replace the component's style section with:

```vue
<style scoped>
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-sm);
  padding: 14px 28px;
  font-family: var(--font-body);
  font-size: var(--font-size-md);
  font-weight: 600;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-smooth);
  min-height: 52px;
}

.btn-primary {
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-hover));
  color: #fff;
  box-shadow:
    0 4px 16px rgba(107, 159, 255, 0.35),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow:
    0 8px 24px rgba(107, 159, 255, 0.45),
    inset 0 1px 0 rgba(255, 255, 255, 0.25);
}

.btn-primary:active:not(:disabled) {
  transform: translateY(0);
  box-shadow:
    0 2px 8px rgba(107, 159, 255, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.15);
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.loading-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
```

**Step 2: Update template for animation**

Wrap button content with transition effect (template stays mostly the same, just ensure loading spinner has class)

**Step 3: Commit**

```bash
git add frontend/src/components/PrimaryButton.vue
git commit -m "feat(ui): enhance PrimaryButton with gradient, shadow and smooth transitions"
```

---

## Task 5: Update GoalInputCard Component

**Files:**
- Modify: `frontend/src/components/GoalInputCard.vue`

**Step 1: Update template**

Add wrapper div for animation:

```vue
<template>
  <div class="goal-card animate-fade-in-up">
    <!-- existing content -->
  </div>
</template>
```

**Step 2: Update styles**

Replace style section:

```vue
<style scoped>
.goal-card {
  background: linear-gradient(
    145deg,
    var(--color-bg-elevated),
    var(--color-bg-surface)
  );
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  padding: var(--space-xl);
  box-shadow:
    0 4px 24px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.05);
  transition: all var(--duration-normal) var(--ease-smooth);
}

.goal-card:hover {
  border-color: var(--color-border-hover);
  transform: translateY(-2px);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.goal-label {
  display: block;
  font-family: var(--font-display);
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: var(--space-md);
}

.goal-textarea {
  width: 100%;
  min-height: 120px;
  padding: var(--space-lg);
  font-family: var(--font-body);
  font-size: var(--font-size-md);
  color: var(--color-text);
  background: var(--color-bg);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  resize: vertical;
  transition: all var(--duration-normal) var(--ease-smooth);
}

.goal-textarea:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 4px var(--color-primary-alpha);
  outline: none;
}

.goal-textarea::placeholder {
  color: var(--color-text-muted);
}

.goal-hint {
  margin-top: var(--space-sm);
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.goal-error {
  margin-top: var(--space-sm);
  font-size: var(--font-size-sm);
  color: var(--color-error);
}
</style>
```

**Step 3: Commit**

```bash
git add frontend/src/components/GoalInputCard.vue
git commit -m "feat(ui): enhance GoalInputCard with card styling and animations"
```

---

## Task 6: Update CourseSelector Component

**Files:**
- Modify: `frontend/src/components/CourseSelector.vue`

**Step 1: Update styles**

```vue
<style scoped>
.selector-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

.selector-row {
  display: flex;
  gap: var(--space-md);
}

.selector-field {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.selector-label {
  font-family: var(--font-display);
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.selector-input {
  padding: 14px 18px;
  font-family: var(--font-body);
  font-size: var(--font-size-md);
  color: var(--color-text);
  background: var(--color-bg);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  transition: all var(--duration-normal) var(--ease-smooth);
}

.selector-input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 4px var(--color-primary-alpha);
  outline: none;
}

.selector-input::placeholder {
  color: var(--color-text-muted);
}
</style>
```

**Step 2: Commit**

```bash
git add frontend/src/components/CourseSelector.vue
git commit -m "feat(ui): enhance CourseSelector with improved input styling"
```

---

## Task 7: Update StepProgress Component

**Files:**
- Modify: `frontend/src/components/StepProgress.vue`

**Step 1: Update styles**

```vue
<style scoped>
.progress-container {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-lg);
  background: linear-gradient(
    145deg,
    var(--color-bg-elevated),
    var(--color-bg-surface)
  );
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  overflow-x: auto;
}

.step-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-sm);
  min-width: 80px;
}

.step-indicator {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-family: var(--font-display);
  font-weight: 600;
  font-size: var(--font-size-sm);
  transition: all var(--duration-normal) var(--ease-smooth);
}

.step-pending {
  background: var(--color-bg);
  border: 2px solid var(--color-border);
  color: var(--color-text-muted);
}

.step-active {
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-hover));
  border: 2px solid var(--color-primary);
  color: #fff;
  box-shadow: 0 4px 16px rgba(107, 159, 255, 0.4);
}

.step-done {
  background: var(--color-success);
  border: 2px solid var(--color-success);
  color: #fff;
}

.step-title {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  text-align: center;
  white-space: nowrap;
}

.step-line {
  flex: 1;
  height: 2px;
  background: var(--color-border);
  min-width: 20px;
}

.step-line.completed {
  background: linear-gradient(90deg, var(--color-success), var(--color-primary));
}
</style>
```

**Step 2: Commit**

```bash
git add frontend/src/components/StepProgress.vue
git commit -m "feat(ui): enhance StepProgress with gradient indicators and animations"
```

---

## Task 8: Update PageHeader Component

**Files:**
- Modify: `frontend/src/components/PageHeader.vue`

**Step 1: Update styles**

```vue
<style scoped>
.header {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.eyebrow {
  font-family: var(--font-body);
  font-size: var(--font-size-xs);
  font-weight: 500;
  color: var(--color-accent-coral);
  text-transform: uppercase;
  letter-spacing: 1.5px;
}

.title {
  font-family: var(--font-display);
  font-size: var(--font-size-2xl);
  font-weight: 600;
  color: var(--color-text);
  line-height: 1.2;
}

.subtitle {
  font-family: var(--font-body);
  font-size: var(--font-size-md);
  color: var(--color-text-secondary);
  line-height: 1.5;
}
</style>
```

**Step 2: Commit**

```bash
git add frontend/src/components/PageHeader.vue
git commit -m "feat(ui): enhance PageHeader with accent color and typography"
```

---

## Task 9: Update HomeView Page Styles

**Files:**
- Modify: `frontend/src/views/HomeView.vue:300-324`

**Step 1: Update scoped styles**

Replace the style section:

```vue
<style scoped>
.home-page {
  min-height: 100dvh;
  padding: clamp(20px, 4vw, 40px);
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  gap: clamp(18px, 3vw, 32px);
}

.home-toolbar {
  grid-column: 1 / -1;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: var(--space-md);
}

.username {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.hero-panel,
.form-panel {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  background: linear-gradient(
    160deg,
    var(--color-bg-elevated),
    var(--color-bg-surface)
  );
  box-shadow: var(--shadow-md);
  transition: all var(--duration-normal) var(--ease-smooth);
}

.hero-panel {
  padding: clamp(20px, 4vw, 40px);
  display: flex;
  flex-direction: column;
  gap: var(--space-xl);
}

.hero-panel:hover {
  box-shadow: var(--shadow-lg);
}

.form-panel {
  padding: clamp(18px, 3vw, 28px);
}

.start-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

.action-block {
  border-top: 1px solid var(--color-border);
  padding-top: var(--space-lg);
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.history-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
  background: rgba(12, 21, 42, 0.8);
  display: grid;
  gap: var(--space-md);
}

.history-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-sm);
}

.history-list {
  display: grid;
  gap: var(--space-md);
}

.history-item {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-md);
  background: var(--color-bg);
  transition: all var(--duration-normal) var(--ease-smooth);
}

.history-item:hover {
  border-color: var(--color-border-hover);
  transform: translateY(-2px);
  box-shadow: var(--shadow-sm);
}

.history-item-head {
  display: flex;
  justify-content: space-between;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  margin-bottom: var(--space-xs);
}

.history-goal {
  color: var(--color-text);
  margin-bottom: var(--space-xs);
}

.history-meta {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  margin-bottom: var(--space-sm);
}

.history-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.history-time {
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
}

.history-tip {
  margin: 0;
  color: var(--color-text-secondary);
}

.history-error {
  margin: 0;
  color: var(--color-error);
}

.ghost-btn {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-surface);
  color: var(--color-text-secondary);
  padding: 8px 14px;
  font-size: var(--font-size-sm);
  transition: all var(--duration-fast) var(--ease-smooth);
}

.ghost-btn:hover:not(:disabled) {
  background: var(--color-bg-hover);
  border-color: var(--color-border-hover);
  color: var(--color-text);
}

.ghost-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.submit-error {
  margin: 0;
  color: var(--color-error);
  font-size: var(--font-size-sm);
}

@media (max-width: 980px) {
  .home-page {
    grid-template-columns: 1fr;
  }
}
</style>
```

**Step 2: Add animation to template**

Add classes to history items:
```vue
<article v-for="item in recentHistory" :key="item.sessionId" class="history-item animate-fade-in-up">
```

**Step 3: Commit**

```bash
git add frontend/src/views/HomeView.vue
git commit -m "feat(ui): enhance HomeView with card hover effects and animations"
```

---

## Task 10: Update SessionView Page Styles

**Files:**
- Modify: `frontend/src/views/SessionView.vue:341-361`

**Step 1: Update scoped styles**

```vue
<style scoped>
.workflow-page {
  min-height: 100dvh;
  padding: clamp(16px, 2.8vw, 30px);
}

.toolbar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
  flex-wrap: wrap;
}

.workflow-id {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.workflow-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-xl);
  max-width: 960px;
  margin: 0 auto;
}

.step-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  background: linear-gradient(
    165deg,
    var(--color-bg-elevated),
    var(--color-bg-surface)
  );
  padding: clamp(16px, 2.8vw, 26px);
  box-shadow: var(--shadow-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
  transition: all var(--duration-normal) var(--ease-smooth);
}

.step-card:hover {
  box-shadow: var(--shadow-lg);
}

.step-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-sm);
}

.status-tag {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  padding: 6px 14px;
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  background: var(--color-bg);
}

.panel {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-bg);
  padding: var(--space-lg);
}

.path-grid {
  display: grid;
  gap: var(--space-md);
}

.path-item {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-lg);
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-smooth);
  background: var(--color-bg-surface);
}

.path-item:hover {
  border-color: var(--color-border-hover);
  transform: translateY(-2px);
}

.path-item.selected {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-alpha);
}

.task-list {
  display: grid;
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
}

.task-item {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  background: var(--color-bg-surface);
}

.task-toggle {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--color-bg-elevated);
  border: none;
  color: var(--color-text);
  padding: var(--space-md);
  font-size: var(--font-size-md);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-smooth);
}

.task-toggle:hover {
  background: var(--color-bg-hover);
}

.task-body {
  padding: var(--space-lg);
  display: grid;
  gap: var(--space-md);
  background: var(--color-bg);
}

.mastery-list {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-md);
  margin: var(--space-md) 0;
  background: var(--color-bg-surface);
}

.actions {
  display: grid;
  grid-template-columns: 140px 1fr;
  gap: var(--space-lg);
  padding-top: var(--space-lg);
  border-top: 1px solid var(--color-border);
}

.ghost-button {
  min-height: 48px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  background: var(--color-bg-surface);
  font-size: var(--font-size-md);
  transition: all var(--duration-fast) var(--ease-smooth);
}

.ghost-button:hover:not(:disabled) {
  background: var(--color-bg-hover);
  border-color: var(--color-border-hover);
  color: var(--color-text);
}

.ghost-button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

@media (max-width: 900px) {
  .actions {
    grid-template-columns: 1fr;
  }
}
</style>
```

**Step 2: Commit**

```bash
git add frontend/src/views/SessionView.vue
git commit -m "feat(ui): enhance SessionView with improved card and button styles"
```

---

## Task 11: Update ErrorMessage and LoadingSpinner Components

**Files:**
- Modify: `frontend/src/components/ErrorMessage.vue`
- Modify: `frontend/src/components/LoadingSpinner.vue`

**Step 1: Update ErrorMessage styles**

```vue
<style scoped>
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-lg);
  padding: var(--space-xxl);
  background: linear-gradient(
    145deg,
    var(--color-bg-elevated),
    var(--color-bg-surface)
  );
  border: 1px solid rgba(255, 122, 138, 0.3);
  border-radius: var(--radius-xl);
  text-align: center;
}

.error-icon {
  width: 48px;
  height: 48px;
  color: var(--color-error);
}

.error-message {
  color: var(--color-text);
  font-size: var(--font-size-md);
}

.error-retry {
  padding: 10px 20px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  transition: all var(--duration-fast) var(--ease-smooth);
}

.error-retry:hover {
  background: var(--color-bg-hover);
  border-color: var(--color-border-hover);
  color: var(--color-text);
}
</style>
```

**Step 2: Update LoadingSpinner styles**

```vue
<style scoped>
.spinner {
  display: inline-block;
  width: 32px;
  height: 32px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
```

**Step 3: Commit**

```bash
git add frontend/src/components/ErrorMessage.vue frontend/src/components/LoadingSpinner.vue
git commit -m "feat(ui): enhance ErrorMessage and LoadingSpinner with updated styles"
```

---

## Task 12: Verify and Test

**Step 1: Run frontend build**

```bash
cd frontend && npm run build
```

Expected: Build completes without errors

**Step 2: Start dev server**

```bash
cd frontend && npm run dev
```

Expected: Dev server starts, open http://localhost:5173 to verify visual changes

**Step 3: Commit final**

```bash
git add .
git commit -m "feat(ui): complete frontend UI redesign - warm friendly theme"
```

---

## Plan Complete

Two execution options:

**1. Subagent-Driven (this session)** - I dispatch fresh subagent per task, review between tasks, fast iteration

**2. Parallel Session (separate)** - Open new session with executing-plans, batch execution with checkpoints

Which approach?
