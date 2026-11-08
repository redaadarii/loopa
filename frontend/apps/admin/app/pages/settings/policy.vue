<script setup lang="ts">
import type { Policy, Reason } from '../../../../../layers/api/app/types/api'

const { t } = useI18n()
const toast = useToast()
const { isAdmin } = useAuth()
const { data: policy, save: savePolicy } = usePolicy()
const { data: reasons, save: saveReasons } = useReasons()

const form = ref<Policy | null>(null)
const excluded = ref('')
const reasonRows = ref<Reason[]>([])
watch(policy, (p) => {
  if (p) {
    form.value = { ...p }
    excluded.value = p.excludedProductTypes.join(', ')
  }
}, { immediate: true })
watch(reasons, (r) => { if (r) reasonRows.value = r.map(x => ({ ...x })) }, { immediate: true })

/** Les montants sont saisis en MAD, stockés en centimes. */
const maxAmount = computed({
  get: () => (form.value?.autoApproveMaxAmountMinor ?? 0) / 100,
  set: (v: number) => { if (form.value) form.value.autoApproveMaxAmountMinor = Math.round((v ?? 0) * 100) }
})
const bonus = computed({
  get: () => (form.value?.storeCreditBonusMinor ?? 0) / 100,
  set: (v: number) => { if (form.value) form.value.storeCreditBonusMinor = Math.round((v ?? 0) * 100) }
})

async function submitPolicy() {
  if (!form.value) return
  try {
    await savePolicy.mutateAsync({
      ...form.value,
      excludedProductTypes: excluded.value.split(',').map(s => s.trim()).filter(Boolean)
    })
    toast.add({ title: t('common.saved'), color: 'success', icon: 'i-lucide-check' })
  } catch (e) {
    toast.add({ title: apiErrorMessage(e), color: 'error' })
  }
}

function addReason() {
  reasonRows.value.push({ code: '', labelFr: '', labelEn: '', labelAr: '', requiresPhoto: false, active: true })
}

async function submitReasons() {
  try {
    await saveReasons.mutateAsync(reasonRows.value
      .filter(r => r.code.trim())
      .map(r => ({ ...r, code: r.code.trim().toUpperCase().replace(/[^A-Z0-9_]/g, '_') })))
    toast.add({ title: t('common.saved'), color: 'success', icon: 'i-lucide-check' })
  } catch (e) {
    toast.add({ title: apiErrorMessage(e), color: 'error' })
  }
}
</script>

<template>
  <UDashboardPanel id="policy">
    <template #header>
      <UDashboardNavbar :title="t('admin.policy')" />
    </template>
    <template #body>
      <div class="mx-auto w-full max-w-4xl space-y-6">
        <p class="text-muted">
          {{ t('admin.policyIntro') }}
        </p>
        <UAlert v-if="!isAdmin" color="neutral" variant="subtle" icon="i-lucide-lock" :title="t('admin.readOnly')" />

        <UCard v-if="form">
          <form class="space-y-6" @submit.prevent="submitPolicy">
            <fieldset :disabled="!isAdmin" class="grid gap-6 sm:grid-cols-2">
              <UFormField :label="t('admin.windowDays')" :help="t('admin.windowHint')">
                <UInputNumber v-model="form.windowDays" :min="1" :max="365" class="w-full" />
              </UFormField>
              <UFormField :label="t('admin.bonusLabel')" :help="t('admin.bonusHint')">
                <UInputNumber v-model="bonus" :min="0" :step="10" class="w-full" />
              </UFormField>
              <div class="sm:col-span-2">
                <USwitch v-model="form.autoApprove" :label="t('admin.autoApprove')" :description="t('admin.autoApproveHint')" />
              </div>
              <UFormField v-if="form.autoApprove" :label="t('admin.autoApproveMax')">
                <UInputNumber v-model="maxAmount" :min="0" :step="50" class="w-full" />
              </UFormField>
              <div class="space-y-3 sm:col-span-2">
                <p class="text-sm font-medium">
                  {{ t('admin.resolutionsAllowed') }}
                </p>
                <div class="flex flex-wrap gap-6">
                  <UCheckbox v-model="form.allowStoreCredit" :label="t('resolution.STORE_CREDIT')" />
                  <UCheckbox v-model="form.allowExchange" :label="t('resolution.EXCHANGE')" />
                  <UCheckbox v-model="form.allowRefund" :label="t('resolution.REFUND')" />
                </div>
              </div>
              <UFormField :label="t('admin.excluded')" :help="t('admin.excludedHint')" class="sm:col-span-2">
                <UInput v-model="excluded" class="w-full" />
              </UFormField>
            </fieldset>
            <div v-if="isAdmin" class="flex justify-end">
              <UButton type="submit" :label="t('common.save')" :loading="savePolicy.isLoading.value" />
            </div>
          </form>
        </UCard>

        <UCard>
          <template #header>
            <div>
              <h2 class="font-semibold">
                {{ t('admin.reasonsSection') }}
              </h2>
              <p class="text-sm text-muted">
                {{ t('admin.reasonsHint') }}
              </p>
            </div>
          </template>
          <div class="overflow-x-auto">
            <table class="w-full min-w-[720px] text-sm">
              <thead>
                <tr class="text-start text-xs text-muted">
                  <th class="pb-2 text-start font-medium">
                    {{ t('admin.code') }}
                  </th>
                  <th class="pb-2 text-start font-medium">
                    {{ t('admin.labelFr') }}
                  </th>
                  <th class="pb-2 text-start font-medium">
                    {{ t('admin.labelEn') }}
                  </th>
                  <th class="pb-2 text-start font-medium">
                    {{ t('admin.labelAr') }}
                  </th>
                  <th class="pb-2 font-medium">
                    {{ t('admin.photo') }}
                  </th>
                  <th class="pb-2 font-medium">
                    {{ t('admin.active') }}
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(r, i) in reasonRows" :key="i" class="border-t border-default">
                  <td class="py-2 pe-2">
                    <UInput v-model="r.code" size="sm" :disabled="!isAdmin || !!r.position || r.position === 0" class="w-40 font-mono" />
                  </td>
                  <td class="py-2 pe-2">
                    <UInput v-model="r.labelFr" size="sm" :disabled="!isAdmin" class="w-full" />
                  </td>
                  <td class="py-2 pe-2">
                    <UInput v-model="r.labelEn" size="sm" :disabled="!isAdmin" class="w-full" />
                  </td>
                  <td class="py-2 pe-2">
                    <UInput v-model="r.labelAr" size="sm" dir="rtl" :disabled="!isAdmin" class="w-full" />
                  </td>
                  <td class="py-2 text-center">
                    <UCheckbox v-model="r.requiresPhoto" :disabled="!isAdmin" class="inline-flex" :aria-label="t('admin.photo')" />
                  </td>
                  <td class="py-2 text-center">
                    <USwitch v-model="r.active" :disabled="!isAdmin" class="inline-flex" :aria-label="t('admin.active')" />
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <template v-if="isAdmin" #footer>
            <div class="flex justify-between">
              <UButton :label="t('admin.addReason')" icon="i-lucide-plus" color="neutral" variant="outline" @click="addReason" />
              <UButton :label="t('common.save')" :loading="saveReasons.isLoading.value" @click="submitReasons" />
            </div>
          </template>
        </UCard>
      </div>
    </template>
  </UDashboardPanel>
</template>
