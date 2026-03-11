export interface ApiError {
  message: string;
  code?: string;
}

export interface SelectOption {
  label: string;
  value: string;
}

export type AsyncState = 'idle' | 'loading' | 'success' | 'error';
