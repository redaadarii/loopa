<script setup lang="ts">
import type { DropdownMenuItem, NavigationMenuItem } from '@nuxt/ui'

const { t, locales, setLocale, locale } = useI18n()
const { session, logout, isAdmin } = useAuth()
const route = useRoute()
const { data: summary } = useAnalytics(30)

const links = computed<NavigationMenuItem[][]>(() => [[
  { label: t('admin.dashboard'), icon: 'i-lucide-layout-dashboard', to: '/' },
  {
    label: t('admin.returns'),
    icon: 'i-lucide-undo-2',
    to: '/returns',
    badge: summary.value?.pending ? String(summary.value.pending) : undefined
  },
  {
    label: t('admin.settings'),
    icon: 'i-lucide-settings',
    defaultOpen: route.path.startsWith('/settings'),
    children: [
      { label: t('admin.policy'), to: '/settings/policy' },
      { label: t('admin.branding'), to: '/settings/branding' },
      ...(isAdmin.value
        ? [
            { label: t('admin.team'), to: '/settings/team' },
            { label: t('admin.billing'), to: '/settings/billing' },
            { label: t('admin.integrations'), to: '/settings/integrations' }
          ]
        : [])
    ]
  }
]])

const userMenu = computed<DropdownMenuItem[][]>(() => [
  [
    { label: session.value?.user.email ?? '', type: 'label' },
    { label: t('admin.account'), icon: 'i-lucide-user-cog', to: '/account' }
  ],
  locales.value.map(l => ({
    label: l.name ?? l.code,
    icon: l.code === locale.value ? 'i-lucide-check' : undefined,
    onSelect: () => setLocale(l.code)
  })),
  [{ label: t('common.logout'), icon: 'i-lucide-log-out', onSelect: () => logout() }]
])
</script>

<template>
  <UDashboardGroup>
    <UDashboardSidebar collapsible resizable :ui="{ root: 'bg-default' }">
      <template #header="{ collapsed }">
        <NuxtLink to="/" class="flex items-center">
          <LoopaLogo :with-text="!collapsed" />
        </NuxtLink>
      </template>

      <template #default="{ collapsed }">
        <UNavigationMenu :items="links[0]" :collapsed="collapsed" orientation="vertical" highlight />
      </template>

      <template #footer="{ collapsed }">
        <UDropdownMenu :items="userMenu" :content="{ side: 'top', align: 'start' }" class="w-full">
          <UButton
            color="neutral"
            variant="ghost"
            block
            :square="collapsed"
            class="justify-start data-[state=open]:bg-elevated"
          >
            <UAvatar :alt="session?.user.name" size="xs" />
            <span v-if="!collapsed" class="min-w-0 text-start">
              <span class="block truncate text-sm font-semibold">{{ session?.user.name }}</span>
              <span class="block truncate text-xs text-muted">
                {{ session?.shop.name }} · {{ t(`admin.roles.${session?.user.role ?? 'AGENT'}`) }}
              </span>
            </span>
          </UButton>
        </UDropdownMenu>
      </template>
    </UDashboardSidebar>

    <slot />
  </UDashboardGroup>
</template>
