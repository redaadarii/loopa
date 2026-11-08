// Vitrine : pré-rendue (SSG) pour le SEO et la vitesse.
export default defineNuxtConfig({
  extends: ['../../layers/ui', '../../layers/i18n', '../../layers/api'],
  modules: ['@vueuse/nuxt'],
  compatibilityDate: '2026-11-01',
  devtools: { enabled: false },
  typescript: { strict: true },
  nitro: {
    prerender: {
      routes: ['/', '/pricing', '/faq', '/contact', '/legal/privacy', '/legal/terms', '/legal/returns-policy'],
      crawlLinks: true
    }
  },
  runtimeConfig: {
    public: {
      adminUrl: 'http://localhost:3002',
      portalUrl: 'http://localhost:3001'
    }
  },
  app: {
    head: {
      title: 'Loopa — Les retours qui font revenir vos clients',
      meta: [
        { name: 'description', content: 'Automatisez retours et échanges de votre boutique Shopify et transformez les remboursements en nouvelles ventes.' },
        { property: 'og:title', content: 'Loopa — Les retours qui font revenir vos clients' }
      ],
      link: [{ rel: 'icon', type: 'image/svg+xml', href: '/favicon.svg' }]
    }
  }
})
