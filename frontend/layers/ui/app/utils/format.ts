// Formatage partagé : montants en unités mineures, dates et durées, dans la langue courante.

// fr-FR plutôt que fr-MA : séparateur de milliers en espace (« 28 112 MAD »), sans ambiguïté avec la virgule.
const LOCALES: Record<string, string> = { fr: 'fr-FR', en: 'en-US', ar: 'ar-MA' }

export function intlLocale(locale: string) {
  return LOCALES[locale] ?? 'fr-FR'
}

export function formatMoney(amountMinor: number, currency: string, locale = 'fr') {
  return new Intl.NumberFormat(intlLocale(locale), {
    style: 'currency',
    currency,
    maximumFractionDigits: amountMinor % 100 === 0 ? 0 : 2,
    numberingSystem: 'latn'
  }).format(amountMinor / 100)
}

export function formatDate(value: string | number | Date, locale = 'fr', withTime = false) {
  return new Intl.DateTimeFormat(intlLocale(locale), {
    day: 'numeric',
    month: 'short',
    year: withTime ? undefined : 'numeric',
    hour: withTime ? '2-digit' : undefined,
    minute: withTime ? '2-digit' : undefined,
    numberingSystem: 'latn'
  }).format(new Date(value))
}

export function formatRelative(value: string | number | Date, locale = 'fr') {
  const diffSeconds = (new Date(value).getTime() - Date.now()) / 1000
  const rtf = new Intl.RelativeTimeFormat(intlLocale(locale), { numeric: 'auto' })
  const units: Array<[Intl.RelativeTimeFormatUnit, number]> = [
    ['day', 86400], ['hour', 3600], ['minute', 60]
  ]
  for (const [unit, seconds] of units) {
    if (Math.abs(diffSeconds) >= seconds) return rtf.format(Math.round(diffSeconds / seconds), unit)
  }
  return rtf.format(0, 'minute')
}

/** 200 → « 3 h 20 », 45 → « 45 min ». */
export function formatMinutes(minutes: number | null | undefined) {
  if (minutes == null) return '—'
  if (minutes < 1) return '< 1 min'
  if (minutes < 60) return `${Math.round(minutes)} min`
  const h = Math.floor(minutes / 60)
  const m = Math.round(minutes % 60)
  return m ? `${h} h ${String(m).padStart(2, '0')}` : `${h} h`
}
