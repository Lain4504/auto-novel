<script setup lang="ts">
import { useI18n } from 'vue-i18n';

const { t } = useI18n();
</script>

<template>
  <div class="layout-content">
    <n-result
      status="404"
      :title="t('raw.404 页面不存在')"
      :description="t('raw.怎么回事捏？')"
      style="margin-top: 100px"
    />
  </div>
</template>
