<script setup lang="ts">
// Enveloppe du portail d'une boutique : charge la marque (SSR) et applique sa couleur au design system.
const route = useRoute()
const { t, locale, locales, setLocale } = useI18n()
const portal = usePortalApi()
const shop = usePortalShop()
const handle = computed(() => String(route.params.shop))

const { error } = await useAsyncData(`shop-${handle.value}`, async () => {
  shop.value = await portal.shop(handle.value)
  return shop.value
})

useHead(() => ({
  title: shop.value ? `${shop.value.name} — ${t('portal.title')}` : t('portal.title'),
  style: shop.value ? [{ key: 'brand', innerHTML: `:root{--ui-primary:${shop.value.primaryColor}}` }] : []
}))

const languageItems = computed(() => locales.value.map(l => ({
  label: l.name ?? l.code,
  icon: l.code === locale.value ? 'i-lucide-check' : undefined,
  onSelect: () => setLocale(l.code)
})))
</script>

<template>
  <div class="flex min-h-dvh flex-col bg-canvas">
    <header class="sticky top-0 z-10 border-b border-default bg-white/90 backdrop-blur">
      <div class="mx-auto flex h-14 max-w-xl items-center justify-between px-4">
        <NuxtLink :to="`/${handle}`" class="flex items-center gap-2">
          <img v-if="shop?.logoUrl" :src="shop.logoUrl" :alt="shop.name" class="h-7">
          <span v-else class="font-display text-lg font-bold">{{ shop?.name }}</span>
        </NuxtLink>
        <UDropdownMenu :items="languageItems">
          <UButton icon="i-lucide-languages" color="neutral" variant="ghost" :label="locale.toUpperCase()" :aria-label="t('common.language')" />
        </UDropdownMenu>
      </div>
    </header>

    <main class="mx-auto w-full max-w-xl flex-1 px-4 pb-32 pt-6">
      <div v-if="error" class="space-y-3 py-16 text-center">
        <UIcon name="i-lucide-store" class="size-10 text-muted" />
        <h1 class="text-2xl font-bold">
          {{ t('portal.shopNotFound') }}
        </h1>
        <p class="text-muted">
          {{ t('portal.shopNotFoundText') }}
        </p>
      </div>
      <NuxtPage v-else-if="shop" />
    </main>

    <footer class="pb-6 text-center text-xs text-muted">
      {{ t('portal.poweredBy') }} <strong class="text-primary">Loopa</strong>
    </footer>
  </div>
</template>
