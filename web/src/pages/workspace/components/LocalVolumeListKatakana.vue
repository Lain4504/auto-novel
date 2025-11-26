<script lang="ts" setup>
import { DeleteOutlineOutlined } from '@vicons/material';
import { useI18n } from 'vue-i18n';

import { GenericNovelId } from '@/model/Common';

import { useBookshelfLocalStore } from '@/pages/bookshelf/BookshelfLocalStore';
import { doAction } from '@/pages/util';

defineEmits<{
  volumeLoaded: [string];
}>();

const message = useMessage();
const { t } = useI18n();

const store = useBookshelfLocalStore();

const deleteVolume = (volumeId: string) =>
  doAction(
    store.deleteVolume(volumeId),
    t('workspace.localVolume.deleteAction'),
    message,
  );
</script>

<template>
  <local-volume-list>
    <template #volume="volume">
      <n-flex :size="4" vertical>
        <n-text>{{ volume.id }}</n-text>

        <n-text depth="3">
          <n-time :time="volume.createAt" type="relative" />
          /
          {{ t('workspace.localVolume.total') }}
          {{ volume.toc.length }}
        </n-text>

        <n-flex :size="8">
          <c-button
            :label="t('workspace.localVolume.load')"
            size="tiny"
            secondary
            @action="$emit('volumeLoaded', volume.id)"
          />

          <glossary-button
            :gnid="GenericNovelId.local(volume.id)"
            :value="volume.glossary"
            size="tiny"
            secondary
          />

          <div style="flex: 1" />

          <c-button-confirm
            :hint="t('workspace.localVolume.deleteHint', { title: volume.id })"
            :icon="DeleteOutlineOutlined"
            size="tiny"
            secondary
            circle
            type="error"
            @action="deleteVolume(volume.id)"
          />
        </n-flex>
      </n-flex>
    </template>
  </local-volume-list>
</template>
