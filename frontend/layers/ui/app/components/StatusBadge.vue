<script setup lang="ts">
import type { ReturnStatus } from '../utils/returnStatus'

const props = defineProps<{ status: ReturnStatus }>()
const { t } = useI18n()
const meta = computed(() => RETURN_STATUS_META[props.status as ReturnStatus])
</script>

<template>
  <UBadge :color="meta.color" variant="subtle" :icon="meta.icon" class="rounded-full">
    {{ t(`status.${status}`) }}
  </UBadge>
</template>
