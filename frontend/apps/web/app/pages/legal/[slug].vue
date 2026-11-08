<script setup lang="ts">
// Pages légales : textes à faire valider par un juriste avant la mise en ligne.
const route = useRoute()
const PAGES: Record<string, { title: string, sections: Array<[string, string]> }> = {
  'privacy': {
    title: 'Politique de confidentialité',
    sections: [
      ['Données traitées', 'Pour chaque boutique : comptes de l\'équipe, réglages, commandes importées depuis Shopify. Pour chaque client final : nom, email ou téléphone, articles retournés, photos jointes et messages.'],
      ['Finalités', 'Gérer les demandes de retour, informer le client de leur avancement, exécuter remboursements, échanges et avoirs, produire les statistiques de la boutique.'],
      ['Conservation', 'Les données d\'un retour sont conservées 24 mois puis anonymisées. Les codes de vérification expirent après 10 minutes.'],
      ['Vos droits', 'Accès, rectification, effacement : écrivez à privacy@loopa.app. Les demandes transmises par Shopify (customers/redact) sont traitées automatiquement. Traitement déclaré conformément à la loi 09-08.']
    ]
  },
  'terms': {
    title: 'Conditions d\'utilisation',
    sections: [
      ['Service', 'Loopa fournit aux marchands un logiciel de gestion des retours et échanges, accessible en ligne et via l\'application Shopify.'],
      ['Abonnement', 'Les plans payants sont facturés mensuellement via Shopify. Le quota de retours est remis à zéro le 1er de chaque mois.'],
      ['Responsabilités', 'Le marchand reste seul décideur de sa politique de retour et des remboursements accordés à ses clients.']
    ]
  },
  'returns-policy': {
    title: 'Modèle de politique de retour',
    sections: [
      ['Délai', 'Vous disposez de 30 jours après réception de votre commande pour demander un retour ou un échange.'],
      ['Conditions', 'Les articles doivent être non portés, non lavés et dans leur emballage d\'origine, étiquettes attachées.'],
      ['Solutions', 'Avoir majoré de 50 MAD, échange de taille ou de couleur, ou remboursement (par virement pour le paiement à la livraison).']
    ]
  }
}
const page = computed(() => PAGES[String(route.params.slug)])
if (!page.value) throw createError({ statusCode: 404, statusMessage: 'Page introuvable' })
useSeoMeta({ title: () => `${page.value?.title} — Loopa` })
</script>

<template>
  <main v-if="page" class="mx-auto max-w-3xl px-4 py-16">
    <h1 class="mb-8 text-4xl font-extrabold">
      {{ page.title }}
    </h1>
    <section v-for="[title, text] in page.sections" :key="title" class="mb-6">
      <h2 class="mb-2 text-xl font-bold">
        {{ title }}
      </h2>
      <p class="leading-relaxed text-muted">
        {{ text }}
      </p>
    </section>
  </main>
</template>
