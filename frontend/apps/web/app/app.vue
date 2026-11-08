<script setup lang="ts">
const { locale, locales } = useI18n()
const dir = computed(() => locales.value.find(l => l.code === locale.value)?.dir ?? 'ltr')
useHead({ htmlAttrs: { lang: locale, dir } })
</script>

<template>
  <UApp>
    <NuxtLayout>
      <NuxtPage />
    </NuxtLayout>
  </UApp>
</template>
