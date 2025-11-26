<script lang="ts" setup>
import { PlusOutlined } from '@vicons/material';
import type { UploadCustomRequestOptions, UploadFileInfo } from 'naive-ui';
import { useI18n } from 'vue-i18n';

import { useBookshelfLocalStore } from '../BookshelfLocalStore';

const { t } = useI18n();

const props = defineProps<{
  favoredId?: string;
}>();
const emit = defineEmits<{
  done: [File];
}>();

const message = useMessage();

const store = useBookshelfLocalStore();

const onFinish = ({ file }: { file: UploadFileInfo }) => {
  emit('done', file.file!);
};

const beforeUpload = ({ file }: { file: UploadFileInfo }) => {
  if (
    !(
      file.name.endsWith('.txt') ||
      file.name.endsWith('.srt') ||
      file.name.endsWith('.epub')
    )
  ) {
    message.error(t('bookshelf.localAddButton.uploadFailedType', { filename: file.name }));
    return false;
  }
  if (file.file?.size && file.file.size > 1024 * 1024 * 100) {
    message.error(t('bookshelf.localAddButton.uploadFailedSize', { filename: file.name }));
    return false;
  }
};

const customRequest = ({
  file,
  onFinish,
  onError,
}: UploadCustomRequestOptions) => {
  store
    .addVolume(file.file!, props.favoredId ?? 'default')
    .then(onFinish)
    .catch((error) => {
      message.error(t('bookshelf.localAddButton.uploadFailed', { error, filename: file.name }));
      onError();
    });
};
</script>

<template>
  <n-upload
    :show-file-list="false"
    accept=".txt,.epub,.srt"
    multiple
    directory-dnd
    :custom-request="customRequest"
    @before-upload="beforeUpload"
    @finish="onFinish"
  >
    <n-tooltip trigger="hover">
      <template #trigger>
        <c-button :label="t('bookshelf.localAddButton.add')" :icon="PlusOutlined" />
      </template>
      {{ t('bookshelf.localAddButton.uploadTip') }}
    </n-tooltip>
  </n-upload>
  <DropZone
    @finish="onFinish"
    accept=".txt,.epub,.srt"
    multiple
    directory-dnd
    :custom-request="customRequest"
    @before-upload="beforeUpload"
  >
    {{ t('bookshelf.localAddButton.dragHere') }}
  </DropZone>
</template>
