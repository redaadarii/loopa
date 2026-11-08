<script setup lang="ts">
// Vignette produit : image de la boutique si disponible, sinon pictogramme teinté selon le type de produit.
const props = withDefaults(defineProps<{
  imageUrl?: string | null
  productType?: string | null
  title?: string
  size?: 'sm' | 'md' | 'lg'
}>(), { imageUrl: null, productType: null, title: '', size: 'md' })

const ICONS: Array<[RegExp, string, string]> = [
  [/chemise|shirt/i, 'i-lucide-shirt', 'bg-sky-100 text-sky-700'],
  [/t-shirt/i, 'i-lucide-shirt', 'bg-indigo-100 text-indigo-700'],
  [/pantalon|pant/i, 'i-lucide-ruler', 'bg-amber-100 text-amber-800'],
  [/robe|dress/i, 'i-lucide-sparkles', 'bg-rose-100 text-rose-700'],
  [/chaussure|basket|babouche|shoe/i, 'i-lucide-footprints', 'bg-stone-200 text-stone-700'],
  [/veste|jacket/i, 'i-lucide-layers', 'bg-blue-100 text-blue-800'],
  [/pull|maille/i, 'i-lucide-cloud', 'bg-violet-100 text-violet-700'],
  [/sac|accessoire|bag/i, 'i-lucide-shopping-bag', 'bg-orange-100 text-orange-800'],
  [/djellaba|traditionnel|caftan/i, 'i-lucide-crown', 'bg-emerald-100 text-emerald-800']
]

const visual = computed(() => {
  const key = `${props.productType ?? ''} ${props.title}`
  const found = ICONS.find(([re]) => re.test(key))
  return { icon: found?.[1] ?? 'i-lucide-package', tone: found?.[2] ?? 'bg-elevated text-muted' }
})
const sizeKey = computed(() => (props.size ?? 'md') as 'sm' | 'md' | 'lg')
const box = computed(() => ({ sm: 'size-10', md: 'size-14', lg: 'size-20' })[sizeKey.value])
const iconSize = computed(() => ({ sm: 'size-5', md: 'size-7', lg: 'size-9' })[sizeKey.value])
</script>

<template>
  <img
    v-if="imageUrl"
    :src="imageUrl"
    :alt="title"
    :class="[box, 'shrink-0 rounded-lg object-cover']"
  >
  <div v-else :class="[box, visual.tone, 'grid shrink-0 place-items-center rounded-lg']" aria-hidden="true">
    <UIcon :name="visual.icon" :class="iconSize" />
  </div>
</template>
