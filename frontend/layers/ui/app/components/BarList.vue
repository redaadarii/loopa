<script setup lang="ts">
// Liste de barres horizontales : valeur absolue + part du total, lisible sans légende.
type Item = { label: string, count: number }
const props = defineProps<{ items: Item[], unit?: string }>()
const list = computed(() => props.items as Item[])
const total = computed(() => list.value.reduce((s: number, i: Item) => s + i.count, 0) || 1)
const max = computed(() => Math.max(...list.value.map((i: Item) => i.count), 1))
</script>

<template>
  <ul class="space-y-3.5">
    <li v-for="item in items" :key="item.label">
      <div class="mb-1 flex items-baseline justify-between gap-3 text-sm">
        <span class="truncate">{{ item.label }}</span>
        <span class="shrink-0 tabular-nums text-muted">
          <strong class="text-highlighted">{{ item.count }}</strong>
          {{ unit }} · {{ Math.round(item.count / total * 100) }} %
        </span>
      </div>
      <div class="h-2 rounded-full bg-elevated">
        <div class="h-2 rounded-full bg-primary" :style="{ width: `${item.count / max * 100}%` }" />
      </div>
    </li>
  </ul>
</template>
