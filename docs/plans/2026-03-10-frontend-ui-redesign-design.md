# Frontend UI Redesign Design

## Project Overview

Redesign the AI Learning Workflow Navigator frontend with a warm and friendly visual style, addressing:

- Color contrast issues
- Overly simple component styles
- Missing animation effects

## Design Direction

**Theme: Warm & Friendly**

- Keep dark theme base but add warm accents
- Softer rounded corners and gentle gradients
- Subtle micro-interactions that enhance without distracting

---

## 1. Color Palette

### New CSS Variables

```css
:root {
  /* Primary - Warm Blue (softer, more inviting) */
  --color-primary: #6B9FFF;
  --color-primary-hover: #8AB6FF;
  --color-primary-active: #5A8AE8;
  --color-primary-alpha: rgba(107, 159, 255, 0.18);

  /* Accent Colors - Warm & Friendly */
  --color-accent-coral: #FF8A7A;    /* 珊瑚橙 - 强调色 */
  --color-accent-lavender: #B8A9FF; /* 淡紫 - 次要强调 */
  --color-accent-mint: #7EDFC5;      /* 薄荷绿 - 成功状态 */
  --color-accent-peach: #FFB88C;     /* 蜜桃橙 - 警告状态 */

  /* Background - Adjusted for better contrast */
  --color-bg: #0D1117;              /* 更深的背景 */
  --color-bg-elevated: #151D2B;      /* 提升的卡片层 */
  --color-bg-surface: #1C2636;      /* 表面层 */
  --color-bg-hover: #252F42;         /* 悬停状态 */

  /* Text - Improved contrast */
  --color-text: #F0F4F8;             /* 主要文字 - 更亮 */
  --color-text-secondary: #A3B4CC;   /* 次要文字 - 提升对比度 */
  --color-text-muted: #6B7A94;       /* 弱化文字 */

  /* Border - Softer but visible */
  --color-border: #2A3A52;
  --color-border-hover: #3D5068;

  /* Status Colors */
  --color-success: #5DD4A6;
  --color-warning: #FFB88C;
  --color-error: #FF7A8A;
  --color-info: #7EB8FF;
}
```

### Rationale

- Primary color shifted to warmer blue (#6B9FFF) - more inviting than cool blue
- Added coral, lavender, mint, peach accents for visual interest
- Text contrast ratio improved from ~4.5:1 to ~6:1 for accessibility
- Background layers have clearer distinction

---

## 2. Typography

### Font Updates

```css
:root {
  /* Keep existing fonts but adjust weights */
  --font-display: 'Space Grotesk', sans-serif;
  --font-body: 'Manrope', sans-serif;

  /* Add font weight variables */
  --font-weight-regular: 400;
  --font-weight-medium: 500;
  --font-weight-semibold: 600;
  --font-weight-bold: 700;
}
```

### Display Hierarchy

| Element | Font | Size | Weight |
|---------|------|------|--------|
| H1 | Space Grotesk | 2.5rem (40px) | 600 |
| H2 | Space Grotesk | 1.75rem (28px) | 600 |
| H3 | Space Grotesk | 1.25rem (20px) | 600 |
| Body | Manrope | 1rem (16px) | 400 |
| Small | Manrope | 0.875rem (14px) | 400 |
| Caption | Manrope | 0.75rem (12px) | 500 |

---

## 3. Component Styles

### Cards

```css
.card {
  background: linear-gradient(
    145deg,
    var(--color-bg-elevated),
    var(--color-bg-surface)
  );
  border: 1px solid var(--color-border);
  border-radius: 20px; /* Larger radius - friendlier */
  padding: 24px;
  box-shadow:
    0 4px 24px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.05); /* Subtle top highlight */
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.card:hover {
  border-color: var(--color-border-hover);
  transform: translateY(-2px);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.08);
}
```

### Buttons

**Primary Button:**
```css
.btn-primary {
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-hover));
  border: none;
  border-radius: 12px;
  padding: 12px 24px;
  font-weight: 600;
  color: #fff;
  box-shadow:
    0 4px 16px rgba(107, 159, 255, 0.35),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
  transition: all 0.25s ease;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow:
    0 6px 24px rgba(107, 159, 255, 0.45),
    inset 0 1px 0 rgba(255, 255, 255, 0.25);
}

.btn-primary:active {
  transform: translateY(0);
  box-shadow:
    0 2px 8px rgba(107, 159, 255, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.15);
}
```

**Secondary/Ghost Button:**
```css
.btn-ghost {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 10px 20px;
  color: var(--color-text-secondary);
  transition: all 0.2s ease;
}

.btn-ghost:hover {
  background: var(--color-bg-hover);
  border-color: var(--color-border-hover);
  color: var(--color-text);
}
```

### Input Fields

```css
.input {
  background: var(--color-bg);
  border: 1.5px solid var(--color-border);
  border-radius: 12px;
  padding: 14px 18px;
  color: var(--color-text);
  transition: all 0.25s ease;
}

.input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 4px var(--color-primary-alpha);
}

.input::placeholder {
  color: var(--color-text-muted);
}
```

### Status Tags

```css
.tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 500;
}

.tag-success {
  background: rgba(93, 212, 166, 0.15);
  color: var(--color-success);
  border: 1px solid rgba(93, 212, 166, 0.3);
}

.tag-warning {
  background: rgba(255, 184, 140, 0.15);
  color: var(--color-warning);
  border: 1px solid rgba(255, 184, 140, 0.3);
}

.tag-error {
  background: rgba(255, 122, 138, 0.15);
  color: var(--color-error);
  border: 1px solid rgba(255, 122, 138, 0.3);
}
```

---

## 4. Animation Specifications

### Global Transitions

```css
:root {
  /* Timing functions */
  --ease-smooth: cubic-bezier(0.4, 0, 0.2, 1);
  --ease-bounce: cubic-bezier(0.68, -0.55, 0.265, 1.55);
  --ease-gentle: cubic-bezier(0.25, 0.1, 0.25, 1);

  /* Duration */
  --duration-instant: 150ms;
  --duration-fast: 200ms;
  --duration-normal: 300ms;
  --duration-slow: 500ms;
}
```

### Animations

**1. Fade In Up (for cards entering):**
```css
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
```

**2. Pulse (for loading states):**
```css
@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}
```

**3. Shimmer (for skeleton loading):**
```css
@keyframes shimmer {
  0% {
    background-position: -200% 0;
  }
  100% {
    background-position: 200% 0;
  }
}
```

**4. Scale In (for buttons, small elements):**
```css
@keyframes scaleIn {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}
```

### Usage Examples

```css
/* Card entrance animation */
.card-animate {
  animation: fadeInUp 0.4s var(--ease-smooth) forwards;
}

/* Staggered children */
.card-animate .child:nth-child(1) { animation-delay: 0ms; }
.card-animate .child:nth-child(2) { animation-delay: 80ms; }
.card-animate .child:nth-child(3) { animation-delay: 160ms; }

/* Button hover */
.btn:hover {
  animation: scaleIn 0.2s var(--ease-smooth);
}
```

---

## 5. Page-Specific Updates

### HomeView

1. **Hero Panel:**
   - Add subtle gradient border glow
   - Animate StepProgress on load
   - History cards with hover lift effect

2. **Form Panel:**
   - Input fields with floating labels effect
   - Submit button with loading state animation
   - Error messages with shake animation

### SessionView

1. **Step Progress:**
   - Animated progress bar fills
   - Step indicators with pulse effect when active

2. **Step Cards:**
   - Smooth expand/collapse transitions
   - Path selection cards with checkmark animation

3. **Task List:**
   - Accordion with smooth height transition
   - Status badges with subtle glow

### HistoryView

1. **Session Cards:**
   - Hover lift effect
   - Progress bar with gradient fill animation

---

## 6. Background & Atmosphere

### Enhanced Background

```css
body {
  background-color: var(--color-bg);
  background-image:
    /* Subtle warm gradient overlay */
    radial-gradient(ellipse at 20% 30%, rgba(107, 159, 255, 0.08) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 70%, rgba(255, 138, 122, 0.06) 0%, transparent 50%),
    radial-gradient(ellipse at 50% 100%, rgba(184, 169, 255, 0.05) 0%, transparent 40%),
    /* Base gradient */
    linear-gradient(180deg, #0D1117 0%, #0A0E14 100%);
  background-attachment: fixed;
}
```

---

## 7. Accessibility Improvements

| Area | Change |
|------|--------|
| Contrast | Text ratio ≥ 4.5:1 (primary), ≥ 3:1 (secondary) |
| Focus | Visible focus rings with primary color |
| Motion | Respect `prefers-reduced-motion` |
| Touch | Minimum 44px touch targets |

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

---

## 8. Implementation Priority

1. **Phase 1 - Foundation:**
   - Update CSS variables (colors, spacing, typography)
   - Update global styles and background

2. **Phase 2 - Components:**
   - Card styles
   - Button styles
   - Input styles
   - Status tags

3. **Phase 3 - Animations:**
   - Global animation keyframes
   - Apply to cards and buttons
   - Page-specific transitions

4. **Phase 4 - Polish:**
   - Hover/focus states
   - Responsive adjustments
   - Accessibility review
