#!/usr/bin/env node
/**
 * API simulée de Loopa : même contrat HTTP que loopa-api (Spring Boot), données en mémoire chargées depuis
 * backend/src/main/resources/demo/seed.json. Sert au développement du front sans Java/PostgreSQL,
 * aux tests de bout en bout et aux captures d'écran.
 *
 *   node tools/mock-api/server.mjs            → http://localhost:8080
 *   PORT=8090 node tools/mock-api/server.mjs
 *
 * Comptes : yasmine@atlas-store.ma (propriétaire), salma@atlas-store.ma (admin), karim@atlas-store.ma (agent),
 * mot de passe Loopa2026!. Portail : boutique « atlas », commande #1042, nadia@exemple.ma ; le code est renvoyé
 * dans la réponse (devCode), comme le profil dev du back-end.
 */
import { createServer } from 'node:http'
import { readFileSync } from 'node:fs'
import { randomUUID, randomInt } from 'node:crypto'
import { fileURLToPath } from 'node:url'

const SEED_PATH = fileURLToPath(new URL('../../../backend/src/main/resources/demo/seed.json', import.meta.url))
const seed = JSON.parse(readFileSync(SEED_PATH, 'utf8'))
const PORT = Number(process.env.PORT ?? 8080)
const MINUTE = 60_000
const now = () => Date.now()
const iso = (ms) => (ms == null ? null : new Date(ms).toISOString())
const ago = (minutes) => (minutes == null ? null : now() - minutes * MINUTE)

/* ------------------------------------------------------------------ état */
let shop, users, policy, reasons, orders, returns, returnSeq, files, otps, tokens, lastSyncAt

/** (Ré)initialise toutes les données depuis seed.json : appelé au démarrage et par POST /__reset. */
function reset() {
  shop = { ...seed.shop, active: true }
  users = seed.users.map(u => ({ ...u, active: true, lastLoginAt: null, password: seed.password }))
  policy = { ...seed.policy, excludedProductTypes: [...seed.policy.excludedProductTypes] }
  reasons = seed.reasons.map((r, i) => ({ ...r, active: true, position: i }))
  orders = seed.orders.map(o => ({
    ...o,
    orderedAt: ago(o.orderedMinutesAgo),
    fulfilledAt: ago(o.fulfilledMinutesAgo),
    lines: o.lines.map(l => ({ ...l, id: randomUUID() })),
  }))
  returns = seed.returns.map(r => {
    const createdAt = ago(r.createdMinutesAgo)
    return {
      ...r,
      createdAt,
      approvedAt: r.approvedAfterMinutes == null ? null : createdAt + r.approvedAfterMinutes * MINUTE,
      updatedAt: ago(r.updatedMinutesAgo),
      resolutionError: null,
      attachmentIds: [],
      lines: r.lines.map(l => ({ ...l, id: randomUUID() })),
      history: r.history.map(h => ({ type: h.type, actor: h.actor, message: h.message, occurredAt: ago(h.minutesAgo) })),
    }
  })
  returnSeq = Math.max(...returns.map(r => Number(r.number.split('-').pop())))
  files = new Map()
  otps = new Map()
  tokens = new Map()
  lastSyncAt = null
}
reset()

const ALLOWED_NEXT = {
  SUBMITTED: ['APPROVED', 'REJECTED', 'CANCELLED'],
  APPROVED: ['IN_TRANSIT', 'RECEIVED', 'CANCELLED'],
  IN_TRANSIT: ['RECEIVED'],
  RECEIVED: ['RESOLVED', 'REJECTED'],
  REJECTED: [], CANCELLED: [], RESOLVED: [],
}
const PLANS = [
  { code: 'STARTER', name: 'Starter', priceMinor: 0, currency: 'MAD', quota: 20, features: ['Portail de retour', 'Remboursements', '1 utilisateur'] },
  { code: 'GROWTH', name: 'Growth', priceMinor: 29000, currency: 'MAD', quota: 300, features: ['Échanges et avoirs', 'Bonus shop-now', 'Statistiques', '5 utilisateurs'] },
  { code: 'SCALE', name: 'Scale', priceMinor: 89000, currency: 'MAD', quota: -1, features: ['Retours illimités', 'Transporteurs', 'API et webhooks', 'Utilisateurs illimités'] },
]

/* ------------------------------------------------------------------ outils */
class ApiError extends Error {
  constructor(status, code, detail) { super(detail); this.status = status; this.code = code }
}
const notFound = (what, id) => new ApiError(404, 'NOT_FOUND', `${what} introuvable : ${id}`)
const invalid = (detail) => new ApiError(422, 'INVALID_RETURN', detail)

function total(r) { return r.lines.reduce((s, l) => s + l.unitPriceMinor * l.quantity, 0) }
function returnView(r, withHistory = true) {
  const t = total(r)
  return {
    id: r.id, number: r.number, orderId: r.orderId, orderName: r.orderName, customerName: r.customerName,
    customerEmail: r.customerEmail, locale: r.locale, status: r.status, resolutionType: r.resolutionType,
    totalMinor: t, bonusMinor: r.bonusMinor, creditMinor: t + r.bonusMinor, currency: 'MAD',
    customerNote: r.customerNote, rejectionReason: r.rejectionReason, carrier: r.carrier,
    trackingNumber: r.trackingNumber, resolutionRef: r.resolutionRef, resolutionError: r.resolutionError,
    lines: r.lines.map(l => ({ id: l.id, orderLineId: l.orderLineId, title: l.title, variantTitle: l.variantTitle,
      quantity: l.quantity, unitPriceMinor: l.unitPriceMinor, reasonCode: l.reasonCode, exchangeVariantId: l.exchangeVariantId })),
    attachmentIds: r.attachmentIds,
    history: withHistory ? r.history.map(h => ({ ...h, occurredAt: iso(h.occurredAt) })) : [],
    createdAt: iso(r.createdAt), approvedAt: iso(r.approvedAt), updatedAt: iso(r.updatedAt),
  }
}
function customerReturn(r) {
  const v = returnView(r)
  return {
    id: v.id, number: v.number, status: v.status, resolutionType: v.resolutionType, totalMinor: v.totalMinor,
    bonusMinor: v.bonusMinor, creditMinor: v.creditMinor, currency: v.currency, rejectionReason: v.rejectionReason,
    carrier: v.carrier, trackingNumber: v.trackingNumber, lines: v.lines,
    steps: v.history.filter(h => !['NOTE', 'RESOLUTION_RETRY', 'RESOLUTION_FAILED'].includes(h.type))
      .map(h => ({ type: h.type, occurredAt: h.occurredAt })),
    createdAt: v.createdAt,
  }
}
function transition(r, target, actor, type, message) {
  if (!ALLOWED_NEXT[r.status].includes(target)) {
    throw new ApiError(409, 'INVALID_TRANSITION', `Le retour ${r.number} ne peut pas passer de ${r.status} à ${target}.`)
  }
  r.status = target
  r.updatedAt = now()
  if (target === 'APPROVED') r.approvedAt = r.updatedAt
  r.history.push({ type, actor, message: message ?? null, occurredAt: r.updatedAt })
  if (target === 'RECEIVED') {
    // Comme le module resolution : exécution asynchrone après réception.
    setTimeout(() => {
      if (r.status !== 'RECEIVED') return
      const prefix = { REFUND: 'REFUND', EXCHANGE: 'EXCHANGE', STORE_CREDIT: 'GIFTCARD' }[r.resolutionType]
      r.resolutionRef = `${prefix}-${r.number.split('-').pop()}`
      transition(r, 'RESOLVED', 'SYSTEM', 'RESOLVED', r.resolutionRef)
    }, 1500)
  }
}
function issueToken(claims) {
  const token = `mock.${randomUUID()}`
  tokens.set(token, { ...claims, exp: now() + 8 * 60 * MINUTE })
  return token
}
function auth(req, scopes) {
  const header = req.headers.authorization ?? ''
  const claims = tokens.get(header.replace(/^Bearer\s+/i, ''))
  if (!claims || claims.exp < now()) throw new ApiError(401, 'UNAUTHENTICATED', 'Authentification requise')
  if (!scopes.includes(claims.scope)) throw new ApiError(403, 'FORBIDDEN', 'Accès refusé')
  return claims
}
const findReturn = (id) => returns.find(r => r.id === id) ?? (() => { throw notFound('Retour', id) })()
const findOrder = (id) => orders.find(o => o.id === id) ?? (() => { throw notFound('Commande', id) })()
const reasonLabel = (code) => reasons.find(r => r.code === code)?.labelFr ?? code
const allowedResolutions = () => [
  ...(policy.allowStoreCredit ? ['STORE_CREDIT'] : []),
  ...(policy.allowExchange ? ['EXCHANGE'] : []),
  ...(policy.allowRefund ? ['REFUND'] : []),
]
function quantitiesInProgress(orderId) {
  const q = {}
  for (const r of returns.filter(r => r.orderId === orderId && !['CANCELLED', 'REJECTED'].includes(r.status))) {
    for (const l of r.lines) q[l.orderLineId] = (q[l.orderLineId] ?? 0) + l.quantity
  }
  return q
}
function eligibility(order) {
  const ref = order.fulfilledAt ?? order.orderedAt
  const until = ref + policy.windowDays * 24 * 60 * MINUTE
  const windowOpen = now() < until
  const used = quantitiesInProgress(order.id)
  return {
    until, windowOpen,
    lines: order.lines.map(l => {
      const remaining = Math.max(0, l.quantity - (used[l.externalLineId] ?? 0))
      const reason = !windowOpen ? 'WINDOW_EXPIRED'
        : policy.excludedProductTypes.some(t => t.toLowerCase() === (l.productType ?? '').toLowerCase()) ? 'EXCLUDED_PRODUCT'
          : remaining === 0 ? 'ALREADY_RETURNED' : null
      return { line: l, returnable: reason ? 0 : remaining, eligible: !reason, reason }
    }),
  }
}
const digits = (s) => (s ?? '').replace(/\D/g, '')
function contactMatches(order, contact) {
  const c = contact.trim()
  if (c.includes('@')) return (order.customerEmail ?? '').toLowerCase() === c.toLowerCase()
  const d = digits(c)
  const p = digits(order.customerPhone)
  return d.length >= 9 && p.length >= 9 && p.endsWith(d.slice(-9))
}
function mask(contact) {
  const at = contact.indexOf('@')
  return at > 0 ? `${contact[0]}•••${contact.slice(at)}` : `•••• ${digits(contact).slice(-2)}`
}

/* ------------------------------------------------------------------ statistiques */
function summary(days) {
  const period = Math.min(Math.max(days, 1), 365)
  const start = new Date(); start.setUTCHours(0, 0, 0, 0)
  const since = start.getTime() - (period - 1) * 24 * 60 * MINUTE
  const rs = returns.filter(r => r.createdAt >= since)
  const count = (s) => rs.filter(r => r.status === s).length
  const live = rs.filter(r => !['REJECTED', 'CANCELLED'].includes(r.status))
  const sumBy = (type) => live.filter(r => r.resolutionType === type).reduce((s, r) => s + total(r), 0)
  const approvals = rs.filter(r => r.approvedAt).map(r => (r.approvedAt - r.createdAt) / MINUTE).sort((a, b) => a - b)
  const median = approvals.length === 0 ? null
    : approvals.length % 2 ? approvals[(approvals.length - 1) / 2]
      : (approvals[approvals.length / 2 - 1] + approvals[approvals.length / 2]) / 2
  const tally = (items) => Object.entries(items.reduce((m, k) => ({ ...m, [k[0]]: (m[k[0]] ?? 0) + k[1] }), {}))
    .map(([key, count]) => ({ key, count })).sort((a, b) => b.count - a.count)
  const daily = []
  for (let i = 0; i < period; i++) {
    const d = since + i * 24 * 60 * MINUTE
    daily.push({ date: new Date(d).toISOString().slice(0, 10), count: rs.filter(r => r.createdAt >= d && r.createdAt < d + 24 * 60 * MINUTE).length })
  }
  return {
    days: period, currency: 'MAD', totalReturns: rs.length, pending: count('SUBMITTED'),
    approved: count('APPROVED') + count('IN_TRANSIT') + count('RECEIVED'), rejected: count('REJECTED'), resolved: count('RESOLVED'),
    retainedMinor: sumBy('STORE_CREDIT') + sumBy('EXCHANGE'), refundedMinor: sumBy('REFUND'),
    bonusMinor: live.reduce((s, r) => s + r.bonusMinor, 0), medianApprovalMinutes: median,
    autoApproved: rs.filter(r => r.history.some(h => h.type === 'APPROVED' && h.actor === 'SYSTEM')).length,
    reasons: tally(rs.flatMap(r => r.lines.map(l => [l.reasonCode, 1]))),
    resolutions: tally(live.map(r => [r.resolutionType, 1])),
    topProducts: tally(rs.flatMap(r => r.lines.map(l => [l.title, l.quantity]))).slice(0, 5),
    daily,
  }
}

/* ------------------------------------------------------------------ routes */
const routes = []
const route = (method, pattern, handler) => {
  const keys = []
  const regex = new RegExp('^' + pattern.replace(/:(\w+)/g, (_, k) => { keys.push(k); return '([^/]+)' }) + '$')
  routes.push({ method, regex, keys, handler })
}

// Outil de démo : remet les données dans leur état initial (captures, tests de bout en bout).
route('POST', '/__reset', () => { reset(); return { reset: true } })

// Authentification marchand
route('POST', '/api/auth/login', ({ body }) => {
  const u = users.find(x => x.email.toLowerCase() === String(body.email ?? '').trim().toLowerCase())
  if (!u || !u.active || u.password !== body.password) throw new ApiError(401, 'BAD_CREDENTIALS', 'Email ou mot de passe incorrect.')
  u.lastLoginAt = now()
  const token = issueToken({ sub: u.id, scope: u.role === 'AGENT' ? 'agent' : 'admin', name: u.name })
  return { token, expiresAt: iso(now() + 8 * 60 * MINUTE), user: userDto(u), shop: shopDto() }
})
route('POST', '/api/auth/password', ({ req, body }) => {
  const c = auth(req, ['admin', 'agent']); const u = users.find(x => x.id === c.sub)
  if (u.password !== body.currentPassword) throw new ApiError(422, 'BAD_CREDENTIALS', 'Le mot de passe actuel est incorrect.')
  if (String(body.newPassword ?? '').length < 10) throw new ApiError(400, 'VALIDATION', 'Le nouveau mot de passe doit faire au moins 10 caractères.')
  if (body.newPassword === body.currentPassword) throw new ApiError(422, 'SAME_PASSWORD', 'Le nouveau mot de passe doit être différent de l\'actuel.')
  u.password = body.newPassword
  return { status: 204, body: {} }
})
route('GET', '/api/auth/me', ({ req }) => {
  const c = auth(req, ['admin', 'agent'])
  return { token: null, expiresAt: null, user: userDto(users.find(u => u.id === c.sub)), shop: shopDto() }
})
const userDto = (u) => ({ id: u.id, email: u.email, name: u.name, role: u.role })
const shopDto = () => ({ id: shop.id, handle: shop.handle, name: shop.name, currency: shop.currency, plan: shop.plan, platform: shop.platform })

// Retours (admin)
route('GET', '/api/admin/v1/returns', ({ req, query }) => {
  auth(req, ['admin', 'agent'])
  const limit = Math.min(Math.max(Number(query.get('limit') ?? 25), 1), 100)
  const status = query.get('status')
  const q = (query.get('q') ?? '').trim().toLowerCase()
  let list = [...returns].sort((a, b) => b.createdAt - a.createdAt)
    .filter(r => !status || r.status === status)
    .filter(r => !q || [r.number, r.customerEmail, r.customerName, r.orderName].some(v => (v ?? '').toLowerCase().includes(q)))
  const cursor = query.get('cursor')
  if (cursor) list = list.filter(r => r.createdAt < findReturn(cursor).createdAt)
  const items = list.slice(0, limit).map(r => returnView(r, false))
  const counts = Object.fromEntries(Object.keys(ALLOWED_NEXT).map(s => [s, returns.filter(r => r.status === s).length]))
  return { items, nextCursor: items.length === limit ? items.at(-1).id : null, counts }
})
route('GET', '/api/admin/v1/returns/export.csv', ({ req, query }) => {
  auth(req, ['admin', 'agent'])
  const status = query.get('status')
  const cell = (v) => v == null ? '' : `"${String(/^[=+\-@]/.test(String(v)) ? `'${v}` : v).replace(/"/g, '""')}"`
  const dec = (m) => (m / 100).toFixed(2).replace('.', ',')
  const rows = [...returns].sort((a, b) => b.createdAt - a.createdAt).filter(r => !status || r.status === status).map((r) => {
    const v = returnView(r)
    return [cell(v.number), cell(v.createdAt), cell(v.orderName), cell(v.customerName), cell(v.customerEmail), cell(v.status),
      cell(v.resolutionType), cell(v.lines.map(l => `${l.title} x${l.quantity}`).join(', ')),
      cell([...new Set(v.lines.map(l => l.reasonCode))].join(', ')), dec(v.totalMinor), dec(v.bonusMinor), dec(v.creditMinor),
      cell(v.currency), cell(v.trackingNumber), cell(v.resolutionRef)].join(';')
  })
  const csv = '\uFEFFNuméro;Date;Commande;Client;Email;Statut;Solution;Articles;Motifs;Montant;Bonus;Rendu;Devise;Suivi;Référence\n' + rows.join('\n') + '\n'
  return { raw: Buffer.from(csv, 'utf8'), contentType: 'text/csv; charset=utf-8' }
})
route('POST', '/api/admin/v1/returns/bulk-approve', ({ req, body }) => {
  const c = auth(req, ['admin', 'agent'])
  const approved = []; const failed = {}
  for (const id of body.ids ?? []) {
    try { transition(findReturn(id), 'APPROVED', c.name, 'APPROVED'); approved.push(id) } catch (e) { failed[id] = e.message }
  }
  return { approved, failed }
})
route('GET', '/api/admin/v1/returns/:id', ({ req, params }) => { auth(req, ['admin', 'agent']); return returnView(findReturn(params.id)) })
route('POST', '/api/admin/v1/returns/:id/approve', ({ req, params }) => {
  const c = auth(req, ['admin', 'agent']); const r = findReturn(params.id)
  transition(r, 'APPROVED', c.name, 'APPROVED'); return returnView(r)
})
route('POST', '/api/admin/v1/returns/:id/reject', ({ req, params, body }) => {
  const c = auth(req, ['admin', 'agent']); const r = findReturn(params.id)
  if (!String(body.reason ?? '').trim()) throw new ApiError(400, 'VALIDATION', 'Le motif du rejet est obligatoire.')
  transition(r, 'REJECTED', c.name, 'REJECTED', body.reason.trim()); r.rejectionReason = body.reason.trim(); return returnView(r)
})
route('POST', '/api/admin/v1/returns/:id/receive', ({ req, params }) => {
  const c = auth(req, ['admin', 'agent']); const r = findReturn(params.id)
  transition(r, 'RECEIVED', c.name, 'RECEIVED'); return returnView(r)
})
route('POST', '/api/admin/v1/returns/:id/resolve', ({ req, params }) => {
  const c = auth(req, ['admin', 'agent']); const r = findReturn(params.id)
  if (r.status !== 'RECEIVED') throw new ApiError(409, 'NOT_RECEIVED', 'Le retour doit être reçu avant d\'exécuter la résolution.')
  r.history.push({ type: 'RESOLUTION_RETRY', actor: c.name, message: null, occurredAt: now() })
  r.status = 'APPROVED'; transition(r, 'RECEIVED', 'SYSTEM', 'RECEIVED')
  return returnView(r)
})
route('POST', '/api/admin/v1/returns/:id/notes', ({ req, params, body }) => {
  const c = auth(req, ['admin', 'agent']); const r = findReturn(params.id)
  if (!String(body.note ?? '').trim()) throw new ApiError(400, 'VALIDATION', 'La note est vide.')
  r.history.push({ type: 'NOTE', actor: c.name, message: body.note.trim(), occurredAt: now() }); return returnView(r)
})
route('GET', '/api/admin/v1/files/:id', ({ req, params }) => {
  auth(req, ['admin', 'agent'])
  const f = files.get(params.id); if (!f) throw notFound('Fichier', params.id)
  return { raw: f.data, contentType: f.contentType }
})

// Statistiques
route('GET', '/api/admin/v1/analytics/summary', ({ req, query }) => { auth(req, ['admin', 'agent']); return summary(Number(query.get('days') ?? 30)) })

// Réglages
route('GET', '/api/admin/v1/settings/policy', ({ req }) => { auth(req, ['admin', 'agent']); return policy })
route('PUT', '/api/admin/v1/settings/policy', ({ req, body }) => {
  auth(req, ['admin'])
  if (!body.allowRefund && !body.allowExchange && !body.allowStoreCredit) throw new ApiError(422, 'INVALID_POLICY', 'Au moins une solution (remboursement, échange ou avoir) doit rester proposée.')
  if (!(body.windowDays >= 1 && body.windowDays <= 365)) throw new ApiError(400, 'VALIDATION', 'Le délai doit être compris entre 1 et 365 jours.')
  policy = { ...policy, ...body }; return policy
})
route('GET', '/api/admin/v1/settings/reasons', ({ req }) => { auth(req, ['admin', 'agent']); return reasons })
route('PUT', '/api/admin/v1/settings/reasons', ({ req, body }) => {
  auth(req, ['admin'])
  const incoming = body.map((r, i) => ({ ...r, position: i }))
  const removed = reasons.filter(r => !incoming.some(x => x.code === r.code)).map(r => ({ ...r, active: false, position: 999 }))
  reasons = [...incoming, ...removed]; return reasons
})
route('GET', '/api/admin/v1/settings/branding', ({ req }) => { auth(req, ['admin', 'agent']); return brandingDto() })
route('PUT', '/api/admin/v1/settings/branding', ({ req, body }) => {
  auth(req, ['admin'])
  if (!/^#[0-9A-Fa-f]{6}$/.test(body.primaryColor ?? '')) throw new ApiError(400, 'VALIDATION', 'Couleur invalide (format #RRGGBB).')
  Object.assign(shop, { name: body.name, primaryColor: body.primaryColor.toUpperCase(), logoUrl: body.logoUrl || null, welcomeMessage: body.welcomeMessage || null })
  return brandingDto()
})
const brandingDto = () => ({ name: shop.name, primaryColor: shop.primaryColor, logoUrl: shop.logoUrl, welcomeMessage: shop.welcomeMessage, handle: shop.handle })

// Équipe
const memberDto = (u) => ({ id: u.id, email: u.email, name: u.name, role: u.role, active: u.active, lastLoginAt: iso(u.lastLoginAt) })
route('GET', '/api/admin/v1/team', ({ req }) => { auth(req, ['admin']); return users.map(memberDto) })
route('POST', '/api/admin/v1/team', ({ req, body }) => {
  auth(req, ['admin'])
  if (body.role === 'OWNER') throw new ApiError(422, 'OWNER_UNIQUE', 'Une boutique n\'a qu\'un propriétaire.')
  if (users.some(u => u.email.toLowerCase() === String(body.email).toLowerCase())) throw new ApiError(409, 'EMAIL_TAKEN', 'Cet email est déjà utilisé.')
  const temporaryPassword = randomUUID().slice(0, 12)
  const u = { id: randomUUID(), email: body.email.toLowerCase(), name: body.name, role: body.role, active: true, lastLoginAt: null, password: temporaryPassword }
  users.push(u); return { member: memberDto(u), temporaryPassword }
})
route('PATCH', '/api/admin/v1/team/:id', ({ req, params, body }) => {
  auth(req, ['admin'])
  const u = users.find(x => x.id === params.id); if (!u) throw notFound('Membre', params.id)
  if (u.role === 'OWNER') throw new ApiError(422, 'OWNER_LOCKED', 'Le propriétaire ne peut pas être modifié.')
  if (body.role && body.role !== 'OWNER') u.role = body.role
  if (typeof body.active === 'boolean') u.active = body.active
  return memberDto(u)
})

// Abonnement
route('GET', '/api/admin/v1/billing', ({ req }) => {
  auth(req, ['admin'])
  const plan = PLANS.find(p => p.code === shop.plan)
  const first = new Date(); first.setUTCDate(1); first.setUTCHours(0, 0, 0, 0)
  return { plan: plan.code, quota: plan.quota, used: returns.filter(r => r.createdAt >= first.getTime()).length, periodStart: first.toISOString().slice(0, 10), plans: PLANS }
})
route('POST', '/api/admin/v1/billing/subscribe', ({ req, body }) => {
  auth(req, ['admin'])
  if (!PLANS.some(p => p.code === body.plan)) throw new ApiError(422, 'UNKNOWN_PLAN', `Plan inconnu : ${body.plan}`)
  shop.plan = body.plan; return { plan: shop.plan, confirmationUrl: null }
})

// Intégrations
route('GET', '/api/admin/v1/integrations', ({ req }) => {
  auth(req, ['admin'])
  return { platform: shop.platform, shopDomain: null, connected: false, appConfigured: false, ordersSynced: orders.length, lastSyncAt: iso(lastSyncAt) }
})
route('POST', '/api/admin/v1/integrations/shopify/sync', ({ req }) => {
  auth(req, ['admin'])
  throw new ApiError(409, 'SHOPIFY_NOT_CONNECTED', 'Boutique Shopify non connectée.')
})

// Portail : public
route('GET', '/api/portal/v1/shops/:handle', ({ params }) => {
  if (params.handle !== shop.handle) throw notFound('Boutique', params.handle)
  return { handle: shop.handle, name: shop.name, primaryColor: shop.primaryColor, logoUrl: shop.logoUrl, welcomeMessage: shop.welcomeMessage,
    currency: shop.currency, defaultLocale: shop.defaultLocale, windowDays: policy.windowDays, storeCreditBonusMinor: policy.storeCreditBonusMinor }
})
route('POST', '/api/portal/v1/sessions', ({ body }) => {
  if (body.shop !== shop.handle) throw notFound('Boutique', body.shop)
  const normalized = String(body.orderNumber ?? '').replace(/[^0-9A-Za-z]/g, '').toUpperCase()
  const order = orders.find(o => o.name.replace(/[^0-9A-Za-z]/g, '').toUpperCase() === normalized && contactMatches(o, String(body.contact ?? '')))
  if (!order) throw new ApiError(404, 'ORDER_NOT_FOUND', 'Aucune commande ne correspond à ce numéro et à ce contact.')
  const code = String(randomInt(0, 1_000_000)).padStart(6, '0')
  const sessionId = randomUUID()
  otps.set(sessionId, { code, orderId: order.id, contact: body.contact.trim(), attempts: 0, expiresAt: now() + 10 * MINUTE })
  return { sessionId, channel: body.contact.includes('@') ? 'EMAIL' : 'SMS', maskedContact: mask(body.contact.trim()), expiresAt: iso(now() + 10 * MINUTE), devCode: code }
})
route('POST', '/api/portal/v1/sessions/verify', ({ body }) => {
  const otp = otps.get(body.sessionId)
  if (!otp) throw new ApiError(422, 'INVALID_CODE', 'Code incorrect.')
  if (otp.expiresAt < now() || otp.consumed) throw new ApiError(410, 'CODE_EXPIRED', 'Ce code a expiré. Demandez-en un nouveau.')
  if (otp.attempts >= 5) throw new ApiError(429, 'TOO_MANY_ATTEMPTS', 'Trop d\'essais. Demandez un nouveau code.')
  otp.attempts++
  if (String(body.code ?? '').trim() !== otp.code) throw new ApiError(422, 'INVALID_CODE', 'Code incorrect.')
  otp.consumed = true
  const token = issueToken({ scope: 'portal', orderId: otp.orderId, sub: otp.contact })
  return { token, expiresAt: iso(now() + 30 * MINUTE), orderId: otp.orderId }
})

// Portail : avec jeton
route('GET', '/api/portal/v1/order', ({ req }) => {
  const c = auth(req, ['portal']); const order = findOrder(c.orderId); const e = eligibility(order)
  return {
    orderId: order.id, orderName: order.name, customerName: order.customerName, currency: order.currency,
    orderedAt: iso(order.orderedAt), fulfilledAt: iso(order.fulfilledAt), eligibleUntil: iso(e.until), windowOpen: e.windowOpen,
    paymentGateway: order.paymentGateway,
    lines: e.lines.map(({ line: l, returnable, eligible, reason }) => ({ orderLineId: l.externalLineId, title: l.title, variantTitle: l.variantTitle,
      sku: l.sku, productType: l.productType, quantity: l.quantity, returnableQuantity: returnable, unitPriceMinor: l.unitPriceMinor,
      imageUrl: l.imageUrl, eligible, ineligibleReason: reason })),
    reasons: reasons.filter(r => r.active), resolutions: allowedResolutions(), storeCreditBonusMinor: policy.storeCreditBonusMinor,
    existingReturns: returns.filter(r => r.orderId === order.id).sort((a, b) => b.createdAt - a.createdAt)
      .map(r => ({ id: r.id, number: r.number, status: r.status, createdAt: iso(r.createdAt) })),
  }
})
route('POST', '/api/portal/v1/attachments', async ({ req, form }) => {
  auth(req, ['portal'])
  const file = form?.get('file')
  if (!file || typeof file === 'string') throw new ApiError(400, 'VALIDATION', 'Fichier manquant.')
  if (!['image/jpeg', 'image/png', 'image/webp', 'image/heic', 'image/heif', 'application/pdf'].includes(file.type)) throw new ApiError(415, 'UNSUPPORTED_FILE', 'Formats acceptés : JPEG, PNG, WebP, HEIC ou PDF.')
  if (file.size > 10 * 1024 * 1024) throw new ApiError(413, 'FILE_TOO_LARGE', 'Le fichier doit faire moins de 10 Mo.')
  const id = randomUUID()
  files.set(id, { data: Buffer.from(await file.arrayBuffer()), contentType: file.type, filename: file.name })
  return { status: 201, body: { id, filename: file.name, contentType: file.type, size: file.size, createdAt: iso(now()) } }
})
route('POST', '/api/portal/v1/returns', ({ req, body }) => {
  const c = auth(req, ['portal']); const order = findOrder(c.orderId)
  if (!allowedResolutions().includes(body.resolutionType)) throw invalid('Cette solution n\'est pas proposée par la boutique.')
  if (!Array.isArray(body.lines) || body.lines.length === 0) throw invalid('Choisissez au moins un article.')
  const e = eligibility(order)
  let photoRequired = false
  const lines = body.lines.map(lr => {
    const el = e.lines.find(x => x.line.externalLineId === lr.orderLineId); if (!el) throw invalid('Article inconnu.')
    if (!el.eligible || lr.quantity < 1 || lr.quantity > el.returnable) throw invalid(`« ${el.line.title} » ne peut pas être retourné dans cette quantité.`)
    const reason = reasons.find(r => r.active && r.code === lr.reasonCode); if (!reason) throw invalid('Motif inconnu.')
    photoRequired ||= reason.requiresPhoto
    return { id: randomUUID(), orderLineId: el.line.externalLineId, title: el.line.title, variantTitle: el.line.variantTitle, quantity: lr.quantity,
      unitPriceMinor: el.line.unitPriceMinor, currency: 'MAD', reasonCode: reason.code,
      exchangeVariantId: body.resolutionType === 'EXCHANGE' ? (lr.exchangePreference?.trim() || null) : null }
  })
  const attachmentIds = body.attachmentIds ?? []
  if (photoRequired && attachmentIds.length === 0) throw invalid('Ajoutez au moins une photo pour ce motif.')
  if (attachmentIds.some(id => !files.has(id))) throw invalid('Photo introuvable.')
  const created = now()
  const r = {
    id: randomUUID(), number: `RMA-2026-${String(++returnSeq).padStart(6, '0')}`, orderId: order.id, orderName: order.name,
    customerName: order.customerName, customerEmail: order.customerEmail, locale: ['fr', 'en', 'ar'].includes(body.locale) ? body.locale : 'fr',
    status: 'SUBMITTED', resolutionType: body.resolutionType, bonusMinor: body.resolutionType === 'STORE_CREDIT' ? policy.storeCreditBonusMinor : 0,
    customerNote: body.customerNote?.trim() || null, rejectionReason: null, carrier: null, trackingNumber: null, resolutionRef: null, resolutionError: null,
    createdAt: created, approvedAt: null, updatedAt: created, attachmentIds, lines,
    history: [{ type: 'SUBMITTED', actor: 'CUSTOMER', message: null, occurredAt: created }],
  }
  returns.push(r)
  if (policy.autoApprove && total(r) <= policy.autoApproveMaxAmountMinor && !photoRequired) transition(r, 'APPROVED', 'SYSTEM', 'APPROVED', 'Approbation automatique')
  return { status: 201, body: customerReturn(r) }
})
const portalReturn = (req, id) => {
  const c = auth(req, ['portal']); const r = findReturn(id)
  if (r.orderId !== c.orderId) throw notFound('Retour', id)
  return r
}
route('GET', '/api/portal/v1/returns/:id', ({ req, params }) => customerReturn(portalReturn(req, params.id)))
route('POST', '/api/portal/v1/returns/:id/tracking', ({ req, params, body }) => {
  const r = portalReturn(req, params.id)
  if (!String(body.trackingNumber ?? '').trim()) throw new ApiError(400, 'VALIDATION', 'Le numéro de suivi est obligatoire.')
  transition(r, 'IN_TRANSIT', 'CUSTOMER', 'SHIPPED', `${body.carrier ? body.carrier + ' · ' : ''}${body.trackingNumber.trim()}`)
  r.carrier = body.carrier || null; r.trackingNumber = body.trackingNumber.trim()
  return customerReturn(r)
})
route('POST', '/api/portal/v1/returns/:id/cancel', ({ req, params }) => {
  const r = portalReturn(req, params.id); transition(r, 'CANCELLED', 'CUSTOMER', 'CANCELLED'); return customerReturn(r)
})

/* ------------------------------------------------------------------ serveur */
const CORS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, PUT, PATCH, DELETE, OPTIONS',
  'Access-Control-Allow-Headers': 'Authorization, Content-Type, Accept-Language',
}
createServer(async (req, res) => {
  const url = new URL(req.url, `http://${req.headers.host}`)
  if (req.method === 'OPTIONS') { res.writeHead(204, CORS); return res.end() }
  const match = routes.map(r => ({ r, m: r.method === req.method && url.pathname.match(r.regex) })).find(x => x.m)
  try {
    if (!match) throw new ApiError(404, 'NOT_FOUND', `Route inconnue : ${req.method} ${url.pathname}`)
    const params = Object.fromEntries(match.r.keys.map((k, i) => [k, decodeURIComponent(match.m[i + 1])]))
    const type = req.headers['content-type'] ?? ''
    let body = {}
    let form = null
    if (type.startsWith('multipart/form-data')) {
      form = await new Request(url, { method: 'POST', headers: req.headers, body: req, duplex: 'half' }).formData()
    } else if (['POST', 'PUT', 'PATCH'].includes(req.method)) {
      const chunks = []; for await (const c of req) chunks.push(c)
      body = chunks.length ? JSON.parse(Buffer.concat(chunks).toString('utf8')) : {}
    }
    await new Promise(r => setTimeout(r, 120)) // latence réaliste
    let out = await match.r.handler({ req, params, query: url.searchParams, body, form })
    if (out && out.raw) { res.writeHead(200, { ...CORS, 'Content-Type': out.contentType }); return res.end(out.raw) }
    const status = out && out.status && out.body ? out.status : 200
    if (out && out.status && out.body) out = out.body
    res.writeHead(status, { ...CORS, 'Content-Type': 'application/json; charset=utf-8' })
    res.end(JSON.stringify(out))
  } catch (e) {
    const status = e instanceof ApiError ? e.status : 500
    if (!(e instanceof ApiError)) console.error(e)
    res.writeHead(status, { ...CORS, 'Content-Type': 'application/problem+json; charset=utf-8' })
    res.end(JSON.stringify({ type: `https://loopa.app/errors/${(e.code ?? 'internal').toLowerCase().replace(/_/g, '-')}`,
      title: status === 500 ? 'Internal Server Error' : 'Error', status, detail: e.message, code: e.code ?? 'INTERNAL' }))
  }
}).listen(PORT, () => {
  console.log(`API simulée Loopa sur http://localhost:${PORT} — ${orders.length} commandes, ${returns.length} retours`)
  console.log(`Admin : ${users[0].email} / ${seed.password} · Portail : boutique « ${shop.handle} », commande #1042, nadia@exemple.ma`)
})
