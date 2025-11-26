<script lang="ts" setup>
import { DeleteOutlineOutlined, RemoveRedEyeOutlined } from '@vicons/material';
import { useI18n } from 'vue-i18n';

import type { ParsedFile } from '@/util/file';

defineProps<{
  file: ParsedFile;
}>();

const emit = defineEmits<{
  delete: [];
}>();

const { t } = useI18n();
const showPreviewModal = ref(false);
</script>

<template>
  <n-flex :size="4" aign="center" style="font-size: 12px" :wrap="false">
    <c-icon-button
      :tooltip="t('workspace.fileCard.preview')"
      :icon="RemoveRedEyeOutlined"
      text
      size="small"
      type="primary"
      @action="showPreviewModal = true"
    />
    <c-icon-button
      :tooltip="t('workspace.fileCard.remove')"
      :icon="DeleteOutlineOutlined"
      text
      size="small"
      type="error"
      @action="emit('delete')"
    />
    <n-text>{{ file.name }}</n-text>
  </n-flex>

  <c-modal
    :title="t('workspace.fileCard.modalTitle')"
    v-model:show="showPreviewModal"
  >
    <n-text>{{ t('workspace.fileCard.comingSoon') }}</n-text>
  </c-modal>
</template>
