export interface ApiError {
  message: string;
  code?: string;
}

export interface CodeLabel {
  code: string;
  label: string;
}

export interface ResponseMetadata {
  schemaVersion: string;
  generatedAt: string;
  traceId?: string;
  requestId?: string;
  strategy?: string;
}

export interface ApiEnvelope<T> {
  code: string;
  message: string;
  data: T;
  metadata: ResponseMetadata;
}

export interface SelectOption {
  label: string;
  value: string;
}

export type AsyncState = 'idle' | 'loading' | 'success' | 'error';
