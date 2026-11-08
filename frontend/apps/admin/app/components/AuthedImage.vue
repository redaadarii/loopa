<script setup lang="ts">
// Photo jointe à un retour : téléchargée avec le jeton du marchand (jamais publique).
const props = defineProps<{ fileId: string }>()
const { url, error } = useAuthedFile(() => props.fileId)
</script>

<template>
  <a v-if="url" :href="url" target="_blank" rel="noopener" class="block">
    <img :src="url" alt="" class="size-28 rounded-lg object-cover ring-1 ring-default transition hover:opacity-90">
  </a>
  <div v-else class="grid size-28 place-items-center rounded-lg bg-elevated text-muted">
    <UIcon :name="error ? 'i-lucide-image-off' : 'i-lucide-loader-circle'" :class="['size-6', !error && 'animate-spin']" />
  </div>
</template>
