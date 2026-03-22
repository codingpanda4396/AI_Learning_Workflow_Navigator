<template>
  <div class="workflow-page-scaffold">
    <header v-if="$slots.title" class="mb-8">
      <slot name="title" />
    </header>
    <section class="mb-8">
      <slot name="primary" />
    </section>
    <section
      v-if="$slots.secondary"
      class="border-t border-border pt-6"
    >
      <slot name="secondary" />
    </section>
  </div>
</template>

<script setup lang="ts"></script>
