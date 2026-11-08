<script setup lang="ts">
import { useQueryCache } from '@pinia/colada'

const route = useRoute()
const { t, locale } = useI18n()
const toast = useToast()
const portal = usePortalApi()
const cache = useQueryCache()
const shopHandle = String(route.params.shop)
const id = computed(() => String(route.params.id))

if (!portal.hasSession.value) {
  await navigateTo(`/${shopHandle}`, { replace: true })
}

const { data: ret, isPending, refetch } = usePortalReturn(id)
const carrier = ref('Amana')
const tracking = ref('')
const saving = ref(false)

const CARRIERS = ['Amana', 'Sendit', 'CTM Messagerie', 'Cathedis', 'Autre']
const money = (minor: number) => formatMoney(minor, ret.value?.currency ?? 'MAD', locale.value)
const resolutionLabel = computed(() => (ret.value ? t(`resolution.${ret.value.resolutionType}`).toLowerCase() : ''))

const hero = computed(() => {
  switch (ret.value?.status) {
    case 'APPROVED': return { icon: 'i-lucide-check', tone: 'bg-primary/10 text-primary', title: t('portal.approvedTitle'), text: t('portal.approvedText', { number: ret.value.number }) }
    case 'IN_TRANSIT': return { icon: 'i-lucide-truck', tone: 'bg-sky-100 text-sky-700', title: t('portal.transitTitle'), text: t('portal.transitText') }
    case 'RECEIVED': return { icon: 'i-lucide-package-check', tone: 'bg-primary/10 text-primary', title: t('portal.receivedTitle'), text: t('portal.receivedText', { resolution: resolutionLabel.value }) }
    case 'RESOLVED': return { icon: 'i-lucide-party-popper', tone: 'bg-primary/10 text-primary', title: t('portal.resolvedTitle'), text: t('portal.resolvedText', { resolution: resolutionLabel.value, amount: money(ret.value.creditMinor) }) }
    case 'REJECTED': return { icon: 'i-lucide-x', tone: 'bg-red-100 text-red-700', title: t('portal.rejectedTitle'), text: t('portal.rejectedText', { reason: ret.value.rejectionReason ?? '' }) }
    case 'CANCELLED': return { icon: 'i-lucide-ban', tone: 'bg-elevated text-muted', title: t('portal.cancelledTitle'), text: t('portal.cancelledText') }
    default: return { icon: 'i-lucide-hourglass', tone: 'bg-amber-100 text-amber-800', title: t('portal.submittedTitle'), text: t('portal.submittedText') }
  }
})

const PROGRESS = ['SUBMITTED', 'APPROVED', 'SHIPPED', 'RECEIVED', 'RESOLVED'] as const
const progress = computed(() => PROGRESS.map(type => ({
  type,
  label: t(`portal.progress.${type}`, { resolution: t(`resolution.${ret.value?.resolutionType ?? 'REFUND'}`) }),
  at: ret.value?.steps.find(s => s.type === type)?.occurredAt ?? null
})))

async function saveTracking() {
  if (!tracking.value.trim()) return
  saving.value = true
  try {
    await portal.addTracking(id.value, carrier.value, tracking.value.trim())
    await refetch()
    cache.invalidateQueries({ key: ['portal-order'] })
  } catch (e) {
    toast.add({ title: apiErrorMessage(e), color: 'error' })
  } finally {
    saving.value = false
  }
}

async function cancel() {
  if (!window.confirm(t('portal.cancelConfirm'))) return
  try {
    await portal.cancel(id.value)
    await refetch()
    cache.invalidateQueries({ key: ['portal-order'] })
  } catch (e) {
    toast.add({ title: apiErrorMessage(e), color: 'error' })
  }
}
</script>

<template>
  <section class="space-y-6">
    <USkeleton v-if="isPending" class="h-72 rounded-xl" />
    <template v-else-if="ret">
      <div class="space-y-3 text-center">
        <div class="mx-auto grid size-16 place-items-center rounded-full" :class="hero.tone">
          <UIcon :name="hero.icon" class="size-8" />
        </div>
        <h1 class="text-3xl font-extrabold">
          {{ hero.title }}
        </h1>
        <p class="text-muted">
          {{ hero.text }}
        </p>
      </div>

      <UCard v-if="ret.status === 'APPROVED'">
        <form class="space-y-4" @submit.prevent="saveTracking">
          <p class="font-semibold">
            {{ t('portal.trackingTitle') }}
          </p>
          <div class="grid gap-3 sm:grid-cols-[160px_1fr]">
            <UFormField :label="t('portal.carrier')">
              <USelect v-model="carrier" :items="CARRIERS" class="w-full" />
            </UFormField>
            <UFormField :label="t('portal.trackingNumber')">
              <UInput v-model="tracking" placeholder="AM123456789MA" class="w-full font-mono" />
            </UFormField>
          </div>
          <UButton type="submit" :label="t('portal.sendTracking')" block size="lg" :loading="saving" :disabled="!tracking.trim()" />
        </form>
      </UCard>

      <UCard>
        <template #header>
          <div class="flex items-center justify-between">
            <span class="font-semibold">{{ ret.number }}</span>
            <StatusBadge :status="ret.status" />
          </div>
        </template>
        <ol v-if="ret.status !== 'REJECTED' && ret.status !== 'CANCELLED'" class="space-y-4">
          <li v-for="(s, i) in progress" :key="s.type" class="flex items-start gap-3">
            <span
              class="grid size-7 shrink-0 place-items-center rounded-full text-xs font-bold"
              :class="s.at ? 'bg-primary text-white' : 'bg-elevated text-muted'"
            >
              <UIcon v-if="s.at" name="i-lucide-check" class="size-4" />
              <template v-else>{{ i + 1 }}</template>
            </span>
            <span class="flex flex-col">
              <span :class="s.at ? 'font-semibold' : 'text-muted'">{{ s.label }}</span>
              <span v-if="s.at" class="text-xs text-muted">{{ formatDate(s.at, locale, true) }}</span>
            </span>
          </li>
        </ol>
        <ul class="mt-4 space-y-2 border-t border-default pt-4 text-sm">
          <li v-for="l in ret.lines" :key="l.id" class="flex justify-between gap-3">
            <span>{{ l.title }} · {{ l.variantTitle }} × {{ l.quantity }}</span>
            <MoneyText :amount-minor="l.unitPriceMinor * l.quantity" :currency="ret.currency" />
          </li>
          <li v-if="ret.bonusMinor" class="flex justify-between font-medium text-coral-800">
            <span>{{ t('admin.bonus') }}</span>
            <span>+<MoneyText :amount-minor="ret.bonusMinor" :currency="ret.currency" /></span>
          </li>
          <li class="flex justify-between border-t border-default pt-2 font-bold">
            <span>{{ t(`resolution.${ret.resolutionType}`) }}</span>
            <MoneyText :amount-minor="ret.creditMinor" :currency="ret.currency" />
          </li>
        </ul>
        <p v-if="ret.trackingNumber" class="mt-3 text-sm text-muted">
          <UIcon name="i-lucide-truck" class="me-1 inline size-4" />{{ ret.carrier }} · <span class="font-mono">{{ ret.trackingNumber }}</span>
        </p>
      </UCard>

      <div class="flex flex-col items-center gap-2">
        <UButton :label="t('portal.newReturn')" variant="link" :to="`/${shopHandle}/order`" />
        <UButton
          v-if="ret.status === 'SUBMITTED' || ret.status === 'APPROVED'"
          :label="t('portal.cancelReturn')"
          color="neutral"
          variant="ghost"
          size="sm"
          @click="cancel"
        />
      </div>
    </template>
  </section>
</template>
