<script lang="ts" setup>
import { DeleteOutlineOutlined, PlusOutlined } from '@vicons/material';
import type { UploadCustomRequestOptions } from 'naive-ui';
import { useI18n } from 'vue-i18n';

import { useLocalVolumeStore } from '@/stores';
import type { ParsedFile } from '@/util/file';
import { parseFile } from '@/util/file';

const message = useMessage();
const { t } = useI18n();

const files = shallowRef<ParsedFile[]>([]);

const loadFile = async (file: File) => {
  if (files.value.find((it) => it.name === file.name) !== undefined) {
    message.warning(t('workspace.toolbox.fileLoaded'));
    return;
  }
  try {
    const toolboxFile = await parseFile(file, ['txt', 'epub']);
    files.value.push(toolboxFile);
    files.value = [...files.value];
    triggerRef(files);
  } catch (e) {
    message.warning(`${e}`);
  }
};

const removeFile = (name: string) => {
  files.value = files.value.filter((it) => !(it.name === name));
  triggerRef(files);
};

const clearFile = () => {
  files.value = [];
  triggerRef(files);
};

const loadLocalFile = (volumeId: string) =>
  useLocalVolumeStore()
    .then((repo) => repo.getFile(volumeId))
    .then((file) => {
      if (file === undefined)
        throw new Error(t('workspace.toolbox.novelMissing'));
      return loadFile(file.file);
    })
    .catch((error) =>
      message.error(
        t('workspace.toolbox.loadFailed', { error: String(error) }),
      ),
    );

const customRequest = ({
  file,
  onFinish,
  onError,
}: UploadCustomRequestOptions) => {
  if (!file.file) return;
  loadFile(file.file)
    .then(onFinish)
    .catch((err) => {
      message.error(
        t('workspace.toolbox.loadFailedShort', { error: String(err) }),
      );
      onError();
    });
};

const showListModal = ref(false);
</script>

<template>
  <div class="layout-content">
    <n-h1>{{ t('workspace.toolbox.title') }}</n-h1>

    <n-flex>
      <div>
        <n-upload
          :show-file-list="false"
          accept=".txt,.epub"
          multiple
          directory-dnd
          :custom-request="customRequest"
        >
          <c-button
            :label="t('workspace.toolbox.loadFiles')"
            :icon="PlusOutlined"
          />
        </n-upload>
      </div>

      <c-button
        :label="t('workspace.toolbox.localShelf')"
        :icon="PlusOutlined"
        @action="showListModal = true"
      />
      <c-button
        :label="t('workspace.toolbox.clear')"
        :icon="DeleteOutlineOutlined"
        @action="clearFile"
      />
    </n-flex>

    <n-flex vertical style="margin-top: 16px">
      <n-text v-for="file of files" :key="file.name">
        <toolbox-file-card :file="file" @delete="removeFile(file.name)" />
      </n-text>
    </n-flex>

    <n-tabs type="segment" animated style="margin-top: 48px">
      <n-tab-pane name="0" :tab="t('workspace.toolbox.tabGlossary')">
        <toolbox-item-glossary :files="files" />
      </n-tab-pane>
      <n-tab-pane name="1" :tab="t('workspace.toolbox.tabCompress')">
        <toolbox-item-compress-image :files="files" />
      </n-tab-pane>
      <n-tab-pane name="2" :tab="t('workspace.toolbox.tabFixOcr')">
        <toolbox-item-fix-ocr :files="files" />
      </n-tab-pane>
      <n-tab-pane name="3" :tab="t('workspace.toolbox.tabConvert')">
        <toolbox-item-convert v-model:files="files" />
      </n-tab-pane>
    </n-tabs>

    <local-volume-list-katakana
      v-model:show="showListModal"
      @volume-loaded="loadLocalFile"
    />
  </div>
</template>
