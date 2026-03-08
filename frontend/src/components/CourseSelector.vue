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
  <section class="selector-card">
    <div class="field-group">
      <label for="course-input" class="label">2. 课程（可自定义）</label>
      <input
        id="course-input"
        class="input"
        type="text"
        :value="courseId"
        list="course-suggestions"
        placeholder="例如：computer_network / linear_algebra"
        @input="onCourseInput"
      />
      <datalist id="course-suggestions">
        <option v-for="course in suggestedCourses" :key="course" :value="course" />
      </datalist>
    </div>

    <div class="field-group">
      <label for="chapter-input" class="label">3. 章节（可自定义）</label>
      <input
        id="chapter-input"
        class="input"
        type="text"
        :value="chapterId"
        list="chapter-suggestions"
        placeholder="例如：tcp / vector_space"
        @input="onChapterInput"
      />
      <datalist id="chapter-suggestions">
        <option v-for="chapter in suggestedChapters" :key="chapter" :value="chapter" />
      </datalist>
    </div>
  </section>
</template>

<style scoped>
.selector-card {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-md);
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.label {
  font-size: var(--font-size-sm);
  font-weight: 600;
}

.input {
  width: 100%;
  min-height: 44px;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: #0a1225;
  color: var(--color-text);
  padding: 0 var(--space-md);
}

.input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-alpha);
}

@media (max-width: 768px) {
  .selector-card {
    grid-template-columns: 1fr;
  }
}
</style>
