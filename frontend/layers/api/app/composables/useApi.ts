import type { ApiProblem } from '../types/api'

/**
 * Client HTTP unique : base URL, jeton Bearer, erreurs RFC 9457.
 * Chaque application stocke son jeton dans son propre cookie (runtimeConfig.public.tokenCookie).
 */
export function useAuthToken() {
  const config = useRuntimeConfig()
  return useCookie<string | null>(config.public.tokenCookie, { sameSite: 'lax', maxAge: 60 * 60 * 8 })
}

export function useApi() {
  const config = useRuntimeConfig()
  const token = useAuthToken()
  // Pas de useI18n() ici : useApi() est aussi appelé depuis des middlewares (hors composant).
  const i18n = useNuxtApp().$i18n

  return $fetch.create({
    baseURL: config.public.apiBase,
    onRequest({ options }) {
      const headers = new Headers(options.headers)
      if (token.value) headers.set('Authorization', `Bearer ${token.value}`)
      headers.set('Accept-Language', i18n.locale.value)
      options.headers = headers
    },
    onResponseError({ response }) {
      if (response.status === 401 && token.value && config.public.tokenCookie === 'loopa_admin_token') {
        token.value = null
        if (import.meta.client) navigateTo('/login')
      }
    }
  })
}

/** Message lisible d'une erreur d'API (detail RFC 9457), avec repli générique. */
export function apiErrorMessage(error: unknown, fallback = 'Une erreur est survenue. Réessayez.') {
  const data = (error as { data?: Partial<ApiProblem> })?.data
  return data?.detail ?? fallback
}

export function apiErrorCode(error: unknown) {
  return (error as { data?: Partial<ApiProblem> })?.data?.code
}
