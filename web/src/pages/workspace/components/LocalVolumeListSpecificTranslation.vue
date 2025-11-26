<script lang="ts" setup>
import { DeleteOutlineOutlined } from '@vicons/material';
import { useKeyModifier } from '@vueuse/core';
import { useI18n } from 'vue-i18n';

import { GenericNovelId } from '@/model/Common';
import type { LocalVolumeMetadata } from '@/model/LocalVolume';
import { useBookshelfLocalStore } from '@/pages/bookshelf/BookshelfLocalStore';
import { doAction } from '@/pages/util';
import { Setting, useLocalVolumeStore, useSettingStore } from '@/stores';
import { downloadFile } from '@/util';

const translateOptions = useTemplateRef('translateOptions');

const props = defineProps<{
  type: 'gpt' | 'sakura';
}>();

const message = useMessage();
const { t } = useI18n();

const settingStore = useSettingStore();
const { setting } = storeToRefs(settingStore);

const store = useBookshelfLocalStore();

const deleteVolume = (volumeId: string) =>
  doAction(
    store.deleteVolume(volumeId),
    t('workspace.localVolume.deleteAction'),
    message,
  );

const calculateFinished = (volume: LocalVolumeMetadata) =>
  volume.toc.filter((it) => {
    let chapterGlossaryId: string | undefined;
    if (props.type === 'gpt') {
      chapterGlossaryId = it.gpt;
    } else {
      chapterGlossaryId = it.sakura;
    }
    return chapterGlossaryId === volume.glossaryId;
  }).length;

const calculateExpired = (volume: LocalVolumeMetadata) =>
  volume.toc.filter((it) => {
    let chapterGlossaryId: string | undefined;
    if (props.type === 'gpt') {
      chapterGlossaryId = it.gpt;
    } else {
      chapterGlossaryId = it.sakura;
    }
    return (
      chapterGlossaryId !== undefined && chapterGlossaryId !== volume.glossaryId
    );
  }).length;

const queueAllVolumes = (volumes: LocalVolumeMetadata[]) => {
  const ids = volumes.map((it) => it.id);
  const { success, failed } = store.queueJobsToWorkspace(ids, {
    level: 'expire',
    type: props.type,
    shouldTop: shouldTopJob.value ?? false,
  });
  message.info(t('workspace.specific.queueResult', { success, failed }));
};

const shouldTopJob = useKeyModifier('Control');
const queueVolume = (volumeId: string, total: number = 65536) => {
  const { startIndex, endIndex, level, forceMetadata } =
    translateOptions.value!.getTranslateTaskParams();
  const taskNumber = translateOptions.value!.getTaskNumber();
  const success = store.queueJobToWorkspace(volumeId, {
    level: level,
    type: props.type,
    shouldTop: shouldTopJob.value ?? false,
    startIndex: startIndex,
    endIndex: endIndex,
    taskNumber: taskNumber,
    total: total,
  });
  if (success) {
    message.success(t('workspace.specific.queueSuccess'));
  } else {
    message.error(t('workspace.specific.queueExists'));
  }
};

const downloadVolume = async (volumeId: string) => {
  const { mode } = setting.value.downloadFormat;
  const repo = await useLocalVolumeStore();

  try {
    const { filename, blob } = await repo.getTranslationFile({
      id: volumeId,
      mode,
      translationsMode: 'priority',
      translations: [props.type],
    });
    downloadFile(filename, blob);
  } catch (error) {
    message.error(
      t('workspace.specific.downloadError', { error: String(error) }),
    );
  }
};

const progressFilter = ref<'all' | 'finished' | 'unfinished'>('all');
const progressFilterOptions = computed(() => [
  { value: 'all', label: t('workspace.specific.filterAll') },
  { value: 'finished', label: t('workspace.specific.filterFinished') },
  { value: 'unfinished', label: t('workspace.specific.filterUnfinished') },
]);
const progressFilterFunc = computed(() => {
  if (progressFilter.value === 'finished') {
    return (volume: LocalVolumeMetadata) => {
      return volume.toc.length === calculateFinished(volume);
    };
  } else if (progressFilter.value === 'unfinished') {
    return (volume: LocalVolumeMetadata) => {
      return volume.toc.length !== calculateFinished(volume);
    };
  } else {
    return undefined;
  }
});
</script>

<template>
  <local-volume-list
    :filter="progressFilterFunc"
    :options="{ [t('workspace.specific.queueAll')]: queueAllVolumes }"
    @volume-add="queueVolume($event.name)"
  >
    <template #extra>
      <TranslateOptions
        ref="translateOptions"
        :gnid="GenericNovelId.local('')"
        :glossary="{}"
      />
      <n-divider style="margin: 12px 0" />
      <c-action-wrapper :title="t('workspace.specific.statusTitle')">
        <c-radio-group
          v-model:value="progressFilter"
          :options="progressFilterOptions"
          size="small"
        />
      </c-action-wrapper>

      <c-action-wrapper :title="t('workspace.specific.languageTitle')">
        <c-radio-group
          v-model:value="setting.downloadFormat.mode"
          :options="Setting.downloadModeOptions"
          size="small"
        />
      </c-action-wrapper>
    </template>

    <template #volume="volume">
      <n-flex :size="4" vertical>
        <n-text>{{ volume.id }}</n-text>

        <n-text depth="3">
          <n-time :time="volume.createAt" type="relative" />
          / {{ t('workspace.specific.stats.total') }} {{ volume.toc.length }} /
          {{ t('workspace.specific.stats.finished') }}
          {{ calculateFinished(volume) }} /
          {{ t('workspace.specific.stats.expired') }}
          {{ calculateExpired(volume) }}
        </n-text>

        <n-flex :size="8">
          <c-button
            :label="t('workspace.specific.buttons.queue')"
            size="tiny"
            secondary
            @action="queueVolume(volume.id, volume.toc.length)"
          />

          <router-link
            v-if="!volume.id.endsWith('.epub')"
            :to="`/workspace/reader/${encodeURIComponent(volume.id)}/0`"
            target="_blank"
          >
            <c-button
              :label="t('workspace.specific.buttons.read')"
              size="tiny"
              secondary
            />
          </router-link>

          <c-button
            :label="t('workspace.specific.buttons.download')"
            size="tiny"
            secondary
            @action="downloadVolume(volume.id)"
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
