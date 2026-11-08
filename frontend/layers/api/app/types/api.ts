// Contrats de l'API loopa-api (miroir des records Java). `pnpm gen:api` génère les types OpenAPI équivalents.
import type { ReturnStatus } from '../../../ui/app/utils/returnStatus'

export type { ReturnStatus }
export type ResolutionType = 'REFUND' | 'EXCHANGE' | 'STORE_CREDIT'
export type Role = 'OWNER' | 'ADMIN' | 'AGENT'

/* ---------- Authentification ---------- */
export interface SessionUser { id: string, email: string, name: string, role: Role }
export interface SessionShop { id: string, handle: string, name: string, currency: string, plan: string, platform: string }
export interface AuthSession { token: string | null, expiresAt: string | null, user: SessionUser, shop: SessionShop }

/* ---------- Retours ---------- */
export interface ReturnLine {
  id: string
  orderLineId: string
  title: string
  variantTitle: string | null
  quantity: number
  unitPriceMinor: number
  reasonCode: string
  exchangeVariantId: string | null
}
export interface HistoryEntry { type: string, actor: string, message: string | null, occurredAt: string }
export interface ReturnView {
  id: string
  number: string
  orderId: string
  orderName: string
  customerName: string | null
  customerEmail: string | null
  locale: string
  status: ReturnStatus
  resolutionType: ResolutionType
  totalMinor: number
  bonusMinor: number
  creditMinor: number
  currency: string
  customerNote: string | null
  rejectionReason: string | null
  carrier: string | null
  trackingNumber: string | null
  resolutionRef: string | null
  resolutionError: string | null
  lines: ReturnLine[]
  attachmentIds: string[]
  history: HistoryEntry[]
  createdAt: string
  approvedAt: string | null
  updatedAt: string
}
export interface ReturnPage { items: ReturnView[], nextCursor: string | null, counts: Record<ReturnStatus, number> }

/* ---------- Statistiques ---------- */
export interface Count { key: string, count: number }
export interface AnalyticsSummary {
  days: number
  currency: string
  totalReturns: number
  pending: number
  approved: number
  rejected: number
  resolved: number
  retainedMinor: number
  refundedMinor: number
  bonusMinor: number
  medianApprovalMinutes: number | null
  autoApproved: number
  reasons: Count[]
  resolutions: Count[]
  topProducts: Count[]
  daily: Array<{ date: string, count: number }>
}

/* ---------- Réglages ---------- */
export interface Policy {
  windowDays: number
  autoApprove: boolean
  autoApproveMaxAmountMinor: number
  allowRefund: boolean
  allowExchange: boolean
  allowStoreCredit: boolean
  storeCreditBonusMinor: number
  excludedProductTypes: string[]
}
export interface Reason {
  code: string
  labelFr: string
  labelEn: string
  labelAr: string
  requiresPhoto: boolean
  active: boolean
  position?: number
}
export interface Branding { name: string, primaryColor: string, logoUrl: string | null, welcomeMessage: string | null, handle: string }
export interface Member { id: string, email: string, name: string, role: Role, active: boolean, lastLoginAt: string | null }
export interface Plan { code: string, name: string, priceMinor: number, currency: string, quota: number, features: string[] }
export interface BillingUsage { plan: string, quota: number, used: number, periodStart: string, plans: Plan[] }
export interface IntegrationStatus {
  platform: string
  shopDomain: string | null
  connected: boolean
  appConfigured: boolean
  ordersSynced: number
  lastSyncAt: string | null
}

/* ---------- Portail ---------- */
export interface PortalShop {
  handle: string
  name: string
  primaryColor: string
  logoUrl: string | null
  welcomeMessage: string | null
  currency: string
  defaultLocale: string
  windowDays: number
  storeCreditBonusMinor: number
}
export interface PortalSessionStarted { sessionId: string, channel: 'EMAIL' | 'SMS', maskedContact: string, expiresAt: string, devCode: string | null }
export interface PortalSessionVerified { token: string, expiresAt: string, orderId: string }
export interface PortalOrderLine {
  orderLineId: string
  title: string
  variantTitle: string | null
  sku: string | null
  productType: string | null
  quantity: number
  returnableQuantity: number
  unitPriceMinor: number
  imageUrl: string | null
  eligible: boolean
  ineligibleReason: string | null
}
export interface PortalOrder {
  orderId: string
  orderName: string
  customerName: string | null
  currency: string
  orderedAt: string
  fulfilledAt: string | null
  eligibleUntil: string
  windowOpen: boolean
  paymentGateway: string | null
  lines: PortalOrderLine[]
  reasons: Reason[]
  resolutions: ResolutionType[]
  storeCreditBonusMinor: number
  existingReturns: Array<{ id: string, number: string, status: ReturnStatus, createdAt: string }>
}
export interface CustomerReturn {
  id: string
  number: string
  status: ReturnStatus
  resolutionType: ResolutionType
  totalMinor: number
  bonusMinor: number
  creditMinor: number
  currency: string
  rejectionReason: string | null
  carrier: string | null
  trackingNumber: string | null
  lines: ReturnLine[]
  steps: Array<{ type: string, occurredAt: string }>
  createdAt: string
}
export interface Attachment { id: string, filename: string, contentType: string, size: number, createdAt: string }
export interface SubmitReturnRequest {
  resolutionType: ResolutionType
  lines: Array<{ orderLineId: string, quantity: number, reasonCode: string, exchangePreference?: string | null }>
  attachmentIds: string[]
  customerNote?: string | null
  locale: string
}

/** Erreur RFC 9457 renvoyée par le back-end. */
export interface ApiProblem { type: string, title: string, status: number, detail: string, code?: string }
