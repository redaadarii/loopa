<script setup lang="ts">
defineProps<{
  label: string
  value: string
  hint?: string
  icon: string
  tone?: 'default' | 'accent'
}>()
</script>

<template>
  <div
    class="rounded-xl border p-5"
    :class="tone === 'accent' ? 'border-coral-200 bg-coral-50' : 'border-default bg-default'"
  >
    <div class="flex items-start justify-between gap-3">
      <p class="text-sm" :class="tone === 'accent' ? 'font-semibold text-coral-800' : 'text-muted'">
        {{ label }}
      </p>
      <UIcon :name="icon" class="size-5 shrink-0" :class="tone === 'accent' ? 'text-coral-700' : 'text-primary'" />
    </div>
    <p class="mt-2 font-display text-3xl font-extrabold tabular-nums">
      {{ value }}
    </p>
    <p v-if="hint" class="mt-1 text-sm" :class="tone === 'accent' ? 'text-coral-800' : 'text-muted'">
      {{ hint }}
    </p>
  </div>
</template>
