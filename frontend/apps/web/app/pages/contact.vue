<script setup lang="ts">
useSeoMeta({ title: 'Demander une démo — Loopa' })
const toast = useToast()
const form = reactive({ name: '', email: '', shop: '', volume: '50 à 300', message: '' })
const sent = ref(false)
const VOLUMES = ['Moins de 50', '50 à 300', 'Plus de 300']

function submit() {
  // Envoi par email au commercial (pas de back-end dédié à la vitrine).
  const body = encodeURIComponent(`Nom : ${form.name}\nBoutique : ${form.shop}\nRetours / mois : ${form.volume}\n\n${form.message}`)
  window.location.href = `mailto:bonjour@loopa.app?subject=${encodeURIComponent('Démo Loopa')}&body=${body}`
  sent.value = true
  toast.add({ title: 'Merci ! Nous revenons vers vous sous 24 h.', color: 'success' })
}
</script>

<template>
  <main class="mx-auto grid max-w-6xl gap-12 px-4 py-16 md:grid-cols-2">
    <div class="space-y-4">
      <h1 class="text-4xl font-extrabold">
        Voyons ensemble vos retours
      </h1>
      <p class="text-muted">
        30 minutes pour configurer votre politique de retour, connecter Shopify et tester le portail avec vos vraies commandes.
      </p>
      <ul class="space-y-2 text-sm">
        <li class="flex gap-2">
          <UIcon name="i-lucide-check" class="size-5 text-primary" />Installation accompagnée
        </li>
        <li class="flex gap-2">
          <UIcon name="i-lucide-check" class="size-5 text-primary" />Import de vos commandes des 60 derniers jours
        </li>
        <li class="flex gap-2">
          <UIcon name="i-lucide-check" class="size-5 text-primary" />Réponse sous 24 h ouvrées
        </li>
      </ul>
    </div>
    <UCard>
      <form class="space-y-4" @submit.prevent="submit">
        <UFormField label="Nom" required>
          <UInput v-model="form.name" required class="w-full" />
        </UFormField>
        <UFormField label="Email professionnel" required>
          <UInput v-model="form.email" type="email" required class="w-full" />
        </UFormField>
        <UFormField label="Boutique (URL)">
          <UInput v-model="form.shop" placeholder="ma-boutique.ma" class="w-full" />
        </UFormField>
        <UFormField label="Retours par mois">
          <USelect v-model="form.volume" :items="VOLUMES" class="w-full" />
        </UFormField>
        <UFormField label="Message">
          <UTextarea v-model="form.message" :rows="4" class="w-full" />
        </UFormField>
        <UButton type="submit" label="Demander une démo" block size="lg" :disabled="sent" />
      </form>
    </UCard>
  </main>
</template>
