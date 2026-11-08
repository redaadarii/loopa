// Back-office marchand : SPA (données privées, pas de SEO), aussi embarquable dans l'admin Shopify.
export default defineNuxtConfig({
  extends: ['../../layers/ui', '../../layers/i18n', '../../layers/api'],
  modules: ['@vueuse/nuxt'],
  ssr: false,
  compatibilityDate: '2026-11-01',
  devtools: { enabled: false },
  typescript: { strict: true },
  vite: { optimizeDeps: { include: ['zod'] } },
  runtimeConfig: {
    public: {
      tokenCookie: 'loopa_admin_token',
      // URL publique du portail client (lien affiché dans « Portail client »)
      portalUrl: 'http://localhost:3001',
      demoLogin: true
    }
  },
  app: {
    head: {
      title: 'Loopa — Back-office',
      link: [{ rel: 'icon', type: 'image/svg+xml', href: '/favicon.svg' }]
    }
  }
})
