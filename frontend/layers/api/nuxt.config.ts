// Layer "api" : client HTTP vers loopa-api + cache des données serveur (Pinia Colada).
// Règle : aucun composant n'appelle fetch directement, tout passe par les composables de ce layer.
export default defineNuxtConfig({
  modules: ['@pinia/nuxt', '@pinia/colada-nuxt'],
  runtimeConfig: {
    public: {
      // NUXT_PUBLIC_API_BASE en production
      apiBase: 'http://localhost:8080',
      tokenCookie: 'loopa_token'
    }
  }
})
