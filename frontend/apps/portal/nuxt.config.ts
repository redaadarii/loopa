// Portail client final : SSR pour un premier affichage rapide sur mobile, aux couleurs du marchand.
// URL : /<boutique>/… (sous-domaine ou domaine personnalisé derrière un proxy en production).
export default defineNuxtConfig({
  extends: ['../../layers/ui', '../../layers/i18n', '../../layers/api'],
  modules: ['@vueuse/nuxt'],
  compatibilityDate: '2026-11-01',
  devtools: { enabled: false },
  typescript: { strict: true },
  vite: { optimizeDeps: { include: ['zod'] } },
  runtimeConfig: {
    public: {
      tokenCookie: 'loopa_portal_token',
      demoShop: 'atlas'
    }
  },
  app: {
    head: {
      title: 'Retours et échanges',
      meta: [
        { name: 'viewport', content: 'width=device-width, initial-scale=1, viewport-fit=cover' },
        { name: 'robots', content: 'noindex' }
      ],
      link: [{ rel: 'icon', type: 'image/svg+xml', href: '/favicon.svg' }]
    }
  }
})
