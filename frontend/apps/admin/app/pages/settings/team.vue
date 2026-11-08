<script setup lang="ts">
import type { Member, Role } from '../../../../../layers/api/app/types/api'

const { t, locale } = useI18n()
const toast = useToast()
const { data: members, isPending, invite, update } = useTeam()

const open = ref(false)
const form = reactive<{ email: string, name: string, role: Role }>({ email: '', name: '', role: 'AGENT' })
const created = ref<{ name: string, password: string } | null>(null)
const roleItems = computed(() => (['ADMIN', 'AGENT'] as Role[]).map(r => ({ label: t(`admin.roles.${r}`), value: r })))

async function submitInvite() {
  try {
    const result = await invite.mutateAsync({ ...form })
    created.value = { name: result.member.name, password: result.temporaryPassword }
    toast.add({ title: t('admin.inviteSent'), color: 'success', icon: 'i-lucide-check' })
    Object.assign(form, { email: '', name: '', role: 'AGENT' })
  } catch (e) {
    toast.add({ title: apiErrorMessage(e), color: 'error' })
  }
}

async function change(member: Member, patch: { role?: Role, active?: boolean }) {
  try {
    await update.mutateAsync({ id: member.id, ...patch })
    toast.add({ title: t('common.saved'), color: 'success' })
  } catch (e) {
    toast.add({ title: apiErrorMessage(e), color: 'error' })
  }
}

function closeModal() {
  open.value = false
  created.value = null
}
</script>

<template>
  <UDashboardPanel id="team">
    <template #header>
      <UDashboardNavbar :title="t('admin.team')">
        <template #right>
          <UModal v-model:open="open" :title="t('admin.invite')" @after:leave="created = null">
            <UButton :label="t('admin.invite')" icon="i-lucide-user-plus" />
            <template #body>
              <div v-if="created" class="space-y-3">
                <p class="text-sm">
                  {{ t('admin.tempPassword', { name: created.name }) }}
                </p>
                <UInput :model-value="created.password" readonly class="w-full font-mono" />
              </div>
              <form v-else id="invite-form" class="space-y-4" @submit.prevent="submitInvite">
                <UFormField :label="t('admin.name')" required>
                  <UInput v-model="form.name" class="w-full" />
                </UFormField>
                <UFormField :label="t('admin.email')" required>
                  <UInput v-model="form.email" type="email" class="w-full" />
                </UFormField>
                <UFormField :label="t('admin.role')">
                  <USelect v-model="form.role" :items="roleItems" class="w-full" />
                </UFormField>
              </form>
            </template>
            <template #footer>
              <UButton v-if="created" :label="t('common.close')" @click="closeModal" />
              <UButton v-else type="submit" form="invite-form" :label="t('admin.invite')" :loading="invite.isLoading.value" />
            </template>
          </UModal>
        </template>
      </UDashboardNavbar>
    </template>
    <template #body>
      <div class="mx-auto w-full max-w-4xl space-y-4">
        <p class="text-muted">
          {{ t('admin.teamIntro') }}
        </p>
        <USkeleton v-if="isPending" class="h-48 rounded-xl" />
        <UCard v-else :ui="{ body: 'p-0 sm:p-0' }">
          <ul class="divide-y divide-default">
            <li v-for="m in members" :key="m.id" class="flex flex-wrap items-center gap-4 px-5 py-4">
              <UAvatar :alt="m.name" />
              <div class="min-w-0 flex-1">
                <p class="font-semibold" :class="!m.active && 'text-muted line-through'">
                  {{ m.name }}
                </p>
                <p class="truncate text-sm text-muted">
                  {{ m.email }} · {{ t('admin.lastLogin') }} :
                  {{ m.lastLoginAt ? formatRelative(m.lastLoginAt, locale) : t('admin.never') }}
                </p>
              </div>
              <UBadge v-if="m.role === 'OWNER'" color="primary" variant="subtle" :label="t('admin.roles.OWNER')" />
              <template v-else>
                <USelect
                  :model-value="m.role"
                  :items="roleItems"
                  size="sm"
                  class="w-40"
                  @update:model-value="(r) => change(m, { role: r as Role })"
                />
                <UButton
                  :label="m.active ? t('admin.deactivate') : t('admin.activate')"
                  size="sm"
                  :color="m.active ? 'neutral' : 'primary'"
                  variant="ghost"
                  @click="change(m, { active: !m.active })"
                />
              </template>
            </li>
          </ul>
        </UCard>
      </div>
    </template>
  </UDashboardPanel>
</template>
