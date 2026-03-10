<script setup lang="ts">
import { computed } from 'vue'
import { courseOptions } from '@/constants/courses'

interface CourseSelectorProps {
  courseId: string
  chapterId: string
}

const props = defineProps<CourseSelectorProps>()
const emit = defineEmits<{
  'update:courseId': [value: string]
  'update:chapterId': [value: string]
}>()

const suggestedCourses = computed(() => courseOptions.map((course) => course.id))
const suggestedChapters = computed(() => {
  const ids = new Set<string>()
  for (const course of courseOptions) {
    if (props.courseId && course.id !== props.courseId) continue
    for (const chapter of course.chapters) {
      ids.add(chapter.id)
    }
  }
  return Array.from(ids)
})

function onCourseInput(event: Event) {
  emit('update:courseId', (event.target as HTMLInputElement).value)
}

function onChapterInput(event: Event) {
  emit('update:chapterId', (event.target as HTMLInputElement).value)
}
</script>

<template>
  <section class="selector-group">
    <div class="selector-row">
      <div class="selector-field">
        <label for="course-input" class="selector-label">课程</label>
        <input
          id="course-input"
          class="selector-input"
          type="text"
          :value="courseId"
          list="course-suggestions"
          placeholder="例如：computer_network"
          @input="onCourseInput"
        />
        <datalist id="course-suggestions">
          <option v-for="course in suggestedCourses" :key="course" :value="course" />
        </datalist>
      </div>

      <div class="selector-field">
        <label for="chapter-input" class="selector-label">章节</label>
        <input
          id="chapter-input"
          class="selector-input"
          type="text"
          :value="chapterId"
          list="chapter-suggestions"
          placeholder="例如：tcp"
          @input="onChapterInput"
        />
        <datalist id="chapter-suggestions">
          <option v-for="chapter in suggestedChapters" :key="chapter" :value="chapter" />
        </datalist>
      </div>
    </div>
  </section>
</template>

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
}

.selector-input {
  padding: 14px 18px;
  font-family: var(--font-body);
  font-size: var(--font-size-md);
  color: var(--color-text);
  background: rgba(6, 10, 18, 0.92);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.selector-input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 4px var(--color-primary-alpha);
  outline: none;
}

.selector-input::placeholder {
  color: var(--color-text-muted);
}

@media (max-width: 768px) {
  .selector-row {
    flex-direction: column;
  }
}
</style>
