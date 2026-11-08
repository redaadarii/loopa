// Source unique de vérité pour l'affichage des statuts : même couleur, même icône, partout.
export type ReturnStatus =
  | 'SUBMITTED'
  | 'APPROVED'
  | 'REJECTED'
  | 'CANCELLED'
  | 'IN_TRANSIT'
  | 'RECEIVED'
  | 'RESOLVED'

type BadgeColor = 'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'error' | 'neutral'

export const RETURN_STATUS_META: Record<ReturnStatus, { color: BadgeColor, icon: string }> = {
  SUBMITTED: { color: 'warning', icon: 'i-lucide-inbox' },
  APPROVED: { color: 'info', icon: 'i-lucide-check' },
  REJECTED: { color: 'error', icon: 'i-lucide-x' },
  CANCELLED: { color: 'neutral', icon: 'i-lucide-ban' },
  IN_TRANSIT: { color: 'info', icon: 'i-lucide-truck' },
  RECEIVED: { color: 'primary', icon: 'i-lucide-package-check' },
  RESOLVED: { color: 'success', icon: 'i-lucide-badge-check' }
}
