<script setup lang="ts">
// Histogramme des retours par jour (SVG, sans dépendance). Le dernier jour est mis en avant.
type Day = { date: string, count: number }
const props = defineProps<{ days: Day[] }>()
const list = computed(() => props.days as Day[])
const { locale } = useI18n()

const W = 640
const H = 160
const PAD_LEFT = 28
const PAD_BOTTOM = 22
const max = computed(() => Math.max(4, ...list.value.map((d: Day) => d.count)))
const ticks = computed(() => {
  const step = Math.ceil(max.value / 4)
  return [0, step, step * 2, step * 3, step * 4].filter(t => t <= max.value + step)
})
const top = computed(() => ticks.value.at(-1) ?? max.value)
const bar = computed(() => (W - PAD_LEFT) / Math.max(list.value.length, 1))
const y = (v: number) => (H - PAD_BOTTOM) * (1 - v / top.value)
const label = (iso: string) =>
  new Intl.DateTimeFormat(intlLocale(locale.value), { day: 'numeric', month: 'short', numberingSystem: 'latn' })
    .format(new Date(iso))
</script>

<template>
  <svg :viewBox="`0 0 ${W} ${H}`" class="h-auto w-full" role="img" :aria-label="$t('admin.dailyChart')">
    <g class="text-muted">
      <g v-for="t in ticks" :key="t">
        <line :x1="PAD_LEFT" :x2="W" :y1="y(t)" :y2="y(t)" stroke="currentColor" stroke-opacity="0.15" />
        <text :x="PAD_LEFT - 6" :y="y(t) + 3" text-anchor="end" font-size="10" fill="currentColor">{{ t }}</text>
      </g>
    </g>
    <g>
      <rect
        v-for="(d, i) in list"
        :key="d.date"
        :x="PAD_LEFT + i * bar + bar * 0.18"
        :y="y(d.count)"
        :width="bar * 0.64"
        :height="Math.max(0, H - PAD_BOTTOM - y(d.count))"
        rx="2"
        :fill="i === list.length - 1 ? 'var(--ui-secondary)' : 'var(--ui-primary)'"
      >
        <title>{{ label(d.date) }} : {{ d.count }}</title>
      </rect>
    </g>
    <g class="text-muted" font-size="10" fill="currentColor">
      <text
        v-for="(d, i) in list"
        v-show="i % Math.ceil(list.length / 6) === 0 || i === list.length - 1"
        :key="`l-${d.date}`"
        :x="PAD_LEFT + i * bar + bar / 2"
        :y="H - 6"
        text-anchor="middle"
      >{{ label(d.date) }}</text>
    </g>
  </svg>
</template>
