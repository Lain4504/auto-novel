<script lang="ts" setup>
import { useI18n } from 'vue-i18n';

import type { WebNovelOutlineDto } from '@/model/WebNovel';

defineProps<{
  novels: WebNovelOutlineDto[] | undefined;
  error: Error | null;
}>();

const { t } = useI18n();
</script>

<template>
  <template v-if="novels !== undefined">
    <n-grid :x-gap="12" :y-gap="12" cols="1 850:4">
      <n-grid-item
        v-for="item in novels"
        :key="`${item.providerId}/${item.novelId}`"
        style="padding: 8px"
      >
        <c-a :to="`/novel/${item.providerId}/${item.novelId}`">
          <span class="text-2line">
            {{ item.titleJp }}
          </span>
        </c-a>
        <div class="text-2line">{{ item.titleZh }}</div>
        <n-text depth="3">
          {{ item.type }} / {{ t('home.panelTotal') }} {{ item.total }} /
          {{ t('home.panelBaidu') }} {{ item.baidu }}
          <br />
          {{ t('home.panelYoudao') }} {{ item.youdao }} /
          {{ t('home.panelGpt') }} {{ item.gpt }} / {{ t('home.panelSakura') }}
          {{ item.sakura }}
        </n-text>
      </n-grid-item>
    </n-grid>
    <n-empty v-if="novels.length === 0" :description="t('home.panelEmpty')" />
  </template>

  <CResultX v-else :error="error" :title="t('home.panelError')" />
</template>
