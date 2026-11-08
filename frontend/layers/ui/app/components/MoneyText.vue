<script setup lang="ts">
// Affiche un montant stocké en unités mineures (centimes), formaté selon la langue courante.
const props = defineProps<{ amountMinor: number, currency: string }>()
const { locale } = useI18n()
const formatted = computed(() => formatMoney(props.amountMinor, props.currency, locale.value))
</script>

<template>
  <span class="tabular-nums whitespace-nowrap">{{ formatted }}</span>
</template>
