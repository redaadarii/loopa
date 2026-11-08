<script setup lang="ts">
const { t, locale } = useI18n()
const toast = useToast()
const { data: usage, isPending, subscribe } = useBilling()

const percent = computed(() => {
  const u = usage.value
  if (!u || u.quota < 0) return 0
  return Math.min(100, Math.round(u.used / Math.max(u.quota, 1) * 100))
})

async function choose(plan: string) {
  try {
    const result = await subscribe.mutateAsync(plan)
    if (result.confirmationUrl) {
      window.location.href = result.confirmationUrl
      return
    }
    toast.add({ title: t('admin.planChanged'), color: 'success', icon: 'i-lucide-check' })
  } catch (e) {
    toast.add({ title: apiErrorMessage(e), color: 'error' })
  }
}
</script>

<template>
  <UDashboardPanel id="billing">
    <template #header>
      <UDashboardNavbar :title="t('admin.billing')" />
    </template>
    <template #body>
      <div class="mx-auto w-full max-w-5xl space-y-6">
        <p class="text-muted">
          {{ t('admin.billingIntro') }}
        </p>
        <USkeleton v-if="isPending" class="h-64 rounded-xl" />
        <template v-else-if="usage">
          <UCard>
            <div class="flex flex-wrap items-end justify-between gap-2">
              <div>
                <p class="text-sm text-muted">
                  {{ t('admin.usage') }}
                </p>
                <p class="font-display text-3xl font-extrabold tabular-nums">
                  {{ usage.used }}
                  <span class="text-lg font-semibold text-muted">/ {{ usage.quota < 0 ? t('admin.unlimited') : usage.quota }}</span>
                </p>
              </div>
              <UBadge color="primary" variant="subtle" size="lg" :label="`${t('admin.currentPlan')} : ${usage.plan}`" />
            </div>
            <UProgress v-if="usage.quota >= 0" :model-value="percent" class="mt-4" :color="percent > 85 ? 'warning' : 'primary'" />
          </UCard>

          <div class="grid gap-4 md:grid-cols-3">
            <UCard
              v-for="p in usage.plans"
              :key="p.code"
              :class="p.code === usage.plan ? 'ring-2 ring-primary' : ''"
            >
              <p class="font-semibold">
                {{ p.name }}
              </p>
              <p class="mt-2 font-display text-3xl font-extrabold">
                {{ p.priceMinor === 0 ? t('admin.free') : formatMoney(p.priceMinor, p.currency, locale) }}
                <span v-if="p.priceMinor" class="text-base font-medium text-muted">{{ t('admin.perMonth') }}</span>
              </p>
              <p class="mt-1 text-sm text-muted">
                {{ p.quota < 0 ? t('admin.unlimited') : `${p.quota} ${t('admin.returns').toLowerCase()}` }}
              </p>
              <ul class="mt-4 space-y-2 text-sm">
                <li v-for="f in p.features" :key="f" class="flex gap-2">
                  <UIcon name="i-lucide-check" class="mt-0.5 size-4 shrink-0 text-primary" />{{ f }}
                </li>
              </ul>
              <UButton
                class="mt-6"
                block
                :label="p.code === usage.plan ? t('admin.currentPlan') : t('admin.choose')"
                :disabled="p.code === usage.plan"
                :variant="p.code === usage.plan ? 'soft' : 'solid'"
                :loading="subscribe.isLoading.value"
                @click="choose(p.code)"
              />
            </UCard>
          </div>
        </template>
      </div>
    </template>
  </UDashboardPanel>
</template>
