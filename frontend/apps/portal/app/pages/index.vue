<script setup lang="ts">
// Racine du portail : chaque boutique a son propre lien (/<boutique>). Démo : /atlas.
const config = useRuntimeConfig()
const { t } = useI18n()
</script>

<template>
  <div class="grid min-h-dvh place-items-center bg-canvas px-6 text-center">
    <div class="max-w-sm space-y-4">
      <LoopaLogo />
      <h1 class="text-2xl font-bold">
        {{ t('portal.shopNotFound') }}
      </h1>
      <p class="text-muted">
        {{ t('portal.shopNotFoundText') }}
      </p>
      <UButton v-if="config.public.demoShop" :to="`/${config.public.demoShop}`" label="Atlas Store (démo)" trailing-icon="i-lucide-arrow-right" />
    </div>
  </div>
</template>
