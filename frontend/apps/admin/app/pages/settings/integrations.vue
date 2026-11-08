<script setup lang="ts">
const { t, locale } = useI18n()
const toast = useToast()
const config = useRuntimeConfig()
const { data: status, isPending, sync } = useIntegrations()
const shopDomain = ref('')

async function runSync() {
  try {
    const result = await sync.mutateAsync()
    toast.add({ title: `${result.imported} ${t('admin.ordersSynced').toLowerCase()}`, color: 'success' })
  } catch (e) {
    toast.add({ title: apiErrorMessage(e), color: 'error' })
  }
}

function install() {
  const domain = shopDomain.value.trim().toLowerCase()
  const shop = domain.endsWith('.myshopify.com') ? domain : `${domain}.myshopify.com`
  window.location.href = `${config.public.apiBase}/auth/shopify/install?shop=${encodeURIComponent(shop)}`
}
</script>

<template>
  <UDashboardPanel id="integrations">
    <template #header>
      <UDashboardNavbar :title="t('admin.integrations')" />
    </template>
    <template #body>
      <div class="mx-auto w-full max-w-3xl space-y-6">
        <p class="text-muted">
          {{ t('admin.integrationsIntro') }}
        </p>
        <USkeleton v-if="isPending" class="h-48 rounded-xl" />
        <UCard v-else-if="status">
          <div class="flex items-start gap-4">
            <div class="grid size-12 shrink-0 place-items-center rounded-xl bg-[#95BF47]/15 text-[#5E8E3E]">
              <UIcon name="i-lucide-shopping-bag" class="size-6" />
            </div>
            <div class="flex-1">
              <div class="flex flex-wrap items-center gap-2">
                <h2 class="text-lg font-semibold">
                  Shopify
                </h2>
                <UBadge
                  :color="status.connected ? 'success' : 'neutral'"
                  variant="subtle"
                  :label="status.connected ? t('admin.shopifyConnected') : t('admin.shopifyNotConnected')"
                />
              </div>
              <p v-if="status.shopDomain" class="font-mono text-sm text-muted">
                {{ status.shopDomain }}
              </p>
              <dl class="mt-4 grid grid-cols-2 gap-4 text-sm">
                <div>
                  <dt class="text-muted">
                    {{ t('admin.ordersSynced') }}
                  </dt>
                  <dd class="text-xl font-bold tabular-nums">
                    {{ status.ordersSynced }}
                  </dd>
                </div>
                <div>
                  <dt class="text-muted">
                    {{ t('admin.lastSync') }}
                  </dt>
                  <dd class="font-semibold">
                    {{ status.lastSyncAt ? formatRelative(status.lastSyncAt, locale) : '—' }}
                  </dd>
                </div>
              </dl>
            </div>
          </div>
          <template #footer>
            <UButton
              v-if="status.connected"
              :label="t('admin.syncNow')"
              icon="i-lucide-refresh-cw"
              :loading="sync.isLoading.value"
              @click="runSync"
            />
            <form v-else class="space-y-3" @submit.prevent="install">
              <p class="text-sm font-medium">
                {{ t('admin.installShopify') }}
              </p>
              <div class="flex flex-wrap gap-2">
                <UInput v-model="shopDomain" placeholder="ma-boutique.myshopify.com" class="min-w-64 flex-1 font-mono" />
                <UButton type="submit" :label="t('admin.installShopify')" icon="i-lucide-plug" :disabled="!shopDomain.trim()" />
              </div>
              <p class="text-xs text-muted">
                {{ t('admin.installHint') }}
              </p>
            </form>
          </template>
        </UCard>
      </div>
    </template>
  </UDashboardPanel>
</template>
