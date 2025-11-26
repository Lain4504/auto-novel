<script lang="ts" setup>
import { FormatListBulletedOutlined } from '@vicons/material';
import { useI18n } from 'vue-i18n';

defineProps<{ menuKey: string }>();
const { t } = useI18n();
const showListModal = ref(false);
</script>

<template>
  <c-button
    :label="t('bookshelf.listButton.list')"
    :icon="FormatListBulletedOutlined"
    @action="showListModal = true"
  />

  <c-drawer-right v-model:show="showListModal" :title="t('bookshelf.listButton.favorites')">
    <template #action>
      <bookshelf-add-button />
    </template>
    <bookshelf-menu :value="menuKey" />
  </c-drawer-right>
</template>
