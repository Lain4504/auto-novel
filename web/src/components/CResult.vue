<script setup lang="ts" generic="T extends any">
import { useI18n } from 'vue-i18n';

import type { Result } from '@/util/result';

defineProps<{
  result?: Result<T>;
  showEmpty?: (value: T) => boolean;
}>();

const { t } = useI18n();
</script>

<template>
  <template v-if="result?.ok">
    <n-empty
      v-if="showEmpty && showEmpty(result.value)"
      :description="t('components.cResult.empty')"
    />
    <slot v-else :value="result.value" />
  </template>

  <n-result
    v-else-if="result && !result.ok"
    status="error"
    :title="t('components.cResult.errorTitle')"
    :description="result.error.message"
  />
</template>
