import { useMutation, useQuery } from '@pinia/colada'
import type {
  Attachment, CustomerReturn, PortalOrder, PortalSessionStarted, PortalSessionVerified, PortalShop,
  SubmitReturnRequest
} from '../types/api'

/** Parcours client final : boutique, code à usage unique, commande, photos, création et suivi du retour. */
export function usePortalApi() {
  const api = useApi()
  const token = useAuthToken()

  return {
    shop: (handle: string) => api<PortalShop>(`/api/portal/v1/shops/${encodeURIComponent(handle)}`),
    startSession: (body: { shop: string, orderNumber: string, contact: string, locale: string }) =>
      api<PortalSessionStarted>('/api/portal/v1/sessions', { method: 'POST', body }),
    async verify(sessionId: string, code: string) {
      const result = await api<PortalSessionVerified>('/api/portal/v1/sessions/verify', {
        method: 'POST',
        body: { sessionId, code }
      })
      token.value = result.token
      return result
    },
    upload(file: File) {
      const form = new FormData()
      form.append('file', file)
      return api<Attachment>('/api/portal/v1/attachments', { method: 'POST', body: form })
    },
    submit: (body: SubmitReturnRequest) => api<CustomerReturn>('/api/portal/v1/returns', { method: 'POST', body }),
    addTracking: (id: string, carrier: string, trackingNumber: string) =>
      api<CustomerReturn>(`/api/portal/v1/returns/${id}/tracking`, { method: 'POST', body: { carrier, trackingNumber } }),
    cancel: (id: string) => api<CustomerReturn>(`/api/portal/v1/returns/${id}/cancel`, { method: 'POST' }),
    hasSession: computed(() => !!token.value),
    clearSession: () => { token.value = null }
  }
}

export function usePortalOrder() {
  const api = useApi()
  return useQuery({ key: ['portal-order'], query: () => api<PortalOrder>('/api/portal/v1/order') })
}

export function usePortalReturn(id: MaybeRefOrGetter<string>) {
  const api = useApi()
  return useQuery({
    key: () => ['portal-return', toValue(id)],
    query: () => api<CustomerReturn>(`/api/portal/v1/returns/${toValue(id)}`)
  })
}

export function useSubmitReturn() {
  const portal = usePortalApi()
  return useMutation({ mutation: (body: SubmitReturnRequest) => portal.submit(body) })
}
