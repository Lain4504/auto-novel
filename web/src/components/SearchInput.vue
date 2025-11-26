<script lang="ts" setup>
import { SearchOutlined } from '@vicons/material';
import { useI18n } from 'vue-i18n';

const value = defineModel<{
  query: string;
  enableRegexMode: boolean;
}>('value', {
  required: true,
});

const attrs = useAttrs();
const { t } = useI18n();
</script>
<template>
  <n-input
    clearable
    size="small"
    v-model:value="value.query"
    type="text"
    v-bind="attrs"
  >
    <template #suffix><n-icon :component="SearchOutlined" /></template>
  </n-input>

  <tag-button
    :label="t('components.searchInput.regex')"
    v-model:checked="value.enableRegexMode"
  />
</template>
