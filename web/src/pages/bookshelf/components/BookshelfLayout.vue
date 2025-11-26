<script lang="ts" setup>
import { useI18n } from 'vue-i18n';

import { useIsWideScreen } from '@/pages/util';

defineProps<{ menuKey: string }>();
const { t } = useI18n();
const isWideScreen = useIsWideScreen();
</script>

<template>
  <c-layout :sidebar="isWideScreen" :sidebar-width="320" class="layout-content">
    <div style="flex: auto">
      <n-h1>{{ t('bookshelf.layout.myFavorites') }}</n-h1>
      <slot />
    </div>
    <template #sidebar>
      <section-header :title="t('bookshelf.layout.favorites')">
        <bookshelf-add-button />
      </section-header>
      <bookshelf-menu :value="menuKey" />
    </template>
  </c-layout>
</template>
