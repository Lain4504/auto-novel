<script lang="ts" setup>
import { useI18n } from 'vue-i18n';

import avaterUrl from '@/image/avater.jpg';

const { t, tm } = useI18n();

const guides = computed(() =>
  (tm('components.markdownGuide.guides') as string[]).map((guide) =>
    guide.replace('{image}', avaterUrl),
  ),
);
</script>

<template>
  <c-modal :title="t('components.markdownGuide.title')">
    <n-p>{{ t('components.markdownGuide.unsupported') }}</n-p>
    <n-table :bordered="false" style="table-layout: fixed; width: 100%">
      <thead>
        <tr>
          <th>
            <b>{{ t('components.markdownGuide.syntax') }}</b>
          </th>
          <th>
            <b>{{ t('components.markdownGuide.preview') }}</b>
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="guide in guides" :key="guide">
          <td style="white-space: pre-wrap">
            {{ guide.trimStart() }}
          </td>
          <td>
            <MarkdownView mode="article" :source="guide" />
          </td>
        </tr>
      </tbody>
    </n-table>
  </c-modal>
</template>
