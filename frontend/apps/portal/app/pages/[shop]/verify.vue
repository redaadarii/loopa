<script setup lang="ts">
const { t } = useI18n()
const route = useRoute()
const flow = usePortalFlow()
const portal = usePortalApi()

if (!flow.value.sessionId) {
  await navigateTo(`/${route.params.shop}`, { replace: true })
}

const code = ref<string[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

async function submit() {
  const value = code.value.join('')
  if (value.length !== 6 || !flow.value.sessionId) return
  loading.value = true
  error.value = null
  try {
    await portal.verify(flow.value.sessionId, value)
    await navigateTo(`/${route.params.shop}/order`)
  } catch (e) {
    error.value = apiErrorMessage(e)
    code.value = []
  } finally {
    loading.value = false
  }
}

watch(code, (v) => { if (v.join('').length === 6) submit() })

function useDevCode() {
  if (flow.value.devCode) {
    code.value = flow.value.devCode.split('')
  }
}
</script>

<template>
  <section class="space-y-6 text-center">
    <div class="mx-auto grid size-16 place-items-center rounded-full bg-primary/10">
      <UIcon name="i-lucide-mail-check" class="size-8 text-primary" />
    </div>
    <div class="space-y-2">
      <h1 class="text-3xl font-extrabold">
        {{ t('portal.verifyTitle') }}
      </h1>
      <p class="text-muted">
        {{ t('portal.verifySent', { contact: flow.maskedContact }) }}
      </p>
    </div>
    <UAlert v-if="error" color="error" variant="subtle" :title="error" class="text-start" />
    <div class="flex justify-center" dir="ltr">
      <UPinInput v-model="code" :length="6" size="xl" otp autofocus :disabled="loading" />
    </div>
    <UButton :label="t('portal.verify')" size="xl" block :loading="loading" :disabled="code.join('').length !== 6" @click="submit" />
    <div class="flex flex-col items-center gap-2">
      <UButton :label="t('portal.resend')" variant="link" :to="`/${route.params.shop}`" />
      <UButton
        v-if="flow.devCode"
        :label="t('portal.devCode', { code: flow.devCode })"
        color="neutral"
        variant="soft"
        size="sm"
        icon="i-lucide-flask-conical"
        @click="useDevCode"
      />
    </div>
  </section>
</template>
