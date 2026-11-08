<script setup lang="ts">
import type { ReturnAction } from '../../../../../layers/api/app/composables/useAdmin'

const route = useRoute()
const { t, locale } = useI18n()
const toast = useToast()

const id = computed(() => String(route.params.id))
const { data: ret, isPending, error } = useAdminReturn(id)
const { mutateAsync, isLoading } = useReturnAction()
const { mutateAsync: addNote, isLoading: addingNote } = useAddNote()
const { data: reasons } = useReasons()

const rejectOpen = ref(false)
const rejectReason = ref('')
const note = ref('')

const reasonLabel = (code: string) => {
  const r = reasons.value?.find(x => x.code === code)
  if (!r) return code
  return locale.value === 'en' ? r.labelEn : locale.value === 'ar' ? r.labelAr : r.labelFr
}
const history = computed(() => [...(ret.value?.history ?? [])].reverse())

async function run(action: ReturnAction) {
  try {
    await mutateAsync({ id: id.value, action, reason: rejectReason.value })
    rejectOpen.value = false
    toast.add({ title: t(`admin.actionDone.${action}`), color: 'success', icon: 'i-lucide-check' })
  } catch (e) {
    toast.add({ title: apiErrorMessage(e), color: 'error', icon: 'i-lucide-circle-alert' })
  }
}

async function submitNote() {
  if (!note.value.trim()) return
  try {
    await addNote({ id: id.value, note: note.value.trim() })
    note.value = ''
    toast.add({ title: t('admin.noteAdded'), color: 'success' })
  } catch (e) {
    toast.add({ title: apiErrorMessage(e), color: 'error' })
  }
}

defineShortcuts({
  a: () => { if (ret.value?.status === 'SUBMITTED') run('approve') },
  r: () => { if (ret.value?.status === 'SUBMITTED') rejectOpen.value = true }
})
</script>

<template>
  <UDashboardPanel id="return-detail">
    <template #header>
      <UDashboardNavbar :title="ret?.number ?? '…'">
        <template #leading>
          <UButton icon="i-lucide-arrow-left" color="neutral" variant="ghost" to="/returns" :aria-label="t('common.back')" />
        </template>
        <template #trailing>
          <StatusBadge v-if="ret" :status="ret.status" />
        </template>
        <template #right>
          <template v-if="ret?.status === 'SUBMITTED'">
            <UModal v-model:open="rejectOpen" :title="t('admin.reject')">
              <UButton :label="t('admin.reject')" color="error" variant="soft" icon="i-lucide-x" />
              <template #body>
                <UFormField :label="t('admin.rejectReason')" required>
                  <UTextarea v-model="rejectReason" :placeholder="t('admin.rejectPlaceholder')" class="w-full" autofocus />
                </UFormField>
              </template>
              <template #footer>
                <UButton :label="t('common.cancel')" color="neutral" variant="ghost" @click="rejectOpen = false" />
                <UButton
                  :label="t('admin.reject')"
                  color="error"
                  :loading="isLoading"
                  :disabled="!rejectReason.trim()"
                  @click="run('reject')"
                />
              </template>
            </UModal>
            <UButton :label="t('admin.approve')" icon="i-lucide-check" :loading="isLoading" @click="run('approve')" />
          </template>
          <UButton
            v-else-if="ret?.status === 'APPROVED' || ret?.status === 'IN_TRANSIT'"
            :label="t('admin.receive')"
            icon="i-lucide-package-check"
            :loading="isLoading"
            @click="run('receive')"
          />
          <UButton
            v-else-if="ret?.status === 'RECEIVED' && ret.resolutionError"
            :label="t('admin.resolve')"
            icon="i-lucide-rotate-cw"
            :loading="isLoading"
            @click="run('resolve')"
          />
        </template>
      </UDashboardNavbar>
    </template>

    <template #body>
      <UAlert v-if="error" color="error" variant="subtle" :title="apiErrorMessage(error)" />
      <USkeleton v-if="isPending" class="h-80 rounded-xl" />
      <div v-else-if="ret" class="grid gap-4 xl:grid-cols-3">
        <div class="space-y-4 xl:col-span-2">
          <UAlert
            v-if="ret.resolutionError"
            color="error"
            variant="subtle"
            icon="i-lucide-triangle-alert"
            :title="t('admin.resolutionError')"
            :description="ret.resolutionError"
          />

          <UCard>
            <template #header>
              <h2 class="font-semibold">
                {{ t('admin.items') }}
              </h2>
            </template>
            <ul class="-my-3 divide-y divide-default">
              <li v-for="line in ret.lines" :key="line.id" class="flex items-center gap-4 py-3">
                <ProductThumb :title="line.title" />
                <div class="min-w-0 flex-1">
                  <p class="font-semibold">
                    {{ line.title }} <span class="font-normal text-muted">· {{ line.variantTitle }} · ×{{ line.quantity }}</span>
                  </p>
                  <div class="mt-1 flex flex-wrap items-center gap-2">
                    <UBadge color="neutral" variant="subtle" :label="reasonLabel(line.reasonCode)" />
                    <UBadge
                      v-if="line.exchangeVariantId"
                      color="info"
                      variant="subtle"
                      icon="i-lucide-repeat"
                      :label="t('admin.exchangeFor', { value: line.exchangeVariantId })"
                    />
                  </div>
                </div>
                <MoneyText :amount-minor="line.unitPriceMinor * line.quantity" :currency="ret.currency" class="font-semibold" />
              </li>
            </ul>
          </UCard>

          <UCard>
            <template #header>
              <h2 class="font-semibold">
                {{ t('admin.photos') }}
              </h2>
            </template>
            <div v-if="ret.attachmentIds.length" class="flex flex-wrap gap-3">
              <AuthedImage v-for="fileId in ret.attachmentIds" :key="fileId" :file-id="fileId" />
            </div>
            <p v-else class="text-sm text-muted">
              {{ t('admin.noPhotos') }}
            </p>
            <blockquote v-if="ret.customerNote" class="mt-4 rounded-lg bg-elevated px-4 py-3 text-sm">
              <p class="mb-1 text-xs font-semibold text-muted">
                {{ t('admin.customerNote') }}
              </p>
              « {{ ret.customerNote }} »
            </blockquote>
          </UCard>

          <UCard>
            <template #header>
              <h2 class="font-semibold">
                {{ t('admin.history') }}
              </h2>
            </template>
            <form class="mb-6 flex gap-2" @submit.prevent="submitNote">
              <UInput v-model="note" :placeholder="t('admin.addNote')" class="flex-1" :aria-label="t('admin.addNote')" />
              <UButton type="submit" icon="i-lucide-send" color="neutral" variant="outline" :loading="addingNote" :aria-label="t('common.add')" />
            </form>
            <ReturnTimeline :entries="history" />
          </UCard>
        </div>

        <div class="space-y-4">
          <UCard>
            <template #header>
              <h2 class="font-semibold">
                {{ t('admin.resolutionCard') }}
              </h2>
            </template>
            <dl class="space-y-2 text-sm">
              <div class="flex justify-between">
                <dt>{{ t(`resolution.${ret.resolutionType}`) }}</dt>
                <dd />
              </div>
              <div class="flex justify-between text-muted">
                <dt>{{ t('admin.itemsValue') }}</dt>
                <dd><MoneyText :amount-minor="ret.totalMinor" :currency="ret.currency" /></dd>
              </div>
              <div v-if="ret.bonusMinor" class="flex justify-between text-coral-800">
                <dt>{{ t('admin.bonus') }}</dt>
                <dd>+<MoneyText :amount-minor="ret.bonusMinor" :currency="ret.currency" /></dd>
              </div>
              <div class="flex justify-between border-t border-default pt-2 text-base font-bold">
                <dt>{{ t('admin.credited') }}</dt>
                <dd><MoneyText :amount-minor="ret.creditMinor" :currency="ret.currency" /></dd>
              </div>
              <div v-if="ret.resolutionRef" class="flex justify-between pt-1 text-xs text-muted">
                <dt>{{ t('admin.resolutionRef') }}</dt>
                <dd class="font-mono">
                  {{ ret.resolutionRef }}
                </dd>
              </div>
            </dl>
          </UCard>

          <UCard>
            <dl class="space-y-3 text-sm">
              <div>
                <dt class="text-muted">
                  {{ t('admin.customer') }}
                </dt>
                <dd class="font-semibold">
                  {{ ret.customerName }}
                </dd>
                <dd class="text-muted">
                  {{ ret.customerEmail }}
                </dd>
              </div>
              <div>
                <dt class="text-muted">
                  {{ t('admin.order') }}
                </dt>
                <dd class="font-semibold">
                  {{ ret.orderName }}
                </dd>
              </div>
              <div>
                <dt class="text-muted">
                  {{ t('admin.createdAt') }}
                </dt>
                <dd class="font-semibold">
                  {{ formatDate(ret.createdAt, locale, true) }}
                </dd>
              </div>
              <div v-if="ret.trackingNumber">
                <dt class="text-muted">
                  {{ t('admin.tracking') }}
                </dt>
                <dd class="font-semibold">
                  {{ ret.carrier }} · <span class="font-mono">{{ ret.trackingNumber }}</span>
                </dd>
              </div>
              <div v-if="ret.rejectionReason">
                <dt class="text-muted">
                  {{ t('admin.rejectReason') }}
                </dt>
                <dd>{{ ret.rejectionReason }}</dd>
              </div>
            </dl>
          </UCard>
          <p v-if="ret.status === 'SUBMITTED'" class="text-center text-xs text-muted">
            <UIcon name="i-lucide-keyboard" class="me-1 inline size-3.5" />{{ t('admin.shortcuts') }}
          </p>
        </div>
      </div>
    </template>
  </UDashboardPanel>
</template>
