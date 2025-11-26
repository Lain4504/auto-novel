<script lang="ts" setup>
import {
  DeleteOutlineOutlined,
  DragIndicatorOutlined,
  KeyboardDoubleArrowDownOutlined,
  KeyboardDoubleArrowUpOutlined,
} from '@vicons/material';
import { useI18n } from 'vue-i18n';

import type { TranslateJob } from '@/model/Translator';

const props = defineProps<{
  job: TranslateJob;
  progress?: { finished: number; error: number; total: number };
}>();
const emit = defineEmits<{
  topJob: [];
  bottomJob: [];
  deleteJob: [];
}>();
const { t } = useI18n();

const percentage = computed(() => {
  if (props.progress === undefined) {
    return 0;
  }
  const { finished, error, total } = props.progress;
  if (total === 0) {
    return 100;
  } else {
    return Math.round((1000 * (finished + error)) / total) / 10;
  }
});
</script>

<template>
  <n-thing>
    <template #avatar>
      <n-flex vertical justify="center" style="height: 100%">
        <n-icon
          class="drag-trigger"
          :size="18"
          :depth="2"
          :component="DragIndicatorOutlined"
          style="cursor: move"
        />
      </n-flex>
    </template>

    <template #header>
      <job-task-link :task="job.task" />
    </template>
    <template #header-extra>
      <n-flex :size="6" :wrap="false">
        <c-icon-button
          :tooltip="t('workspace.jobQueue.top')"
          :icon="KeyboardDoubleArrowUpOutlined"
          @action="emit('topJob')"
        />

        <c-icon-button
          :tooltip="t('workspace.jobQueue.bottom')"
          :icon="KeyboardDoubleArrowDownOutlined"
          @action="emit('bottomJob')"
        />

        <c-icon-button
          :tooltip="t('workspace.jobQueue.delete')"
          :icon="DeleteOutlineOutlined"
          type="error"
          @action="emit('deleteJob')"
        />
      </n-flex>
    </template>

    <template #description>
      {{ job.description }}
      <template v-if="percentage">
        <n-progress :percentage="percentage" style="max-width: 600px" />
      </template>
    </template>
  </n-thing>
</template>
