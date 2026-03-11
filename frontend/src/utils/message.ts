export function emitError(message: string) {
  window.dispatchEvent(new CustomEvent('app:message', { detail: { type: 'error', text: message } }));
}

export function emitInfo(message: string) {
  window.dispatchEvent(new CustomEvent('app:message', { detail: { type: 'info', text: message } }));
}
