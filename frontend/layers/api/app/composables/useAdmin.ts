import { useMutation, useQuery, useQueryCache } from '@pinia/colada'
import type {
  AnalyticsSummary, AuthSession, BillingUsage, Branding, IntegrationStatus, Member, Policy, Reason, ReturnPage,
  ReturnStatus, ReturnView, Role
} from '../types/api'

/** Clés de cache centralisées : invalider ['returns'] rafraîchit liste, détail et statistiques. */
export const keys = {
  returns: ['returns'] as const,
  list: (status?: ReturnStatus, q?: string) => ['returns', 'list', status ?? 'ALL', q ?? ''] as const,
  detail: (id: string) => ['returns', 'detail', id] as const,
  summary: (days: number) => ['returns', 'summary', days] as const
}

/* ---------- Session marchand ---------- */

export function useAdminSession() {
  return useState<AuthSession | null>('admin-session', () => null)
}

export function useAuth() {
  const api = useApi()
  const token = useAuthToken()
  const session = useAdminSession()

  async function login(email: string, password: string) {
    const result = await api<AuthSession>('/api/auth/login', { method: 'POST', body: { email, password } })
    token.value = result.token
    session.value = result
    return result
  }

  async function refresh() {
    if (!token.value) return null
    session.value = await api<AuthSession>('/api/auth/me')
    return session.value
  }

  function logout() {
    token.value = null
    session.value = null
    // Recharge complète : vide le cache des requêtes de l'utilisateur précédent.
    return reloadNuxtApp({ path: '/login' })
  }

  const isAdmin = computed(() => session.value?.user.role !== 'AGENT')
  return { session, token, login, refresh, logout, isAdmin }
}

/* ---------- Retours ---------- */

export function useAdminReturns(filters: Ref<{ status?: ReturnStatus, q?: string }>) {
  const api = useApi()
  const extra = ref<ReturnView[]>([])
  const cursor = ref<string | null>(null)
  const query = useQuery({
    key: () => keys.list(filters.value.status, filters.value.q),
    query: async () => {
      extra.value = []
      const page = await api<ReturnPage>('/api/admin/v1/returns', {
        query: { status: filters.value.status, q: filters.value.q || undefined, limit: 25 }
      })
      cursor.value = page.nextCursor
      return page
    }
  })
  const loadingMore = ref(false)
  async function loadMore() {
    if (!cursor.value) return
    loadingMore.value = true
    try {
      const page = await api<ReturnPage>('/api/admin/v1/returns', {
        query: { status: filters.value.status, q: filters.value.q || undefined, limit: 25, cursor: cursor.value }
      })
      extra.value.push(...page.items)
      cursor.value = page.nextCursor
    } finally {
      loadingMore.value = false
    }
  }
  const items = computed(() => [...(query.data.value?.items ?? []), ...extra.value])
  return { ...query, items, hasMore: computed(() => !!cursor.value), loadMore, loadingMore }
}

export function useAdminReturn(id: MaybeRefOrGetter<string>) {
  const api = useApi()
  return useQuery({
    key: () => keys.detail(toValue(id)),
    query: () => api<ReturnView>(`/api/admin/v1/returns/${toValue(id)}`)
  })
}

export type ReturnAction = 'approve' | 'reject' | 'receive' | 'resolve'

export function useReturnAction() {
  const api = useApi()
  const cache = useQueryCache()
  return useMutation({
    mutation: ({ id, action, reason }: { id: string, action: ReturnAction, reason?: string }) =>
      api<ReturnView>(`/api/admin/v1/returns/${id}/${action}`, {
        method: 'POST',
        body: action === 'reject' ? { reason } : undefined
      }),
    onSuccess: (data) => cache.setQueryData(keys.detail(data.id), data),
    onSettled: () => cache.invalidateQueries({ key: keys.returns })
  })
}

export function useBulkApprove() {
  const api = useApi()
  const cache = useQueryCache()
  return useMutation({
    mutation: (ids: string[]) =>
      api<{ approved: string[], failed: Record<string, string> }>('/api/admin/v1/returns/bulk-approve', { method: 'POST', body: { ids } }),
    onSettled: () => cache.invalidateQueries({ key: keys.returns })
  })
}

/** Télécharge l'export CSV (avec le jeton : un simple lien ne peut pas l'envoyer). */
export function useExportReturns() {
  const api = useApi()
  const loading = ref(false)
  async function download(status?: ReturnStatus) {
    loading.value = true
    try {
      const blob = await api<Blob>('/api/admin/v1/returns/export.csv', { query: { status }, responseType: 'blob' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `retours-${new Date().toISOString().slice(0, 10)}.csv`
      a.click()
      URL.revokeObjectURL(url)
    } finally {
      loading.value = false
    }
  }
  return { download, loading }
}

export function useChangePassword() {
  const api = useApi()
  return useMutation({
    mutation: (body: { currentPassword: string, newPassword: string }) =>
      api('/api/auth/password', { method: 'POST', body })
  })
}

export function useAddNote() {
  const api = useApi()
  const cache = useQueryCache()
  return useMutation({
    mutation: ({ id, note }: { id: string, note: string }) =>
      api<ReturnView>(`/api/admin/v1/returns/${id}/notes`, { method: 'POST', body: { note } }),
    onSuccess: (data) => cache.setQueryData(keys.detail(data.id), data)
  })
}

/** Photo jointe : chargée avec le jeton puis exposée en URL locale (une balise <img> ne peut pas l'envoyer). */
export function useAuthedFile(fileId: MaybeRefOrGetter<string>) {
  const api = useApi()
  const url = ref<string | null>(null)
  const error = ref(false)
  watchEffect(async (onCleanup) => {
    const id = toValue(fileId)
    try {
      const blob = await api<Blob>(`/api/admin/v1/files/${id}`, { responseType: 'blob' })
      const objectUrl = URL.createObjectURL(blob)
      url.value = objectUrl
      onCleanup(() => URL.revokeObjectURL(objectUrl))
    } catch {
      error.value = true
    }
  })
  return { url, error }
}

/* ---------- Statistiques ---------- */

export function useAnalytics(days: MaybeRefOrGetter<number>) {
  const api = useApi()
  return useQuery({
    key: () => keys.summary(toValue(days)),
    query: () => api<AnalyticsSummary>('/api/admin/v1/analytics/summary', { query: { days: toValue(days) } })
  })
}

/* ---------- Réglages ---------- */

function useResource<T>(key: string, path: string) {
  const api = useApi()
  const cache = useQueryCache()
  const query = useQuery({ key: [key], query: () => api<T>(path) })
  const save = useMutation({
    mutation: (body: T) => api<T>(path, { method: 'PUT', body: body as Record<string, unknown> }),
    onSuccess: (data) => cache.setQueryData([key], data)
  })
  return { ...query, save }
}

export const usePolicy = () => useResource<Policy>('policy', '/api/admin/v1/settings/policy')
export const useReasons = () => useResource<Reason[]>('reasons', '/api/admin/v1/settings/reasons')
export const useBranding = () => useResource<Branding>('branding', '/api/admin/v1/settings/branding')

/* ---------- Équipe ---------- */

export function useTeam() {
  const api = useApi()
  const cache = useQueryCache()
  const query = useQuery({ key: ['team'], query: () => api<Member[]>('/api/admin/v1/team') })
  const invite = useMutation({
    mutation: (body: { email: string, name: string, role: Role }) =>
      api<{ member: Member, temporaryPassword: string }>('/api/admin/v1/team', { method: 'POST', body }),
    onSettled: () => cache.invalidateQueries({ key: ['team'] })
  })
  const update = useMutation({
    mutation: ({ id, ...body }: { id: string, role?: Role, active?: boolean }) =>
      api<Member>(`/api/admin/v1/team/${id}`, { method: 'PATCH', body }),
    onSettled: () => cache.invalidateQueries({ key: ['team'] })
  })
  return { ...query, invite, update }
}

/* ---------- Abonnement et intégrations ---------- */

export function useBilling() {
  const api = useApi()
  const cache = useQueryCache()
  const query = useQuery({ key: ['billing'], query: () => api<BillingUsage>('/api/admin/v1/billing') })
  const subscribe = useMutation({
    mutation: (plan: string) =>
      api<{ plan: string, confirmationUrl: string | null }>('/api/admin/v1/billing/subscribe', { method: 'POST', body: { plan } }),
    onSettled: () => cache.invalidateQueries({ key: ['billing'] })
  })
  return { ...query, subscribe }
}

export function useIntegrations() {
  const api = useApi()
  const cache = useQueryCache()
  const query = useQuery({ key: ['integrations'], query: () => api<IntegrationStatus>('/api/admin/v1/integrations') })
  const sync = useMutation({
    mutation: () => api<{ imported: number }>('/api/admin/v1/integrations/shopify/sync', { method: 'POST' }),
    onSettled: () => cache.invalidateQueries({ key: ['integrations'] })
  })
  return { ...query, sync }
}
