// Toute page sauf /login exige une session marchand valide.
export default defineNuxtRouteMiddleware(async (to) => {
  const { token, session, refresh, logout } = useAuth()
  if (to.path === '/login') {
    return token.value ? navigateTo('/') : undefined
  }
  if (!token.value) {
    return navigateTo({ path: '/login', query: to.fullPath !== '/' ? { redirect: to.fullPath } : undefined })
  }
  if (!session.value) {
    try {
      await refresh()
    } catch {
      return logout()
    }
  }
  // Réglages sensibles réservés aux administrateurs.
  const adminOnly = ['/settings/team', '/settings/billing', '/settings/integrations']
  if (session.value?.user.role === 'AGENT' && adminOnly.includes(to.path)) {
    return navigateTo('/')
  }
})
