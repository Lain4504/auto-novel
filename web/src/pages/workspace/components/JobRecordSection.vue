<script lang="ts" setup>
import {
  DeleteOutlineOutlined,
  FileDownloadOutlined,
  RefreshOutlined,
} from '@vicons/material';
import { useI18n } from 'vue-i18n';

import { TranslateJob, TranslateTaskDescriptor } from '@/model/Translator';
import { useBookshelfLocalStore } from '@/pages/bookshelf/BookshelfLocalStore';
import { useWorkspaceStore } from '@/stores';

const props = defineProps<{
  id: 'gpt' | 'sakura';
}>();

const message = useMessage();
const { t } = useI18n();

const workspace = useWorkspaceStore(props.id);
const workspaceRef = workspace.ref;

const store = useBookshelfLocalStore();

const progressFilter = ref<'all' | 'finished' | 'unfinished'>('all');
const progressFilterOptions = computed(() => [
  { value: 'all', label: t('workspace.jobRecordSection.filterAll') },
  { value: 'finished', label: t('workspace.jobRecordSection.filterFinished') },
  {
    value: 'unfinished',
    label: t('workspace.jobRecordSection.filterUnfinished'),
  },
]);

const records = computed(() => {
  const recordsAll = workspaceRef.value.uncompletedJobs;
  if (progressFilter.value === 'finished') {
    return recordsAll.filter((it) => TranslateJob.isFinished(it));
  } else if (progressFilter.value === 'unfinished') {
    return recordsAll.filter((it) => !TranslateJob.isFinished(it));
  } else {
    return recordsAll;
  }
});

const downloadVolumes = async () => {
  const volumeIds = records.value
    .map((it) => TranslateTaskDescriptor.parse(it.task).desc)
    .filter((it) => it.type === 'local')
    .map((it) => it.volumeId);

  if (volumeIds.length === 0) {
    message.info(t('workspace.jobRecordSection.emptyDownload'));
    return;
  }

  const { success, failed } = await store.downloadVolumes(volumeIds);
  message.info(t('workspace.localVolume.downloadResult', { success, failed }));
};
</script>

<template>
  <section-header :title="t('workspace.jobRecordSection.title')" />

  <n-flex vertical>
    <c-action-wrapper :title="t('workspace.jobRecordSection.statusTitle')">
      <c-radio-group
        v-model:value="progressFilter"
        :options="progressFilterOptions"
        size="small"
      />
    </c-action-wrapper>
    <c-action-wrapper
      :title="t('workspace.jobRecordSection.actionTitle')"
      align="center"
    >
      <n-button-group size="small">
        <c-button
          :label="t('workspace.jobRecordSection.retryAll')"
          :icon="RefreshOutlined"
          :round="false"
          @action="workspace.retryAllJobRecords()"
        />
        <c-button
          :label="t('workspace.jobRecordSection.downloadLocal')"
          :icon="FileDownloadOutlined"
          @click="downloadVolumes"
        />
        <c-button
          :label="t('workspace.jobRecordSection.clear')"
          :icon="DeleteOutlineOutlined"
          :round="false"
          @action="workspace.deleteAllJobRecords()"
        />
      </n-button-group>
    </c-action-wrapper>
  </n-flex>

  <n-divider style="margin: 16px 0 0" />

  <n-empty
    v-if="records.length === 0"
    :description="t('workspace.jobRecordSection.empty')"
    style="padding: 32px"
  />
  <n-list>
    <n-list-item v-for="job of records" :key="job.task">
      <job-record
        :job="job"
        @retry-job="workspace.retryJobRecord(job)"
        @delete-job="workspace.deleteJobRecord(job)"
      />
    </n-list-item>
  </n-list>
</template>
