<script setup lang="ts">
import type { Branding } from '../../../../../layers/api/app/types/api'

const { t } = useI18n()
const toast = useToast()
const config = useRuntimeConfig()
const { isAdmin } = useAuth()
const { data, save } = useBranding()
const { data: policy } = usePolicy()

type BrandingForm = Omit<Branding, 'logoUrl' | 'welcomeMessage'> & { logoUrl: string, welcomeMessage: string }
const form = ref<BrandingForm>({ name: '', primaryColor: '#0E7C6B', logoUrl: '', welcomeMessage: '', handle: '' })
const ready = ref(false)
watch(data, (b) => {
  if (b) {
    form.value = { ...b, logoUrl: b.logoUrl ?? '', welcomeMessage: b.welcomeMessage ?? '' }
    ready.value = true
  }
}, { immediate: true })
const portalLink = computed(() => `${config.public.portalUrl}/${form.value.handle}`)
const SWATCHES = ['#0E7C6B', '#1D4ED8', '#7C3AED', '#BE123C', '#C2410C', '#0F172A']

async function submit() {
  try {
    await save.mutateAsync({ ...form.value, logoUrl: form.value.logoUrl || null, welcomeMessage: form.value.welcomeMessage || null })
    toast.add({ title: t('common.saved'), color: 'success', icon: 'i-lucide-check' })
  } catch (e) {
    toast.add({ title: apiErrorMessage(e), color: 'error' })
  }
}
</script>

<template>
  <UDashboardPanel id="branding">
    <template #header>
      <UDashboardNavbar :title="t('admin.branding')" />
    </template>
    <template #body>
      <div v-if="ready" class="mx-auto grid w-full max-w-5xl gap-6 lg:grid-cols-[1fr_360px]">
        <div class="space-y-6">
          <p class="text-muted">
            {{ t('admin.brandingIntro') }}
          </p>
          <UCard>
            <form class="space-y-5" @submit.prevent="submit">
              <fieldset :disabled="!isAdmin" class="space-y-5">
                <UFormField :label="t('admin.shopName')">
                  <UInput v-model="form.name" class="w-full" />
                </UFormField>
                <UFormField :label="t('admin.primaryColor')">
                  <div class="flex flex-wrap items-center gap-2">
                    <button
                      v-for="c in SWATCHES"
                      :key="c"
                      type="button"
                      class="size-9 rounded-full ring-offset-2 transition"
                      :class="form.primaryColor.toUpperCase() === c ? 'ring-2 ring-ink' : ''"
                      :style="{ background: c }"
                      :aria-label="c"
                      @click="form.primaryColor = c"
                    />
                    <UInput v-model="form.primaryColor" class="w-32 font-mono" />
                  </div>
                </UFormField>
                <UFormField :label="t('admin.logoUrl')">
                  <UInput v-model="form.logoUrl" placeholder="https://…/logo.png" class="w-full" />
                </UFormField>
                <UFormField :label="t('admin.welcome')">
                  <UTextarea v-model="form.welcomeMessage" :rows="3" :maxlength="280" class="w-full" />
                </UFormField>
              </fieldset>
              <div v-if="isAdmin" class="flex justify-end">
                <UButton type="submit" :label="t('common.save')" :loading="save.isLoading.value" />
              </div>
            </form>
          </UCard>
          <UCard>
            <p class="mb-2 text-sm font-medium">
              {{ t('admin.portalLink') }}
            </p>
            <div class="flex gap-2">
              <UInput :model-value="portalLink" readonly class="flex-1 font-mono" />
              <UButton icon="i-lucide-external-link" color="neutral" variant="outline" :to="portalLink" target="_blank" :aria-label="portalLink" />
            </div>
          </UCard>
        </div>

        <!-- Aperçu en direct du portail mobile -->
        <div>
          <p class="mb-2 text-sm font-medium text-muted">
            {{ t('admin.preview') }}
          </p>
          <div class="overflow-hidden rounded-[2rem] border-8 border-ink bg-canvas shadow-xl" :style="{ '--ui-primary': form.primaryColor }">
            <div class="flex h-12 items-center border-b border-default bg-white px-4">
              <img v-if="form.logoUrl" :src="form.logoUrl" alt="" class="h-7">
              <span v-else class="font-display font-bold">{{ form.name }}</span>
            </div>
            <div class="space-y-4 p-5">
              <span class="inline-block rounded-full px-2.5 py-1 text-xs font-semibold text-white" :style="{ background: form.primaryColor }">
                {{ t('portal.windowInfo', { days: policy?.windowDays ?? 30 }) }}
              </span>
              <h3 class="text-2xl font-extrabold leading-tight">
                {{ t('portal.title') }}
              </h3>
              <p class="text-sm text-muted">
                {{ form.welcomeMessage || t('portal.subtitle') }}
              </p>
              <div class="space-y-3 rounded-xl border border-default bg-white p-4">
                <div class="h-9 rounded-lg border border-default" />
                <div class="h-9 rounded-lg border border-default" />
                <div class="grid h-10 place-items-center rounded-lg text-sm font-bold text-white" :style="{ background: form.primaryColor }">
                  {{ t('portal.find') }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </UDashboardPanel>
</template>
