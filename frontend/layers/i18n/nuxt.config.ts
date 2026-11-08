// Layer "i18n" : français (défaut), anglais, arabe (RTL).
export default defineNuxtConfig({
  modules: ['@nuxtjs/i18n'],
  i18n: {
    defaultLocale: 'fr',
    strategy: 'no_prefix',
    detectBrowserLanguage: { useCookie: true, cookieKey: 'loopa_lang' },
    locales: [
      { code: 'fr', language: 'fr-MA', name: 'Français', dir: 'ltr', file: 'fr.json' },
      { code: 'en', language: 'en-US', name: 'English', dir: 'ltr', file: 'en.json' },
      { code: 'ar', language: 'ar-MA', name: 'العربية', dir: 'rtl', file: 'ar.json' }
    ]
  }
})
