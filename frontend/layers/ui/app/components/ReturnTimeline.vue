<script setup lang="ts">
// Historique d'un retour (admin) : qui a fait quoi, quand. Les notes internes sont mises en avant.
defineProps<{ entries: Array<{ type: string, actor: string, message: string | null, occurredAt: string }> }>()
const { t, locale } = useI18n()

const ICONS: Record<string, string> = {
  SUBMITTED: 'i-lucide-inbox',
  APPROVED: 'i-lucide-check',
  REJECTED: 'i-lucide-x',
  SHIPPED: 'i-lucide-truck',
  RECEIVED: 'i-lucide-package-check',
  RESOLVED: 'i-lucide-badge-check',
  RESOLUTION_FAILED: 'i-lucide-triangle-alert',
  RESOLUTION_RETRY: 'i-lucide-rotate-cw',
  CANCELLED: 'i-lucide-ban',
  NOTE: 'i-lucide-sticky-note'
}

function iconFor(type: string) {
  return ICONS[type] ?? 'i-lucide-circle'
}

function actorLabel(actor: string) {
  if (actor === 'CUSTOMER') return t('timeline.customer')
  if (actor === 'SYSTEM') return 'Loopa'
  return actor
}
</script>

<template>
  <ol class="relative space-y-5 border-s border-default ps-6">
    <li v-for="(e, i) in entries" :key="i" class="relative">
      <span
        class="absolute -start-[34px] grid size-6 place-items-center rounded-full ring-4 ring-default"
        :class="e.type === 'NOTE' ? 'bg-amber-100 text-amber-800'
          : e.type === 'RESOLUTION_FAILED' || e.type === 'REJECTED' ? 'bg-red-100 text-red-700'
            : 'bg-primary/10 text-primary'"
      >
        <UIcon :name="iconFor(e.type)" class="size-3.5" />
      </span>
      <p class="text-sm">
        <strong>{{ t(`timeline.${e.type}`) }}</strong>
        <span class="text-muted"> · {{ actorLabel(e.actor) }}</span>
      </p>
      <p
        v-if="e.message"
        class="mt-1 text-sm"
        :class="e.type === 'NOTE' ? 'rounded-lg bg-amber-50 px-3 py-2 text-amber-950' : 'text-muted'"
      >
        {{ e.message }}
      </p>
      <p class="mt-0.5 text-xs text-muted">
        {{ formatDate(e.occurredAt, locale, true) }}
      </p>
    </li>
  </ol>
</template>
