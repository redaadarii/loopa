import type { PortalShop } from '../../../../layers/api/app/types/api'

/** Boutique courante du portail, chargée par la page parente /[shop]. */
export function usePortalShop() {
  return useState<PortalShop | null>('portal-shop', () => null)
}

/** État du parcours partagé entre pages (session du code à usage unique). */
export function usePortalFlow() {
  return useState<{ sessionId: string | null, maskedContact: string | null, devCode: string | null }>(
    'portal-flow', () => ({ sessionId: null, maskedContact: null, devCode: null }))
}
