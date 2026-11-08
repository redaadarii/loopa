<script setup lang="ts">
const config = useRuntimeConfig()
const open = ref(false)
const links = [
  { label: 'Fonctionnalités', to: '/#features' },
  { label: 'Comment ça marche', to: '/#how' },
  { label: 'Tarifs', to: '/pricing' },
  { label: 'FAQ', to: '/faq' }
]
</script>

<template>
  <div class="min-h-dvh bg-white">
    <header class="sticky top-0 z-20 border-b border-default bg-white/90 backdrop-blur">
      <div class="mx-auto flex h-16 max-w-6xl items-center justify-between gap-4 px-4">
        <NuxtLink to="/" aria-label="Loopa">
          <LoopaLogo />
        </NuxtLink>
        <nav class="hidden items-center gap-7 text-sm font-medium md:flex">
          <NuxtLink v-for="l in links" :key="l.to" :to="l.to" class="text-muted hover:text-highlighted">
            {{ l.label }}
          </NuxtLink>
        </nav>
        <div class="hidden items-center gap-2 md:flex">
          <UButton label="Se connecter" color="neutral" variant="ghost" :to="config.public.adminUrl" />
          <UButton label="Demander une démo" to="/contact" />
        </div>
        <UButton class="md:hidden" icon="i-lucide-menu" color="neutral" variant="ghost" aria-label="Menu" @click="open = !open" />
      </div>
      <nav v-if="open" class="space-y-1 border-t border-default px-4 py-3 md:hidden">
        <NuxtLink v-for="l in links" :key="l.to" :to="l.to" class="block rounded-lg px-3 py-2 hover:bg-elevated" @click="open = false">
          {{ l.label }}
        </NuxtLink>
        <UButton label="Demander une démo" to="/contact" block class="mt-2" />
      </nav>
    </header>

    <slot />

    <footer class="border-t border-default bg-canvas">
      <div class="mx-auto grid max-w-6xl gap-8 px-4 py-12 sm:grid-cols-2 lg:grid-cols-4">
        <div class="space-y-3">
          <LoopaLogo />
          <p class="text-sm text-muted">
            Les retours qui font revenir vos clients. Conçu au Maroc pour le e-commerce.
          </p>
        </div>
        <div>
          <p class="mb-3 text-sm font-semibold">
            Produit
          </p>
          <ul class="space-y-2 text-sm text-muted">
            <li><NuxtLink to="/#features">Fonctionnalités</NuxtLink></li>
            <li><NuxtLink to="/pricing">Tarifs</NuxtLink></li>
            <li><a :href="`${config.public.portalUrl}/atlas`">Démo du portail</a></li>
          </ul>
        </div>
        <div>
          <p class="mb-3 text-sm font-semibold">
            Ressources
          </p>
          <ul class="space-y-2 text-sm text-muted">
            <li><NuxtLink to="/faq">FAQ</NuxtLink></li>
            <li><NuxtLink to="/contact">Contact</NuxtLink></li>
          </ul>
        </div>
        <div>
          <p class="mb-3 text-sm font-semibold">
            Légal
          </p>
          <ul class="space-y-2 text-sm text-muted">
            <li><NuxtLink to="/legal/privacy">Confidentialité</NuxtLink></li>
            <li><NuxtLink to="/legal/terms">Conditions d'utilisation</NuxtLink></li>
            <li><NuxtLink to="/legal/returns-policy">Modèle de politique de retour</NuxtLink></li>
          </ul>
        </div>
      </div>
      <p class="border-t border-default py-6 text-center text-xs text-muted">
        © {{ new Date().getFullYear() }} Loopa
      </p>
    </footer>
  </div>
</template>
