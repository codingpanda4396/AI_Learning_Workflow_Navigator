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

const chapterOptions = computed(() => {
  const selectedCourse = courseOptions.find((course) => course.id === props.courseId)
  return selectedCourse?.chapters ?? []
})

function onCourseChange(event: Event) {
  const nextCourseId = (event.target as HTMLSelectElement).value
  emit('update:courseId', nextCourseId)
  const fallbackChapterId =
    courseOptions.find((course) => course.id === nextCourseId)?.chapters[0]?.id ?? ''
  emit('update:chapterId', fallbackChapterId)
}

function onChapterChange(event: Event) {
  emit('update:chapterId', (event.target as HTMLSelectElement).value)
}
</script>

<template>
  <section class="selector-card">
    <div class="field-group">
      <label for="course-select" class="label">2. 课程</label>
      <select id="course-select" class="select" :value="courseId" @change="onCourseChange">
        <option v-for="course in courseOptions" :key="course.id" :value="course.id">
          {{ course.name }}
        </option>
      </select>
    </div>

    <div class="field-group">
      <label for="chapter-select" class="label">3. 章节</label>
      <select id="chapter-select" class="select" :value="chapterId" @change="onChapterChange">
        <option v-for="chapter in chapterOptions" :key="chapter.id" :value="chapter.id">
          {{ chapter.name }}
        </option>
      </select>
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

.select {
  width: 100%;
  min-height: 44px;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: #0a1225;
  color: var(--color-text);
  padding: 0 var(--space-md);
}

.select:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-alpha);
}

@media (max-width: 768px) {
  .selector-card {
    grid-template-columns: 1fr;
  }
}
</style>
