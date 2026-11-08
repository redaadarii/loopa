<script setup lang="ts">
import type { ReturnStatus } from '../../../../layers/api/app/types/api'

const { t, locale } = useI18n()
const { session } = useAuth()
const { data: summary, isPending } = useAnalytics(30)
const filters = ref<{ status?: ReturnStatus, q?: string }>({ status: 'SUBMITTED' })
const { items: pending } = useAdminReturns(filters)
const { data: reasons } = useReasons()

const money = (minor: number) => formatMoney(minor, summary.value?.currency ?? 'MAD', locale.value)
const reasonLabel = (code: string) => {
  const r = reasons.value?.find(x => x.code === code)
  if (!r) return code
  return locale.value === 'en' ? r.labelEn : locale.value === 'ar' ? r.labelAr : r.labelFr
}

const reasonItems = computed(() => (summary.value?.reasons ?? []).map(r => ({ label: reasonLabel(r.key), count: r.count })))
const resolutionItems = computed(() => (summary.value?.resolutions ?? []).map(r => ({ label: t(`resolution.${r.key}`), count: r.count })))
const productItems = computed(() => (summary.value?.topProducts ?? []).map(r => ({ label: r.key, count: r.count })))
const topReason = computed(() => {
  const list = summary.value?.reasons ?? []
  const total = list.reduce((s, r) => s + r.count, 0)
  return list[0] && total ? { label: reasonLabel(list[0].key), share: Math.round(list[0].count / total * 100) } : null
})
const firstName = computed(() => session.value?.user.name.split(' ')[0] ?? '')
</script>

<template>
  <UDashboardPanel id="dashboard">
    <template #header>
      <UDashboardNavbar :title="t('admin.dashboard')">
        <template #right>
          <UButton :label="t('admin.processReturns')" icon="i-lucide-arrow-right" trailing to="/returns?status=SUBMITTED" />
        </template>
      </UDashboardNavbar>
    </template>

    <template #body>
      <div>
        <h1 class="text-2xl font-extrabold sm:text-3xl">
          {{ t('admin.hello', { name: firstName }) }}
        </h1>
        <p class="mt-1 text-muted">
          {{ t('admin.pendingSubtitle', { count: summary?.pending ?? 0 }) }}
        </p>
      </div>

      <div v-if="isPending" class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <USkeleton v-for="i in 4" :key="i" class="h-32 rounded-xl" />
      </div>
      <div v-else-if="summary" class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <KpiCard
          :label="t('admin.kpi.total')"
          :value="String(summary.totalReturns)"
          :hint="t('admin.kpi.totalHint', { resolved: summary.resolved, rejected: summary.rejected })"
          icon="i-lucide-undo-2"
        />
        <KpiCard
          :label="t('admin.kpi.retained')"
          :value="money(summary.retainedMinor)"
          :hint="t('admin.kpi.retainedHint')"
          icon="i-lucide-trending-up"
        />
        <KpiCard
          :label="t('admin.kpi.approvalTime')"
          :value="formatMinutes(summary.medianApprovalMinutes)"
          :hint="t('admin.kpi.approvalHint', { auto: summary.autoApproved })"
          icon="i-lucide-timer"
        />
        <KpiCard
          tone="accent"
          :label="t('admin.kpi.bonus')"
          :value="money(summary.bonusMinor)"
          :hint="t('admin.kpi.bonusHint', { amount: money(summary.retainedMinor) })"
          icon="i-lucide-gift"
        />
      </div>

      <div class="grid gap-4 xl:grid-cols-3">
        <UCard class="xl:col-span-2">
          <template #header>
            <div class="flex items-center justify-between">
              <h2 class="font-semibold">
                {{ t('admin.toReview') }}
              </h2>
              <UButton :label="t('admin.seeAll')" variant="link" to="/returns?status=SUBMITTED" />
            </div>
          </template>
          <p v-if="pending.length === 0" class="py-6 text-center text-sm text-muted">
            {{ t('admin.nothingToReview') }}
          </p>
          <ul v-else class="-my-2 divide-y divide-default">
            <li v-for="r in pending.slice(0, 6)" :key="r.id">
              <NuxtLink :to="`/returns/${r.id}`" class="flex items-center gap-3 py-2.5 hover:opacity-80">
                <ProductThumb :title="r.lines[0]?.title" size="sm" />
                <div class="min-w-0 flex-1">
                  <p class="truncate text-sm font-semibold">
                    {{ r.customerName }} · {{ r.lines.map(l => l.title).join(', ') }}
                  </p>
                  <p class="truncate text-xs text-muted">
                    {{ r.number }} · {{ reasonLabel(r.lines[0]?.reasonCode ?? '') }} · {{ formatRelative(r.createdAt, locale) }}
                  </p>
                </div>
                <span class="hidden text-sm sm:block">{{ t(`resolution.${r.resolutionType}`) }}</span>
                <MoneyText :amount-minor="r.totalMinor" :currency="r.currency" class="text-sm font-semibold" />
              </NuxtLink>
            </li>
          </ul>
        </UCard>

        <UCard>
          <template #header>
            <h2 class="font-semibold">
              {{ t('admin.reasonsTitle') }}
            </h2>
          </template>
          <BarList :items="reasonItems" />
          <p v-if="topReason" class="mt-4 rounded-lg bg-elevated px-3 py-2 text-xs text-muted">
            <UIcon name="i-lucide-lightbulb" class="me-1 inline size-3.5 text-primary" />
            {{ t('admin.insight', { reason: topReason.label, share: topReason.share }) }}
          </p>
        </UCard>
      </div>

      <div class="grid gap-4 xl:grid-cols-3">
        <UCard class="xl:col-span-2">
          <template #header>
            <h2 class="font-semibold">
              {{ t('admin.dailyChart') }}
            </h2>
          </template>
          <DailyChart v-if="summary" :days="summary.daily" />
        </UCard>
        <div class="grid gap-4">
          <UCard>
            <template #header>
              <h2 class="font-semibold">
                {{ t('admin.resolutionsTitle') }}
              </h2>
            </template>
            <BarList :items="resolutionItems" />
          </UCard>
          <UCard>
            <template #header>
              <h2 class="font-semibold">
                {{ t('admin.topProducts') }}
              </h2>
            </template>
            <BarList :items="productItems" />
          </UCard>
        </div>
      </div>
    </template>
  </UDashboardPanel>
</template>
