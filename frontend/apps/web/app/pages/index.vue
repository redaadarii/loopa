<script setup lang="ts">
const config = useRuntimeConfig()

const steps = [
  { icon: 'i-lucide-smartphone', title: 'Le client fait sa demande', text: 'Un portail à vos couleurs, en français, anglais et arabe. Numéro de commande, code reçu, deux minutes, sans compte.' },
  { icon: 'i-lucide-shield-check', title: 'Vous validez — ou pas besoin', text: 'Règles automatiques par montant et motif. Les cas à risque (photos, montants élevés) arrivent dans votre file.' },
  { icon: 'i-lucide-repeat', title: 'Loopa exécute', text: 'À la réception du colis : carte cadeau, commande d\'échange ou remboursement créés directement dans Shopify.' },
  { icon: 'i-lucide-chart-line', title: 'Vous comprenez', text: 'Motifs, produits qui reviennent, valeur conservée : les données pour corriger vos fiches produits.' }
]

const features = [
  { icon: 'i-lucide-gift', title: 'Bonus shop-now', text: 'Proposez un avoir majoré avant le remboursement. C\'est le levier qui transforme un retour en nouvelle vente.' },
  { icon: 'i-lucide-truck', title: 'Pensé pour le paiement à la livraison', text: 'Remboursement par virement, suivi Amana, Sendit, CTM : les réalités du e-commerce marocain sont prévues.' },
  { icon: 'i-lucide-languages', title: 'Trilingue, RTL compris', text: 'Portail et emails en français, anglais et arabe, avec mise en page de droite à gauche.' },
  { icon: 'i-lucide-zap', title: 'Approbation automatique', text: 'Fixez un plafond et les motifs à vérifier : le reste est approuvé en une seconde, jour et nuit.' },
  { icon: 'i-lucide-camera', title: 'Photos obligatoires', text: 'Article endommagé ? Le client joint ses photos, vous décidez en connaissance de cause.' },
  { icon: 'i-lucide-users', title: 'Équipe et rôles', text: 'Agents pour traiter, administrateurs pour régler. Chaque action est tracée dans l\'historique du retour.' }
]
</script>

<template>
  <main>
    <section class="relative overflow-hidden">
      <div class="mx-auto grid max-w-6xl items-center gap-12 px-4 py-16 md:grid-cols-[1.1fr_1fr] md:py-24">
        <div class="space-y-6">
          <UBadge color="secondary" variant="subtle" label="Application Shopify · Retours et échanges" class="rounded-full" />
          <h1 class="text-4xl font-extrabold leading-[1.05] md:text-6xl">
            Les retours qui font <span class="text-primary">revenir</span> vos clients.
          </h1>
          <p class="max-w-xl text-lg text-muted">
            Loopa automatise retours et échanges de votre boutique et transforme les remboursements en nouvelles ventes,
            grâce à l'avoir majoré et à l'échange en un clic.
          </p>
          <div class="flex flex-wrap gap-3">
            <UButton label="Demander une démo" size="xl" to="/contact" />
            <UButton
              label="Essayer le portail client"
              size="xl"
              color="neutral"
              variant="outline"
              trailing-icon="i-lucide-arrow-up-right"
              :to="`${config.public.portalUrl}/atlas`"
              target="_blank"
            />
          </div>
          <p class="text-sm text-muted">
            Plan gratuit jusqu'à 20 retours par mois · Sans engagement
          </p>
        </div>

        <!-- Aperçu produit : fragment du portail client -->
        <div class="relative">
          <div class="absolute -inset-6 -z-10 rounded-[3rem] bg-gradient-to-br from-loopa-100 to-coral-100 opacity-70 blur-2xl" />
          <div class="mx-auto max-w-sm rounded-[2rem] border-8 border-ink bg-canvas p-5 shadow-2xl">
            <p class="mb-3 text-xs font-semibold text-muted">
              Étape 3 sur 4 · Solution
            </p>
            <p class="mb-4 font-display text-xl font-extrabold">
              Que souhaitez-vous en échange ?
            </p>
            <div class="space-y-3">
              <div class="rounded-xl border-2 border-primary bg-white p-4">
                <div class="flex items-center justify-between gap-2">
                  <span class="font-bold">Avoir en boutique</span>
                  <span class="rounded-full bg-secondary px-2 py-0.5 text-xs font-bold">+50 MAD offerts</span>
                </div>
                <p class="mt-1 text-xs text-muted">
                  349 MAD utilisables tout de suite
                </p>
              </div>
              <div class="rounded-xl border border-default bg-white p-4">
                <span class="font-bold">Échange</span>
                <p class="mt-1 text-xs text-muted">
                  Une autre taille, expédiée dès réception
                </p>
              </div>
              <div class="rounded-xl border border-default bg-white p-4">
                <span class="font-bold">Remboursement</span>
                <p class="mt-1 text-xs text-muted">
                  Par virement pour le paiement à la livraison
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section id="how" class="bg-canvas py-20">
      <div class="mx-auto max-w-6xl px-4">
        <h2 class="mb-3 text-3xl font-extrabold md:text-4xl">
          Un retour en 4 étapes, sans tableur ni WhatsApp
        </h2>
        <p class="mb-10 max-w-2xl text-muted">
          Du clic du client au remboursement dans Shopify, chaque étape est tracée et notifiée par email.
        </p>
        <div class="grid gap-8 sm:grid-cols-2 lg:grid-cols-4">
          <div v-for="(s, i) in steps" :key="s.title" class="space-y-3">
            <div class="grid size-12 place-items-center rounded-xl bg-primary/10">
              <UIcon :name="s.icon" class="size-6 text-primary" />
            </div>
            <p class="text-xs font-bold text-muted">
              0{{ i + 1 }}
            </p>
            <h3 class="text-lg font-bold">
              {{ s.title }}
            </h3>
            <p class="text-sm text-muted">
              {{ s.text }}
            </p>
          </div>
        </div>
      </div>
    </section>

    <section id="features" class="mx-auto max-w-6xl px-4 py-20">
      <h2 class="mb-10 text-3xl font-extrabold md:text-4xl">
        Tout ce qu'il faut pour des retours rentables
      </h2>
      <div class="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
        <div v-for="f in features" :key="f.title" class="rounded-2xl border border-default p-6">
          <UIcon :name="f.icon" class="size-6 text-primary" />
          <h3 class="mt-4 text-lg font-bold">
            {{ f.title }}
          </h3>
          <p class="mt-2 text-sm text-muted">
            {{ f.text }}
          </p>
        </div>
      </div>
    </section>

    <section class="px-4 pb-20">
      <div class="mx-auto flex max-w-6xl flex-col items-start gap-6 rounded-3xl bg-ink px-8 py-12 text-white md:flex-row md:items-center md:justify-between">
        <div>
          <h2 class="text-3xl font-extrabold">
            Prêt à garder vos ventes ?
          </h2>
          <p class="mt-2 text-white/70">
            Installation en 5 minutes depuis votre admin Shopify.
          </p>
        </div>
        <div class="flex gap-3">
          <UButton label="Voir les tarifs" size="xl" color="neutral" variant="outline" class="text-white ring-white/30" to="/pricing" />
          <UButton label="Demander une démo" size="xl" to="/contact" />
        </div>
      </div>
    </section>
  </main>
</template>
