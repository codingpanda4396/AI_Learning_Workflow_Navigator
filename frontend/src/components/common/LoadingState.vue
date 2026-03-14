<template>
  <div class="app-loading">
    <div class="mx-auto max-w-xl">
      <div class="mx-auto mb-4 h-11 w-11 rounded-2xl bg-slate-100 p-3">
        <div class="app-skeleton h-full rounded-xl" />
      </div>
      <p class="text-base font-semibold text-slate-900">正在准备这一页</p>
      <p class="mt-2 text-sm text-slate-600">内容会在几秒内就绪，不需要重复操作。</p>
      <div class="mx-auto mt-6 grid max-w-md gap-3">
        <div class="app-skeleton h-4 rounded-full" />
        <div class="app-skeleton h-4 w-[82%] rounded-full" />
        <div class="app-skeleton h-20 rounded-[18px]" />
      </div>
    </div>
  </div>
</template>
