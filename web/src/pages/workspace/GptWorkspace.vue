<script lang="ts" setup>
import {
  BookOutlined,
  DeleteOutlineOutlined,
  PlusOutlined,
} from '@vicons/material';
import { VueDraggable } from 'vue-draggable-plus';
import { useI18n } from 'vue-i18n';

import { TranslationCacheRepo } from '@/repos';
import type { TranslateJob } from '@/model/Translator';
import { doAction } from '@/pages/util';
import { useGptWorkspaceStore } from '@/stores';

const message = useMessage();
const { t } = useI18n();

const workspace = useGptWorkspaceStore();
const workspaceRef = workspace.ref;

const showCreateWorkerModal = ref(false);
const showLocalVolumeDrawer = ref(false);

type ProcessedJob = TranslateJob & {
  progress?: { finished: number; error: number; total: number };
};

const processedJobs = ref<Map<string, ProcessedJob>>(new Map());

const getNextJob = () => {
  const job = workspace.ref.value.jobs.find(
    (it) => !processedJobs.value.has(it.task),
  );
  if (job !== undefined) {
    processedJobs.value.set(job.task, job);
  }
  return job;
};

const deleteJob = (task: string) => {
  if (processedJobs.value.has(task)) {
    message.error(t('workspace.gptWorkspace.taskLocked'));
    return;
  }
  workspace.deleteJob(task);
};
const deleteAllJobs = () => {
  workspaceRef.value.jobs.forEach((job) => {
    if (processedJobs.value.has(job.task)) {
      return;
    }
    workspace.deleteJob(job.task);
  });
};

const onProgressUpdated = (
  task: string,
  state:
    | { state: 'finish'; abort: boolean }
    | { state: 'processed'; finished: number; error: number; total: number },
) => {
  if (state.state === 'finish') {
    const job = processedJobs.value.get(task)!;
    processedJobs.value.delete(task);
    if (!state.abort) {
      workspace.addJobRecord(job);
      workspace.deleteJob(task);
    }
  } else {
    const job = processedJobs.value.get(task)!;
    job.progress = {
      finished: state.finished,
      error: state.error,
      total: state.total,
    };
  }
};

const clearCache = async () =>
  doAction(
    TranslationCacheRepo.clear('gpt-seg-cache'),
    t('workspace.gptWorkspace.clearCacheAction'),
    message,
  );
</script>

<template>
  <div class="layout-content">
    <n-h1>{{ t('workspace.gptWorkspace.title') }}</n-h1>

    <bulletin>
      <n-flex>
        <c-a to="/forum/64f3d63f794cbb1321145c07" target="_blank">
          {{ t('workspace.gptWorkspace.docs.guide') }}
        </c-a>
        /
        <n-a href="https://chat.deepseek.com" target="_blank">
          {{ t('workspace.gptWorkspace.docs.chat') }}
        </n-a>
        /
        <n-a href="https://platform.deepseek.com/usage" target="_blank">
          {{ t('workspace.gptWorkspace.docs.api') }}
        </n-a>
      </n-flex>
      <n-p>{{ t('workspace.gptWorkspace.noticeLegacy') }}</n-p>
      <n-p>{{ t('workspace.gptWorkspace.noticeLang') }}</n-p>
    </bulletin>

    <section-header :title="t('workspace.gptWorkspace.translatorSection')">
      <c-button
        :label="t('workspace.gptWorkspace.addWorker')"
        :icon="PlusOutlined"
        @action="showCreateWorkerModal = true"
      />
      <c-button-confirm
        :hint="t('workspace.gptWorkspace.clearCacheHint')"
        :label="t('workspace.gptWorkspace.clearCache')"
        :icon="DeleteOutlineOutlined"
        @action="clearCache"
      />
    </section-header>

    <n-empty
      v-if="workspaceRef.workers.length === 0"
      :description="t('workspace.gptWorkspace.noWorkers')"
    />
    <n-list>
      <vue-draggable
        v-model="workspaceRef.workers"
        :animation="150"
        handle=".drag-trigger"
      >
        <n-list-item v-for="worker of workspaceRef.workers" :key="worker.id">
          <job-worker
            :worker="{ translatorId: 'gpt', ...worker }"
            :get-next-job="getNextJob"
            @update:progress="onProgressUpdated"
          />
        </n-list-item>
      </vue-draggable>
    </n-list>

    <section-header :title="t('workspace.gptWorkspace.queueSection')">
      <c-button
        :label="t('workspace.gptWorkspace.localShelf')"
        :icon="BookOutlined"
        @action="showLocalVolumeDrawer = true"
      />
      <c-button-confirm
        :hint="t('workspace.gptWorkspace.clearQueueHint')"
        :label="t('workspace.gptWorkspace.clearQueue')"
        :icon="DeleteOutlineOutlined"
        @action="deleteAllJobs"
      />
    </section-header>
    <n-empty
      v-if="workspaceRef.jobs.length === 0"
      :description="t('workspace.gptWorkspace.emptyQueue')"
    />
    <n-list>
      <vue-draggable
        v-model="workspaceRef.jobs"
        :animation="150"
        handle=".drag-trigger"
      >
        <n-list-item v-for="job of workspaceRef.jobs" :key="job.task">
          <job-queue
            :job="job"
            :progress="processedJobs.get(job.task)?.progress"
            @top-job="workspace.topJob(job)"
            @bottom-job="workspace.bottomJob(job)"
            @delete-job="deleteJob(job.task)"
          />
        </n-list-item>
      </vue-draggable>
    </n-list>

    <job-record-section id="gpt" />
  </div>

  <local-volume-list-specific-translation
    v-model:show="showLocalVolumeDrawer"
    type="gpt"
  />

  <gpt-worker-modal v-model:show="showCreateWorkerModal" />
</template>
