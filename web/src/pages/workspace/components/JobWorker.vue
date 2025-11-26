<script lang="ts" setup>
import {
  DeleteOutlineOutlined,
  DragIndicatorOutlined,
  FlashOnOutlined,
  FontDownloadOffOutlined,
  FontDownloadOutlined,
  PlayArrowOutlined,
  SettingsOutlined,
  StopOutlined,
} from '@vicons/material';
import { useI18n } from 'vue-i18n';

import type { TranslatorConfig } from '@/domain/translate';
import { Translator } from '@/domain/translate';
import type { GptWorker, SakuraWorker } from '@/model/Translator';
import { TranslateTaskDescriptor } from '@/model/Translator';
import { useWorkspaceStore } from '@/stores';

const props = defineProps<{
  worker:
    | ({ translatorId: 'sakura' } & SakuraWorker)
    | ({ translatorId: 'gpt' } & GptWorker);
  getNextJob: () =>
    | { task: string; description: string; createAt: number }
    | undefined;
}>();

const emit = defineEmits<{
  'update:progress': [
    string,
    (
      | { state: 'finish'; abort: boolean }
      | { state: 'processed'; finished: number; error: number; total: number }
    ),
  ];
}>();

const message = useMessage();
const { t } = useI18n();

const translatorConfig = computed(() => {
  const worker = props.worker;
  if (worker.translatorId === 'gpt') {
    return <TranslatorConfig & { id: 'gpt' }>{
      id: 'gpt',
      type: worker.type,
      model: worker.model,
      endpoint: worker.endpoint,
      key: worker.key,
    };
  } else {
    return <TranslatorConfig & { id: 'sakura' }>{
      id: 'sakura',
      endpoint: worker.endpoint,
      segLength: worker.segLength,
      prevSegLength: worker.prevSegLength,
    };
  }
});

const endpointPrefix = computed(() => {
  const worker = props.worker;
  if (worker.translatorId === 'gpt') {
    if (worker.type === 'web') {
      return `web[${worker.key.slice(-4)}]@`;
    } else {
      return `${worker.model}[${worker.key.slice(-4)}]@`;
    }
  } else {
    return `${worker.segLength ?? 500}@`;
  }
});

const enableAutoMode = ref(true);

const translateTask = useTemplateRef('translateTask');
const currentJob = ref<{
  task: string;
  description: string;
  createAt: number;
}>();
const running = computed(() => currentJob.value !== undefined);

let abortHandler = () => {};

const processTasks = async () => {
  const controller = new AbortController();
  const { signal } = controller;
  abortHandler = () => controller.abort();

  while (true) {
    const job = props.getNextJob();
    currentJob.value = job;

    if (job === undefined) break;
    const { desc, params } = TranslateTaskDescriptor.parse(job.task);

    const state = await translateTask.value!.startTask(
      desc,
      params,
      translatorConfig.value,
      {
        onProgressUpdated: (progress) => {
          emit('update:progress', job.task, {
            state: 'processed',
            ...progress,
          });
        },
      },
      signal,
    );
    emit('update:progress', job.task, {
      state: 'finish',
      abort: state === 'abort',
    });

    if (state !== 'complete' || !enableAutoMode.value) {
      break;
    }
  }
  currentJob.value = undefined;
};

const startWorker = () => {
  if (running.value) return;
  processTasks();
};
const stopWorker = () => {
  if (!running.value) return;
  abortHandler();
};
const deleteWorker = () => {
  const worker = props.worker;
  abortHandler();
  const workspace = useWorkspaceStore(worker.translatorId);
  workspace.deleteWorker(worker.id);
};

const testWorker = async () => {
  const worker = props.worker;
  const textJp = [
    '国境の長いトンネルを抜けると雪国であった。夜の底が白くなった。信号所に汽車が止まった。',
  ];
  try {
    const translator = await Translator.create(translatorConfig.value);
    const textZh = await translator.translate(textJp);

    const lineJp = textJp[0];
    const lineZh = textZh[0];

    if (worker.translatorId === 'gpt') {
      message.success(
        t('workspace.jobWorker.testSuccessGpt', {
          jp: lineJp,
          zh: lineZh,
        }),
      );
    } else {
      const uploadLabel = translator.allowUpload()
        ? t('workspace.jobWorker.uploadAllowed')
        : t('workspace.jobWorker.uploadForbidden');
      message.success(
        t('workspace.jobWorker.testSuccessSakura', {
          jp: lineJp,
          zh: lineZh,
          model: translator.sakuraModel(),
          upload: uploadLabel,
        }),
      );
    }
  } catch (e: unknown) {
    message.error(t('workspace.jobWorker.testError', { error: String(e) }));
  }
};

const showEditWorkerModal = ref(false);
</script>

<template>
  <n-thing content-indented>
    <template #avatar>
      <n-icon
        class="drag-trigger"
        :size="18"
        :depth="2"
        :component="DragIndicatorOutlined"
        style="cursor: move"
      />
    </template>

    <template #header>
      {{ worker.id }}
      <n-text depth="3" style="font-size: 12px; padding-left: 2px">
        {{ endpointPrefix }}{{ translatorConfig.endpoint }}
      </n-text>
    </template>

    <template #description>
      <n-p v-if="currentJob">
        {{ currentJob.description }}
      </n-p>
    </template>

    <template #header-extra>
      <n-flex :size="6" :wrap="false">
        <c-button
          v-if="running"
          :label="t('workspace.jobWorker.stop')"
          :icon="StopOutlined"
          size="tiny"
          secondary
          @action="stopWorker"
        />
        <c-button
          v-else
          :label="t('workspace.jobWorker.start')"
          :icon="PlayArrowOutlined"
          size="tiny"
          secondary
          @action="startWorker"
        />

        <c-icon-button
          :tooltip="t('workspace.jobWorker.test')"
          :icon="FlashOnOutlined"
          @action="testWorker"
        />

        <c-icon-button
          :tooltip="t('workspace.jobWorker.settings')"
          :icon="SettingsOutlined"
          @action="showEditWorkerModal = !showEditWorkerModal"
        />

        <c-icon-button
          v-if="enableAutoMode"
          :tooltip="t('workspace.jobWorker.autoOn')"
          :icon="FontDownloadOutlined"
          @action="enableAutoMode = false"
        />
        <c-icon-button
          v-else
          :tooltip="t('workspace.jobWorker.autoOff')"
          :icon="FontDownloadOffOutlined"
          @action="enableAutoMode = true"
        />

        <c-icon-button
          :tooltip="t('workspace.jobWorker.delete')"
          :icon="DeleteOutlineOutlined"
          type="error"
          @action="deleteWorker"
        />
      </n-flex>
    </template>
  </n-thing>

  <TranslateTask ref="translateTask" style="margin-top: 20px" />

  <sakura-worker-modal
    v-if="worker.translatorId === 'sakura'"
    v-model:show="showEditWorkerModal"
    :worker="worker"
  />
  <gpt-worker-modal
    v-else
    v-model:show="showEditWorkerModal"
    :worker="worker"
  />
</template>
