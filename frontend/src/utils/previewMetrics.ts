const STORAGE_KEY = 'plan_preview_metrics_v1_5';

interface PreviewMetricsState {
  previewShown: number;
  previewAccepted: number;
  firstTaskCompleted: number;
  pendingFirstTaskId?: number;
  lastPreviewId?: string;
}

const defaultState: PreviewMetricsState = {
  previewShown: 0,
  previewAccepted: 0,
  firstTaskCompleted: 0,
};

function readState(): PreviewMetricsState {
  if (typeof window === 'undefined') {
    return { ...defaultState };
  }
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return { ...defaultState };
    }
    const parsed = JSON.parse(raw) as Partial<PreviewMetricsState>;
    return {
      previewShown: Number(parsed.previewShown ?? 0),
      previewAccepted: Number(parsed.previewAccepted ?? 0),
      firstTaskCompleted: Number(parsed.firstTaskCompleted ?? 0),
      pendingFirstTaskId: parsed.pendingFirstTaskId,
      lastPreviewId: parsed.lastPreviewId,
    };
  } catch {
    return { ...defaultState };
  }
}

function writeState(state: PreviewMetricsState) {
  if (typeof window === 'undefined') {
    return;
  }
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

function safeRate(numerator: number, denominator: number): number {
  if (denominator <= 0) {
    return 0;
  }
  return Math.round((numerator / denominator) * 1000) / 10;
}

export function trackPreviewShown(previewId?: string) {
  if (!previewId) {
    return;
  }
  const state = readState();
  if (state.lastPreviewId === previewId) {
    return;
  }
  state.previewShown += 1;
  state.lastPreviewId = previewId;
  writeState(state);
}

export function trackPreviewAccepted(previewId: string, firstTaskId?: number) {
  const state = readState();
  state.previewAccepted += 1;
  state.lastPreviewId = previewId;
  if (typeof firstTaskId === 'number' && Number.isFinite(firstTaskId) && firstTaskId > 0) {
    state.pendingFirstTaskId = firstTaskId;
  }
  writeState(state);
}

export function trackFirstTaskCompleted(taskId: number) {
  if (!Number.isFinite(taskId) || taskId <= 0) {
    return;
  }
  const state = readState();
  if (state.pendingFirstTaskId !== taskId) {
    return;
  }
  state.firstTaskCompleted += 1;
  state.pendingFirstTaskId = undefined;
  writeState(state);
}

export function getPreviewMetricsSnapshot() {
  const state = readState();
  return {
    previewShown: state.previewShown,
    previewAccepted: state.previewAccepted,
    firstTaskCompleted: state.firstTaskCompleted,
    previewAcceptedRate: safeRate(state.previewAccepted, state.previewShown),
    firstTaskCompletionRate: safeRate(state.firstTaskCompleted, state.previewAccepted),
  };
}
