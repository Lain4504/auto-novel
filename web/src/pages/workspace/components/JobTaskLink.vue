<script lang="ts" setup>
import { useI18n } from 'vue-i18n';

import { TranslateTaskDescriptor } from '@/model/Translator';

const props = defineProps<{
  task: string;
}>();

const { t } = useI18n();

const link = computed(() => {
  const { desc, params } = TranslateTaskDescriptor.parse(props.task);
  const { level, forceMetadata, startIndex, endIndex } = params;

  let text: string;
  let url: string | undefined;
  if (desc.type === 'web') {
    text = `web/${desc.providerId}/${desc.novelId}`;
    url = `/novel/${desc.providerId}/${desc.novelId}`;
  } else if (desc.type === 'wenku') {
    text = `wenku/${desc.novelId}`;
    url = `/wenku/${desc.novelId}`;
  } else {
    text = 'local';
    url = undefined;
  }

  if (startIndex > 0 || endIndex < 65535) {
    const endLabel = endIndex < 65535 ? endIndex : t('workspace.jobTask.inf');
    text += ` [${startIndex},${endLabel})`;
  }

  const tags: string[] = [];
  if (level === 'expire') {
    tags.push(t('workspace.jobTask.tags.expire'));
  } else if (level === 'all') {
    tags.push(t('workspace.jobTask.tags.all'));
  } else if (level === 'sync') {
    tags.push(t('workspace.jobTask.tags.sync'));
  }
  if (forceMetadata) tags.push(t('workspace.jobTask.tags.metadata'));
  if (tags.length > 0) {
    text += ` [${tags.join('/')}]`;
  }

  return { text, url };
});
</script>

<template>
  <router-link v-if="link.url" :to="link.url" target="_blank">
    <n-text depth="3" underline style="font-size: 12px">
      {{ link.text }}
    </n-text>
  </router-link>
  <n-text v-else depth="3" style="font-size: 12px">
    {{ link.text }}
  </n-text>
</template>
