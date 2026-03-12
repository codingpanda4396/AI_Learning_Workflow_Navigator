export interface ActiveSession {
  id: string;
  goal: string;
  course: string;
  chapter: string;
  phase: string;
  currentTask?: string;
  progress?: number;
}

export interface StartLearningForm {
  goal: string;
  course: string;
  chapter: string;
}
