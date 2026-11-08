<script setup lang="ts">
import * as z from 'zod'
import type { FormSubmitEvent } from '@nuxt/ui'

const { t, locale } = useI18n()
const route = useRoute()
const shop = usePortalShop()
const flow = usePortalFlow()
const portal = usePortalApi()

const schema = z.object({
  orderNumber: z.string().trim().min(2),
  contact: z.string().trim().min(5)
})
type Schema = z.output<typeof schema>

const state = reactive<Partial<Schema>>({ orderNumber: '', contact: '' })
const loading = ref(false)
const error = ref<string | null>(null)

async function onSubmit(event: FormSubmitEvent<Schema>) {
  loading.value = true
  error.value = null
  try {
    const started = await portal.startSession({
      shop: String(route.params.shop),
      orderNumber: event.data.orderNumber,
      contact: event.data.contact,
      locale: locale.value
    })
    flow.value = { sessionId: started.sessionId, maskedContact: started.maskedContact, devCode: started.devCode }
    await navigateTo(`/${route.params.shop}/verify`)
  } catch (e) {
    error.value = apiErrorMessage(e)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="space-y-6">
    <div class="space-y-3">
      <span class="inline-flex rounded-full bg-primary/10 px-3 py-1 text-xs font-semibold text-primary">
        {{ t('portal.windowInfo', { days: shop?.windowDays ?? 30 }) }}
      </span>
      <h1 class="text-3xl font-extrabold leading-tight sm:text-4xl">
        {{ t('portal.title') }}
      </h1>
      <p class="text-muted">
        {{ shop?.welcomeMessage || t('portal.subtitle') }}
      </p>
    </div>

    <UCard :ui="{ body: 'p-5 sm:p-6' }">
      <UAlert v-if="error" color="error" variant="subtle" icon="i-lucide-circle-alert" :title="error" class="mb-4" />
      <UForm :schema="schema" :state="state" class="space-y-4" @submit="onSubmit">
        <UFormField :label="t('portal.orderNumber')" name="orderNumber">
          <UInput
            v-model="state.orderNumber"
            :placeholder="t('portal.orderPlaceholder')"
            size="xl"
            inputmode="text"
            autocomplete="off"
            class="w-full"
          />
        </UFormField>
        <UFormField :label="t('portal.contact')" name="contact">
          <UInput v-model="state.contact" :placeholder="t('portal.contactPlaceholder')" size="xl" class="w-full" />
        </UFormField>
        <UButton type="submit" :label="t('portal.find')" size="xl" block :loading="loading" />
      </UForm>
    </UCard>

    <ul class="grid grid-cols-3 gap-3 text-center text-xs text-muted">
      <li class="flex flex-col items-center gap-1.5">
        <UIcon name="i-lucide-timer" class="size-5 text-primary" />{{ t('portal.perks.fast') }}
      </li>
      <li class="flex flex-col items-center gap-1.5">
        <UIcon name="i-lucide-user-x" class="size-5 text-primary" />{{ t('portal.perks.noAccount') }}
      </li>
      <li class="flex flex-col items-center gap-1.5">
        <UIcon name="i-lucide-repeat" class="size-5 text-primary" />{{ t('portal.perks.exchange') }}
      </li>
    </ul>
  </section>
</template>
