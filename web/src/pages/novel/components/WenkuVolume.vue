<script lang="ts" setup>
import { FileDownloadOutlined } from '@vicons/material';
import { useKeyModifier } from '@vueuse/core';
import { useI18n } from 'vue-i18n';

import { WenkuNovelApi } from '@/api';
import type { TranslateTaskParams } from '@/model/Translator';
import { TranslateTaskDescriptor } from '@/model/Translator';
import type { VolumeJpDto } from '@/model/WenkuNovel';
import { useSettingStore, useWhoamiStore, useWorkspaceStore } from '@/stores';

const { t } = useI18n();

const { novelId, volume, getParams } = defineProps<{
  novelId: string;
  volume: VolumeJpDto;
  getParams: () => TranslateTaskParams;
}>();

const emit = defineEmits<{
  delete: [];
}>();

const message = useMessage();

const settingStore = useSettingStore();
const { setting } = storeToRefs(settingStore);

const whoamiStore = useWhoamiStore();
const { whoami } = storeToRefs(whoamiStore);

const translateTask = useTemplateRef('translateTask');
const startTranslateTask = (translatorId: 'baidu' | 'youdao') => {
  return translateTask?.value?.startTask(
    { type: 'wenku', novelId, volumeId: volume.volumeId },
    getParams(),
    { id: translatorId },
  );
};

const file = computed(() => {
  const { mode, translationsMode, translations } = setting.value.downloadFormat;

  const { url, filename } = WenkuNovelApi.createFileUrl({
    novelId,
    volumeId: volume.volumeId,
    mode,
    translationsMode,
    translations,
  });
  return { url, filename };
});

const shouldTopJob = useKeyModifier('Control');
const submitJob = (id: 'gpt' | 'sakura') => {
  const task = TranslateTaskDescriptor.wenku(
    novelId,
    volume.volumeId,
    getParams(),
  );
  const workspace = useWorkspaceStore(id);
  const job = {
    task,
    description: volume.volumeId,
    createAt: Date.now(),
  };
  const success = workspace.addJob(job);
  if (success) {
    message.success(t('workspace.specific.queueSuccess'));
    if (shouldTopJob.value) {
      workspace.topJob(job);
    }
  } else {
    message.error(t('workspace.specific.queueExists'));
  }
};
</script>

<template>
  <n-flex align="center" justify="space-between" :wrap="false">
    <n-flex :size="4" vertical>
      <n-text>{{ volume.volumeId }}</n-text>

      <n-text depth="3">
        {{ t('home.panelTotal') }} {{ volume.total }} / {{ t('home.panelBaidu') }} {{ volume.baidu }} / {{ t('home.panelYoudao') }}
        {{ volume.youdao }} / {{ t('home.panelGpt') }} {{ volume.gpt }} / {{ t('home.panelSakura') }} {{ volume.sakura }}
      </n-text>

      <n-flex :size="8">
        <c-button
          v-if="setting.enabledTranslator.includes('baidu')"
          :label="t('novel.wenkuVolume.updateBaidu')"
          size="tiny"
          secondary
          @action="startTranslateTask('baidu')"
        />
        <c-button
          v-if="setting.enabledTranslator.includes('youdao')"
          :label="t('novel.wenkuVolume.updateYoudao')"
          size="tiny"
          secondary
          @action="startTranslateTask('youdao')"
        />

        <c-button
          v-if="setting.enabledTranslator.includes('gpt')"
          :label="t('novel.wenkuVolume.queueGpt')"
          size="tiny"
          secondary
          @action="submitJob('gpt')"
        />
        <c-button
          v-if="setting.enabledTranslator.includes('sakura')"
          :label="t('novel.wenkuVolume.queueSakura')"
          size="tiny"
          secondary
          @action="submitJob('sakura')"
        />
        <c-button-confirm
          v-if="whoami.asAdmin"
          :hint="t('novel.wenkuVolume.deleteConfirm', { volumeId: volume.volumeId })"
          :label="t('novel.wenkuVolume.delete')"
          type="error"
          size="tiny"
          secondary
          @action="emit('delete')"
        />
      </n-flex>
    </n-flex>

    <c-button
      :label="t('novel.wenkuVolume.download')"
      :icon="FileDownloadOutlined"
      tag="a"
      :href="file.url"
      :download="file.filename"
      target="_blank"
    />
  </n-flex>

  <TranslateTask
    ref="translateTask"
    @update:baidu="(vi) => (volume.baidu = vi)"
    @update:youdao="(vi) => (volume.youdao = vi)"
    @update:gpt="(vi) => (volume.gpt = vi)"
    @update:sakura="(vi) => (volume.sakura = vi)"
    style="margin-top: 20px"
  />
</template>
