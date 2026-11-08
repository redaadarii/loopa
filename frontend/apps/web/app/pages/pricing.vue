<script setup lang="ts">
useSeoMeta({ title: 'Tarifs — Loopa', description: 'Un plan gratuit pour démarrer, des plans payants selon votre volume de retours.' })

const plans = [
  { name: 'Starter', price: 'Gratuit', detail: '20 retours / mois', features: ['Portail de retour à vos couleurs', 'Remboursements', 'Emails trilingues', '1 utilisateur'], cta: 'Commencer' },
  { name: 'Growth', price: '290 MAD', per: '/ mois', detail: '300 retours / mois', features: ['Tout Starter', 'Échanges et avoirs', 'Bonus shop-now', 'Approbation automatique', 'Statistiques', '5 utilisateurs'], cta: 'Essai gratuit 14 jours', highlight: true },
  { name: 'Scale', price: '890 MAD', per: '/ mois', detail: 'Retours illimités', features: ['Tout Growth', 'Étiquettes transporteurs', 'API et webhooks', 'Utilisateurs illimités', 'Support prioritaire'], cta: 'Parler à l\'équipe' }
]
</script>

<template>
  <main class="mx-auto max-w-6xl px-4 py-16">
    <h1 class="text-4xl font-extrabold md:text-5xl">
      Des tarifs qui grandissent avec vous
    </h1>
    <p class="mt-3 max-w-2xl text-muted">
      Facturé via Shopify, sans engagement. Le quota est remis à zéro chaque mois.
    </p>
    <div class="mt-10 grid gap-6 md:grid-cols-3">
      <div
        v-for="p in plans"
        :key="p.name"
        class="flex flex-col rounded-2xl border p-6"
        :class="p.highlight ? 'border-primary ring-2 ring-primary' : 'border-default'"
      >
        <div class="flex items-center justify-between">
          <p class="font-semibold">
            {{ p.name }}
          </p>
          <UBadge v-if="p.highlight" label="Le plus choisi" color="secondary" variant="subtle" class="rounded-full" />
        </div>
        <p class="mt-3 font-display text-4xl font-extrabold">
          {{ p.price }} <span v-if="p.per" class="text-base font-medium text-muted">{{ p.per }}</span>
        </p>
        <p class="mt-1 text-sm text-muted">
          {{ p.detail }}
        </p>
        <ul class="mt-6 flex-1 space-y-2.5 text-sm">
          <li v-for="f in p.features" :key="f" class="flex gap-2">
            <UIcon name="i-lucide-check" class="mt-0.5 size-4 shrink-0 text-primary" />{{ f }}
          </li>
        </ul>
        <UButton :label="p.cta" block size="lg" class="mt-8" :variant="p.highlight ? 'solid' : 'outline'" to="/contact" />
      </div>
    </div>
  </main>
</template>
