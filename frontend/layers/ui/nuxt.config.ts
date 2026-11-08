// Layer "ui" : design system Loopa (tokens, thème Nuxt UI, composants partagés).
// Étendu par les trois applications pour garantir une interface cohérente.
// Polices auto-hébergées (Fontsource) : aucune requête vers Google Fonts, RGPD et CSP simplifiés.
import { fileURLToPath } from 'node:url'

export default defineNuxtConfig({
  modules: ['@nuxt/ui'],
  css: [fileURLToPath(new URL('./app/assets/css/main.css', import.meta.url))],
  ui: {
    fonts: false,
    theme: {
      colors: ['primary', 'secondary', 'success', 'info', 'warning', 'error', 'neutral']
    }
  },
  icon: {
    serverBundle: { collections: ['lucide'] },
    clientBundle: { scan: true }
  }
})
