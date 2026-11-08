<script setup lang="ts">
withDefaults(defineProps<{ withText?: boolean }>(), { withText: true })
</script>

<template>
  <span class="inline-flex items-center gap-2 font-display text-xl font-bold text-ink">
    <svg viewBox="0 0 32 32" class="size-7" aria-hidden="true">
      <rect width="32" height="32" rx="9" class="fill-primary" />
      <path
        d="M10 16a6 6 0 1 1 6 6h-3"
        fill="none"
        stroke="white"
        stroke-width="3"
        stroke-linecap="round"
      />
      <path d="M15 19l-3 3 3 3" fill="none" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" />
    </svg>
    <span v-if="withText">Loopa</span>
  </span>
</template>
