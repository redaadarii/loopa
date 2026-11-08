<script setup lang="ts">
useSeoMeta({ title: 'FAQ — Loopa' })
const items = [
  { label: 'Faut-il un compte pour que mes clients fassent un retour ?', content: 'Non. Le client saisit son numéro de commande et son email ou téléphone, reçoit un code à 6 chiffres, et c\'est tout.' },
  { label: 'Comment fonctionne le remboursement d\'une commande payée à la livraison ?', content: 'Loopa crée le remboursement dans Shopify sans transaction carte et vous rappelle d\'effectuer le virement. Mieux : proposez un avoir majoré, utilisable immédiatement.' },
  { label: 'Qu\'est-ce que le bonus shop-now ?', content: 'Un petit montant offert si le client choisit un avoir plutôt qu\'un remboursement. Vous le fixez dans votre politique de retour ; il est affiché en premier dans le portail.' },
  { label: 'Puis-je refuser un retour ?', content: 'Oui. Les retours avec photo obligatoire ou au-dessus de votre plafond arrivent dans votre file ; le motif du refus est envoyé au client par email.' },
  { label: 'Mes données sont-elles protégées ?', content: 'Chaque boutique est isolée, les jetons Shopify sont chiffrés, et nous ne conservons que les commandes ayant fait l\'objet d\'un retour. Loopa répond aux demandes RGPD de Shopify et respecte la loi 09-08.' },
  { label: 'Dans quelles langues est le portail ?', content: 'Français, anglais et arabe (de droite à gauche). La langue du client est retenue pour ses emails.' }
]
</script>

<template>
  <main class="mx-auto max-w-3xl px-4 py-16">
    <h1 class="mb-8 text-4xl font-extrabold">
      Questions fréquentes
    </h1>
    <UAccordion :items="items" type="multiple" :ui="{ trigger: 'text-base font-semibold py-4', body: 'text-muted' }" />
  </main>
</template>
