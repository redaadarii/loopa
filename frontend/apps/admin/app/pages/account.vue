<script setup lang="ts">
const { t } = useI18n()
const toast = useToast()
const { session } = useAuth()
const { mutateAsync, isLoading } = useChangePassword()

const form = reactive({ current: '', next: '', confirm: '' })
const error = ref<string | null>(null)

async function submit() {
  error.value = null
  if (form.next !== form.confirm) {
    error.value = t('admin.passwordMismatch')
    return
  }
  try {
    await mutateAsync({ currentPassword: form.current, newPassword: form.next })
    Object.assign(form, { current: '', next: '', confirm: '' })
    toast.add({ title: t('admin.passwordChanged'), color: 'success', icon: 'i-lucide-check' })
  } catch (e) {
    error.value = apiErrorMessage(e)
  }
}
</script>

<template>
  <UDashboardPanel id="account">
    <template #header>
      <UDashboardNavbar :title="t('admin.account')" />
    </template>
    <template #body>
      <div class="mx-auto w-full max-w-xl space-y-6">
        <div class="flex items-center gap-4">
          <UAvatar :alt="session?.user.name" size="xl" />
          <div>
            <p class="text-lg font-semibold">
              {{ session?.user.name }}
            </p>
            <p class="text-sm text-muted">
              {{ session?.user.email }} · {{ t(`admin.roles.${session?.user.role ?? 'AGENT'}`) }}
            </p>
          </div>
        </div>
        <p class="text-muted">
          {{ t('admin.accountIntro') }}
        </p>
        <UCard>
          <UAlert v-if="error" color="error" variant="subtle" :title="error" class="mb-4" />
          <form class="space-y-4" @submit.prevent="submit">
            <UFormField :label="t('admin.currentPassword')" required>
              <UInput v-model="form.current" type="password" autocomplete="current-password" class="w-full" />
            </UFormField>
            <UFormField :label="t('admin.newPassword')" :help="t('admin.newPasswordHint')" required>
              <UInput v-model="form.next" type="password" autocomplete="new-password" minlength="10" class="w-full" />
            </UFormField>
            <UFormField :label="t('admin.confirmPassword')" required>
              <UInput v-model="form.confirm" type="password" autocomplete="new-password" class="w-full" />
            </UFormField>
            <div class="flex justify-end">
              <UButton
                type="submit"
                :label="t('admin.changePassword')"
                :loading="isLoading"
                :disabled="!form.current || form.next.length < 10"
              />
            </div>
          </form>
        </UCard>
      </div>
    </template>
  </UDashboardPanel>
</template>
