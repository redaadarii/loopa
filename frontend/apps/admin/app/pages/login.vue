<script setup lang="ts">
import * as z from 'zod'
import type { FormSubmitEvent } from '@nuxt/ui'

definePageMeta({ layout: 'auth' })

const { t } = useI18n()
const route = useRoute()
const config = useRuntimeConfig()
const { login } = useAuth()

const schema = z.object({
  email: z.email(),
  password: z.string().min(1)
})
type Schema = z.output<typeof schema>

const state = reactive<Partial<Schema>>({ email: '', password: '' })
const loading = ref(false)
const error = ref<string | null>(null)

async function onSubmit(event: FormSubmitEvent<Schema>) {
  loading.value = true
  error.value = null
  try {
    await login(event.data.email, event.data.password)
    await navigateTo(typeof route.query.redirect === 'string' ? route.query.redirect : '/')
  } catch (e) {
    error.value = apiErrorMessage(e)
  } finally {
    loading.value = false
  }
}

function fillDemo() {
  state.email = 'yasmine@atlas-store.ma'
  state.password = 'Loopa2026!'
}
</script>

<template>
  <div class="w-full max-w-md space-y-6">
    <div class="flex justify-center">
      <LoopaLogo />
    </div>
    <UCard :ui="{ body: 'p-6 sm:p-8' }">
      <div class="mb-6 space-y-1">
        <h1 class="text-2xl font-bold">
          {{ t('login.title') }}
        </h1>
        <p class="text-sm text-muted">
          {{ t('login.subtitle') }}
        </p>
      </div>
      <UAlert
        v-if="route.query.installed"
        color="success"
        variant="subtle"
        icon="i-lucide-check"
        :title="t('login.installed')"
        class="mb-4"
      />
      <UAlert v-if="error" color="error" variant="subtle" :title="error" class="mb-4" />
      <UForm :schema="schema" :state="state" class="space-y-4" @submit="onSubmit">
        <UFormField :label="t('login.email')" name="email">
          <UInput v-model="state.email" type="email" autocomplete="username" size="lg" class="w-full" />
        </UFormField>
        <UFormField :label="t('login.password')" name="password">
          <UInput v-model="state.password" type="password" autocomplete="current-password" size="lg" class="w-full" />
        </UFormField>
        <UButton type="submit" :label="t('login.submit')" size="lg" block :loading="loading" />
      </UForm>
    </UCard>
    <button
      v-if="config.public.demoLogin"
      type="button"
      class="mx-auto flex items-center gap-2 rounded-lg px-3 py-2 text-sm text-muted hover:bg-elevated"
      @click="fillDemo"
    >
      <UIcon name="i-lucide-sparkles" class="size-4 text-primary" />
      {{ t('login.demo') }} : yasmine@atlas-store.ma · Loopa2026!
    </button>
  </div>
</template>
