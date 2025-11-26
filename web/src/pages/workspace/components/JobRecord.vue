<script lang="ts" setup>
import { DeleteOutlineOutlined, RefreshOutlined } from '@vicons/material';
import { useI18n } from 'vue-i18n';

import type { TranslateJobRecord } from '@/model/Translator';
import { TranslateJob } from '@/model/Translator';

const props = defineProps<{
  job: TranslateJobRecord;
}>();
const emit = defineEmits<{
  retryJob: [];
  deleteJob: [];
}>();
const { t } = useI18n();
const isFinished = computed(() => TranslateJob.isFinished(props.job));
</script>

<template>
  <n-thing>
    <template #header>
      <job-task-link :task="job.task" />
    </template>
    <template #header-extra>
      <n-flex :size="6" :wrap="false">
        <c-icon-button
          v-if="!isFinished"
          :tooltip="t('workspace.jobRecord.retry')"
          :icon="RefreshOutlined"
          @action="emit('retryJob')"
        />

        <c-icon-button
          :tooltip="t('workspace.jobRecord.delete')"
          :icon="DeleteOutlineOutlined"
          type="error"
          @action="emit('deleteJob')"
        />
      </n-flex>
    </template>

    <template #description>
      {{ job.description }}
      <br />
      <n-text depth="3">
        <template v-if="!isFinished">
          {{ t('workspace.jobRecord.unfinished') }}
          <template v-if="job.progress !== undefined">
            {{
              t('workspace.jobRecord.progress', {
                total: job.progress?.total,
                finished: job.progress?.finished,
                error: job.progress?.error,
              })
            }}
          </template>
        </template>
        <template v-else>
          {{ t('workspace.jobRecord.finished') }}
          <n-time v-if="job?.finishAt" :time="job?.finishAt" type="datetime" />
        </template>
      </n-text>
    </template>
  </n-thing>
</template>
