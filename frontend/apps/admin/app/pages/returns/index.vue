<script setup lang="ts">
import type { TableColumn } from '@nuxt/ui'
import type { ReturnStatus, ReturnView } from '../../../../../layers/api/app/types/api'

const { t, locale } = useI18n()
const route = useRoute()
const router = useRouter()
const toast = useToast()
const StatusBadge = resolveComponent('StatusBadge')
const MoneyText = resolveComponent('MoneyText')
const UCheckbox = resolveComponent('UCheckbox')

const STATUSES: ReturnStatus[] = ['SUBMITTED', 'APPROVED', 'IN_TRANSIT', 'RECEIVED', 'RESOLVED', 'REJECTED', 'CANCELLED']
const status = ref<string>(typeof route.query.status === 'string' ? route.query.status : 'ALL')
const search = ref('')
const debounced = refDebounced(search, 300)
const filters = computed(() => ({
  status: status.value === 'ALL' ? undefined : status.value as ReturnStatus,
  q: debounced.value
}))
watch(status, s => router.replace({ query: s === 'ALL' ? {} : { status: s } }))

const { data, items, isPending, error, hasMore, loadMore, loadingMore } = useAdminReturns(filters)
const { download, loading: exporting } = useExportReturns()
const { mutateAsync: bulkApprove, isLoading: approving } = useBulkApprove()

// Sélection pour l'approbation groupée : uniquement les retours « à traiter ».
const selected = ref<Set<string>>(new Set())
watch(filters, () => { selected.value = new Set() })
const selectable = computed(() => items.value.filter(r => r.status === 'SUBMITTED'))
function toggle(id: string, value: boolean) {
  const next = new Set(selected.value)
  if (value) next.add(id)
  else next.delete(id)
  selected.value = next
}
function toggleAll(value: boolean) {
  selected.value = value ? new Set(selectable.value.map(r => r.id)) : new Set()
}
async function approveSelected() {
  try {
    const result = await bulkApprove([...selected.value])
    const failed = Object.keys(result.failed).length
    toast.add({
      title: t('admin.bulkDone', { count: result.approved.length }),
      description: failed ? t('admin.bulkFailed', { count: failed }) : undefined,
      color: failed ? 'warning' : 'success',
      icon: 'i-lucide-check'
    })
    selected.value = new Set()
  } catch (e) {
    toast.add({ title: apiErrorMessage(e), color: 'error' })
  }
}

const tabs = computed(() => {
  const counts = data.value?.counts
  const all = counts ? Object.values(counts).reduce((s, n) => s + n, 0) : undefined
  return [
    { label: t('common.all'), value: 'ALL', badge: all },
    ...STATUSES.map(s => ({ label: t(`status.${s}`), value: s, badge: counts?.[s] }))
  ]
})

const columns = computed<TableColumn<ReturnView>[]>(() => [
  {
    id: 'select',
    header: () => selectable.value.length
      ? h(UCheckbox, {
          'modelValue': selected.value.size > 0 && selected.value.size === selectable.value.length,
          'onUpdate:modelValue': (v: boolean | 'indeterminate') => toggleAll(v === true),
          'aria-label': t('admin.selectAll')
        })
      : null,
    cell: ({ row }) => row.original.status === 'SUBMITTED'
      ? h('div', { onClick: (e: Event) => e.stopPropagation() }, [h(UCheckbox, {
          'modelValue': selected.value.has(row.original.id),
          'onUpdate:modelValue': (v: boolean | 'indeterminate') => toggle(row.original.id, v === true),
          'aria-label': row.original.number
        })])
      : null
  },
  {
    accessorKey: 'number',
    header: () => t('admin.col.number'),
    cell: ({ row }) => h('span', { class: 'font-semibold text-highlighted' }, row.original.number)
  },
  {
    accessorKey: 'customerName',
    header: () => t('admin.col.customer'),
    cell: ({ row }) => h('div', [
      h('p', { class: 'font-medium text-highlighted' }, row.original.customerName ?? '—'),
      h('p', { class: 'text-xs text-muted' }, `${row.original.orderName} · ${row.original.lines.map(l => l.title).join(', ')}`)
    ])
  },
  { accessorKey: 'resolutionType', header: () => t('admin.col.resolution'), cell: ({ row }) => t(`resolution.${row.original.resolutionType}`) },
  {
    accessorKey: 'totalMinor',
    header: () => t('admin.col.amount'),
    cell: ({ row }) => h(MoneyText, { amountMinor: row.original.totalMinor, currency: row.original.currency })
  },
  { accessorKey: 'status', header: () => t('admin.col.status'), cell: ({ row }) => h(StatusBadge, { status: row.original.status }) },
  { accessorKey: 'createdAt', header: () => t('admin.col.date'), cell: ({ row }) => formatRelative(row.original.createdAt, locale.value) }
])

function openReturn(_e: Event, row: { original: ReturnView }) {
  navigateTo(`/returns/${row.original.id}`)
}
</script>

<template>
  <UDashboardPanel id="returns">
    <template #header>
      <UDashboardNavbar :title="t('admin.returns')">
        <template #right>
          <UButton
            :label="t('admin.export')"
            icon="i-lucide-download"
            color="neutral"
            variant="outline"
            :loading="exporting"
            @click="download(filters.status)"
          />
          <UInput
            v-model="search"
            icon="i-lucide-search"
            :placeholder="t('admin.search')"
            class="w-48 sm:w-72"
          />
        </template>
      </UDashboardNavbar>
      <UDashboardToolbar class="overflow-x-auto">
        <UTabs v-model="status" :items="tabs" :content="false" variant="link" size="sm" />
      </UDashboardToolbar>
    </template>

    <template #body>
      <UAlert v-if="error" color="error" variant="subtle" :title="apiErrorMessage(error)" />
      <div
        v-if="selected.size"
        class="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-primary/30 bg-primary/5 px-4 py-3"
      >
        <span class="text-sm font-semibold">{{ t('admin.selected', { count: selected.size }) }}</span>
        <div class="flex gap-2">
          <UButton :label="t('common.cancel')" color="neutral" variant="ghost" @click="selected = new Set()" />
          <UButton :label="t('admin.approveSelected')" icon="i-lucide-check-check" :loading="approving" @click="approveSelected" />
        </div>
      </div>
      <div class="overflow-hidden rounded-xl border border-default bg-default">
        <UTable
          :data="items"
          :columns="columns"
          :loading="isPending"
          :empty="t('admin.empty')"
          class="cursor-pointer"
          @select="openReturn"
        />
      </div>
      <div v-if="hasMore" class="flex justify-center">
        <UButton :label="t('admin.loadMore')" color="neutral" variant="outline" :loading="loadingMore" @click="loadMore" />
      </div>
    </template>
  </UDashboardPanel>
</template>
