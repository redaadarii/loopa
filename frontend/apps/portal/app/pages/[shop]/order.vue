<script setup lang="ts">
import type { PortalOrderLine, ResolutionType } from '../../../../../layers/api/app/types/api'

// Assistant de retour : un écran = une décision (articles → motif → solution → récapitulatif).
const { t, locale } = useI18n()
const route = useRoute()
const toast = useToast()
const portal = usePortalApi()
const shopHandle = String(route.params.shop)

if (!portal.hasSession.value) {
  await navigateTo(`/${shopHandle}`, { replace: true })
}

const { data: order, isPending, error } = usePortalOrder()
const { mutateAsync: submitReturn, isLoading: submitting } = useSubmitReturn()

watch(error, (e) => {
  if (e && (e as { statusCode?: number }).statusCode === 401) {
    portal.clearSession()
    toast.add({ title: t('portal.sessionExpired'), color: 'warning' })
    navigateTo(`/${shopHandle}`)
  }
})

type Selection = { quantity: number, reasonCode: string | undefined, exchangePreference: string }
const step = ref(0)
const selection = reactive<Record<string, Selection>>({})
const photos = ref<Array<{ id: string, name: string, preview: string }>>([])
const uploading = ref(false)
const resolution = ref<ResolutionType | undefined>(undefined)
const note = ref('')

const money = (minor: number) => formatMoney(minor, order.value?.currency ?? 'MAD', locale.value)
const reasonLabel = (code: string) => {
  const r = order.value?.reasons.find(x => x.code === code)
  if (!r) return code
  return locale.value === 'en' ? r.labelEn : locale.value === 'ar' ? r.labelAr : r.labelFr
}

const selectedLines = computed(() => (order.value?.lines ?? []).filter(l => selection[l.orderLineId]))
const total = computed(() => selectedLines.value.reduce((s, l) => s + l.unitPriceMinor * (selection[l.orderLineId]?.quantity ?? 1), 0))
const photoRequired = computed(() => selectedLines.value.some(l =>
  order.value?.reasons.find(r => r.code === selection[l.orderLineId]?.reasonCode)?.requiresPhoto))
const credit = computed(() => total.value + (resolution.value === 'STORE_CREDIT' ? order.value?.storeCreditBonusMinor ?? 0 : 0))
const eligibleCount = computed(() => order.value?.lines.filter(l => l.eligible).length ?? 0)

watch(order, (o) => {
  if (o && !resolution.value) resolution.value = o.resolutions[0]
}, { immediate: true })

const steps = computed(() => [
  { title: t('portal.steps.items'), icon: 'i-lucide-package' },
  { title: t('portal.steps.reason'), icon: 'i-lucide-message-circle' },
  { title: t('portal.steps.resolution'), icon: 'i-lucide-repeat' },
  { title: t('portal.steps.review'), icon: 'i-lucide-check' }
])

const canContinue = computed(() => {
  if (step.value === 0) return selectedLines.value.length > 0
  if (step.value === 1) {
    return selectedLines.value.every(l => selection[l.orderLineId]?.reasonCode)
      && (!photoRequired.value || photos.value.length > 0) && !uploading.value
  }
  if (step.value === 2) return !!resolution.value
  return true
})

function toggle(line: PortalOrderLine) {
  if (!line.eligible) return
  if (selection[line.orderLineId]) {
    delete selection[line.orderLineId]
  } else {
    selection[line.orderLineId] = { quantity: 1, reasonCode: undefined, exchangePreference: '' }
  }
}

async function onFiles(files: File[] | File | null | undefined) {
  const list = Array.isArray(files) ? files : files ? [files] : []
  for (const file of list) {
    if (photos.value.some(p => p.name === file.name)) continue
    uploading.value = true
    try {
      const uploaded = await portal.upload(file)
      photos.value.push({ id: uploaded.id, name: file.name, preview: URL.createObjectURL(file) })
    } catch (e) {
      toast.add({ title: apiErrorMessage(e), color: 'error' })
    } finally {
      uploading.value = false
    }
  }
}

function removePhoto(id: string) {
  photos.value = photos.value.filter(p => p.id !== id)
}

async function submit() {
  if (!resolution.value) return
  try {
    const created = await submitReturn({
      resolutionType: resolution.value,
      lines: selectedLines.value.map(l => ({
        orderLineId: l.orderLineId,
        quantity: selection[l.orderLineId]!.quantity,
        reasonCode: selection[l.orderLineId]!.reasonCode!,
        exchangePreference: selection[l.orderLineId]!.exchangePreference || null
      })),
      attachmentIds: photos.value.map(p => p.id),
      customerNote: note.value || null,
      locale: locale.value
    })
    await navigateTo(`/${shopHandle}/returns/${created.id}`)
  } catch (e) {
    toast.add({ title: apiErrorMessage(e), color: 'error', icon: 'i-lucide-circle-alert' })
  }
}

const resolutionOptions = computed(() => (order.value?.resolutions ?? []).map(r => ({
  value: r,
  label: t(`resolution.${r}`),
  description: r === 'STORE_CREDIT'
    ? t('portal.storeCreditDesc', { amount: money(total.value + (order.value?.storeCreditBonusMinor ?? 0)) })
    : r === 'EXCHANGE' ? t('portal.exchangeDesc') : t('portal.refundDesc')
})))
</script>

<template>
  <section class="space-y-6">
    <USkeleton v-if="isPending" class="h-72 rounded-xl" />
    <template v-else-if="order">
      <div class="space-y-1">
        <p class="text-sm text-muted">
          {{ t('portal.orderOf', { name: order.orderName, date: formatDate(order.orderedAt, locale) }) }}
        </p>
        <p class="text-xs text-muted">
          {{ t('portal.returnableUntil', { date: formatDate(order.eligibleUntil, locale) }) }}
        </p>
      </div>

      <div v-if="order.existingReturns.length" class="rounded-xl border border-default bg-white p-4">
        <p class="mb-2 text-sm font-semibold">
          {{ t('portal.existing') }}
        </p>
        <NuxtLink
          v-for="r in order.existingReturns"
          :key="r.id"
          :to="`/${shopHandle}/returns/${r.id}`"
          class="flex items-center justify-between py-1.5 text-sm hover:underline"
        >
          <span>{{ r.number }}</span>
          <StatusBadge :status="r.status" />
        </NuxtLink>
      </div>

      <UAlert v-if="eligibleCount === 0" color="neutral" variant="subtle" icon="i-lucide-info" :title="t('portal.noEligible')" />

      <template v-else>
        <div class="space-y-2">
          <div class="grid grid-cols-4 gap-1.5" aria-hidden="true">
            <div v-for="(s, i) in steps" :key="s.title" class="h-1.5 rounded-full" :class="i <= step ? 'bg-primary' : 'bg-accented'" />
          </div>
          <p class="text-xs font-semibold text-muted">
            {{ t('portal.stepOf', { n: step + 1 }) }} · {{ steps[step]?.title }}
          </p>
        </div>

        <!-- 1. Articles -->
        <div v-if="step === 0" class="space-y-3">
          <h1 class="text-2xl font-extrabold">
            {{ t('portal.chooseItems') }}
          </h1>
          <button
            v-for="line in order.lines"
            :key="line.orderLineId"
            type="button"
            :disabled="!line.eligible"
            :aria-pressed="!!selection[line.orderLineId]"
            class="flex w-full items-center gap-4 rounded-xl border bg-white p-4 text-start transition disabled:opacity-55"
            :class="selection[line.orderLineId] ? 'border-primary ring-2 ring-primary/20' : 'border-default hover:border-accented'"
            @click="toggle(line)"
          >
            <ProductThumb :image-url="line.imageUrl" :product-type="line.productType" :title="line.title" />
            <div class="min-w-0 flex-1">
              <p class="font-semibold">
                {{ line.title }}
              </p>
              <p class="text-sm text-muted">
                {{ line.variantTitle }} · {{ money(line.unitPriceMinor) }}
              </p>
              <p v-if="!line.eligible" class="mt-1 text-xs font-medium text-muted">
                {{ t(`ineligible.${line.ineligibleReason}`) }}
              </p>
            </div>
            <UCheckbox v-if="line.eligible" :model-value="!!selection[line.orderLineId]" tabindex="-1" class="pointer-events-none" />
          </button>
        </div>

        <!-- 2. Motifs + photos -->
        <div v-else-if="step === 1" class="space-y-6">
          <div v-for="line in selectedLines" :key="line.orderLineId" class="space-y-3">
            <div class="flex items-center gap-3">
              <ProductThumb :product-type="line.productType" :title="line.title" size="sm" />
              <div>
                <h2 class="text-lg font-bold leading-tight">
                  {{ t('portal.chooseReason') }}
                </h2>
                <p class="text-sm text-muted">
                  {{ line.title }} · {{ line.variantTitle }}
                </p>
              </div>
            </div>
            <URadioGroup
              v-model="selection[line.orderLineId]!.reasonCode"
              variant="card"
              :items="order.reasons.map(r => ({ label: reasonLabel(r.code), value: r.code }))"
              :ui="{ item: 'bg-white' }"
            />
            <UFormField v-if="line.returnableQuantity > 1" :label="t('portal.quantity')">
              <UInputNumber v-model="selection[line.orderLineId]!.quantity" :min="1" :max="line.returnableQuantity" />
            </UFormField>
          </div>

          <div v-if="photoRequired" class="space-y-3 rounded-xl border border-default bg-white p-4">
            <p class="text-sm font-semibold">
              <UIcon name="i-lucide-camera" class="me-1 inline size-4 text-primary" />{{ t('portal.photoRequired') }}
            </p>
            <div class="flex flex-wrap gap-3">
              <div v-for="p in photos" :key="p.id" class="relative">
                <img :src="p.preview" alt="" class="size-24 rounded-lg object-cover ring-1 ring-default">
                <UButton
                  icon="i-lucide-x"
                  size="xs"
                  color="neutral"
                  class="absolute -end-2 -top-2 rounded-full"
                  :aria-label="t('common.close')"
                  @click="removePhoto(p.id)"
                />
              </div>
              <UFileUpload
                accept="image/*"
                multiple
                :label="uploading ? t('portal.uploading') : t('portal.addPhoto')"
                icon="i-lucide-image-plus"
                :preview="false"
                class="size-24"
                :disabled="uploading"
                @update:model-value="onFiles"
              />
            </div>
          </div>
        </div>

        <!-- 3. Solution : l'avoir avec bonus est proposé en premier, le remboursement reste visible -->
        <div v-else-if="step === 2" class="space-y-3">
          <h1 class="text-2xl font-extrabold">
            {{ t('portal.chooseResolution') }}
          </h1>
          <URadioGroup v-model="resolution" variant="card" :items="resolutionOptions" :ui="{ item: 'bg-white', fieldset: 'gap-3' }">
            <template #label="{ item }">
              <span class="flex flex-wrap items-center gap-2 font-bold">
                {{ item.label }}
                <span
                  v-if="item.value === 'STORE_CREDIT' && order.storeCreditBonusMinor"
                  class="rounded-full bg-secondary px-2 py-0.5 text-xs font-bold text-ink"
                >{{ t('portal.bonus', { amount: money(order.storeCreditBonusMinor) }) }}</span>
              </span>
            </template>
            <template #description="{ item }">
              <span class="block">{{ item.description }}</span>
              <span v-if="item.value === 'STORE_CREDIT'" class="mt-1 block text-xs font-semibold text-primary">
                {{ t('portal.recommended') }}
              </span>
            </template>
          </URadioGroup>
          <div v-if="resolution === 'EXCHANGE'" class="space-y-3 rounded-xl border border-default bg-white p-4">
            <UFormField
              v-for="line in selectedLines"
              :key="line.orderLineId"
              :label="`${t('portal.exchangePreference')} · ${line.title}`"
            >
              <UInput v-model="selection[line.orderLineId]!.exchangePreference" :placeholder="t('portal.exchangePlaceholder')" class="w-full" />
            </UFormField>
          </div>
        </div>

        <!-- 4. Récapitulatif -->
        <div v-else class="space-y-4">
          <h1 class="text-2xl font-extrabold">
            {{ t('portal.reviewTitle') }}
          </h1>
          <UCard>
            <ul class="-my-2 divide-y divide-default">
              <li v-for="line in selectedLines" :key="line.orderLineId" class="flex items-center gap-3 py-3 text-sm">
                <ProductThumb :product-type="line.productType" :title="line.title" size="sm" />
                <div class="flex-1">
                  <p class="font-semibold">
                    {{ line.title }} × {{ selection[line.orderLineId]?.quantity }}
                  </p>
                  <p class="text-muted">
                    {{ reasonLabel(selection[line.orderLineId]?.reasonCode ?? '') }}
                  </p>
                </div>
                <MoneyText :amount-minor="line.unitPriceMinor * (selection[line.orderLineId]?.quantity ?? 1)" :currency="order.currency" />
              </li>
            </ul>
            <template #footer>
              <div class="flex items-center justify-between">
                <div>
                  <p class="text-xs text-muted">
                    {{ t('portal.youGet') }}
                  </p>
                  <p class="font-semibold">
                    {{ resolution ? t(`resolution.${resolution}`) : '' }}
                  </p>
                </div>
                <MoneyText :amount-minor="credit" :currency="order.currency" class="font-display text-2xl font-extrabold text-primary" />
              </div>
            </template>
          </UCard>
          <UFormField :label="t('portal.note')">
            <UTextarea v-model="note" :rows="3" :maxlength="1000" class="w-full" />
          </UFormField>
        </div>

        <!-- Barre d'action fixe en bas : le pouce atteint toujours le bouton principal -->
        <div class="fixed inset-x-0 bottom-0 z-10 border-t border-default bg-white/95 px-4 pb-[max(1rem,env(safe-area-inset-bottom))] pt-4 backdrop-blur">
          <div class="mx-auto flex max-w-xl gap-3">
            <UButton
              v-if="step > 0"
              :label="t('common.back')"
              color="neutral"
              variant="outline"
              size="xl"
              @click="step--"
            />
            <UButton
              v-if="step < 3"
              :label="t('common.next')"
              size="xl"
              block
              :disabled="!canContinue"
              @click="step++"
            />
            <UButton v-else :label="t('portal.submit')" size="xl" block :loading="submitting" @click="submit" />
          </div>
        </div>
      </template>
    </template>
  </section>
</template>
