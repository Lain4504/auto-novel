<script lang="ts" setup>
import {
  BookOutlined,
  DeleteOutlineOutlined,
  PlusOutlined,
} from '@vicons/material';
import { VueDraggable } from 'vue-draggable-plus';
import { useI18n } from 'vue-i18n';

import { SakuraTranslator } from '@/domain/translate';
import { TranslationCacheRepo } from '@/repos';
import type { TranslateJob } from '@/model/Translator';
import { doAction } from '@/pages/util';
import SoundAllTaskCompleted from '@/sound/all_task_completed.mp3';
import { useSakuraWorkspaceStore, useSettingStore } from '@/stores';

const message = useMessage();
const { t } = useI18n();

const settingStore = useSettingStore();
const { setting } = storeToRefs(settingStore);

const workspace = useSakuraWorkspaceStore();
const workspaceRef = workspace.ref;

const showCreateWorkerModal = ref(false);
const showLocalVolumeDrawer = ref(false);

type ProcessedJob = TranslateJob & {
  progress?: { finished: number; error: number; total: number };
};

const processedJobs = ref<Map<string, ProcessedJob>>(new Map());

const getNextJob = () => {
  const job = workspaceRef.value.jobs.find(
    (it) => !processedJobs.value.has(it.task),
  );
  if (job !== undefined) {
    processedJobs.value.set(job.task, job);
  } else if (processedJobs.value.size === 0 && setting.value.workspaceSound) {
    // 全部任务都已经完成
    new Audio(SoundAllTaskCompleted).play();
  }
  return job;
};

const deleteJob = (task: string) => {
  if (processedJobs.value.has(task)) {
    message.error(t('workspace.sakuraWorkspace.taskLocked'));
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
      job.finishAt = Date.now();
      workspace.addJobRecord(job as TranslateJob);
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
    TranslationCacheRepo.clear('sakura-seg-cache'),
    t('workspace.sakuraWorkspace.clearCacheAction'),
    message,
  );
</script>

<template>
  <div class="layout-content">
    <n-h1>{{ t('workspace.sakuraWorkspace.title') }}</n-h1>

    <bulletin>
      <n-flex>
        <c-a to="/forum/656d60530286f15e3384fcf8" target="_blank">
          {{ t('workspace.sakuraWorkspace.docs.local') }}
        </c-a>
        /
        <span>
          <c-a to="/forum/65719bf16843e12bd3a4dc98" target="_blank">
            {{ t('workspace.sakuraWorkspace.docs.autodl') }}
          </c-a>
          :
          <n-a
            href="https://www.autodl.com/console/instance/list"
            target="_blank"
          >
            {{ t('workspace.sakuraWorkspace.docs.console') }}
          </n-a>
        </span>
        /
        <n-a
          href="https://monitor.novelia.cc/public-dashboards/be71c46fcc0e40eeaf06d9e7a2e26f95?refresh=auto&from=now-5m&to=now&timezone=browser"
          target="_blank"
        >
          {{ t('workspace.sakuraWorkspace.docs.monitor') }}
        </n-a>
      </n-flex>

      <n-p>{{ t('workspace.sakuraWorkspace.uploadNotice') }}</n-p>
      <n-p>{{ t('workspace.sakuraWorkspace.allowModelsTitle') }}</n-p>
      <n-ul>
        <n-li
          v-for="({ repo }, model) in SakuraTranslator.allowModels"
          :key="model"
        >
          [
          <n-a
            target="_blank"
            :href="`https://huggingface.co/${repo}/blob/main/${model}.gguf`"
          >
            HF
          </n-a>
          /
          <n-a
            target="_blank"
            :href="`https://hf-mirror.com/${repo}/blob/main/${model}.gguf`"
          >
            国内镜像
          </n-a>
          ]
          {{ model }}
        </n-li>
      </n-ul>
    </bulletin>

    <section-header :title="t('workspace.sakuraWorkspace.translatorSection')">
      <c-button
        :label="t('workspace.sakuraWorkspace.addWorker')"
        :icon="PlusOutlined"
        @action="showCreateWorkerModal = true"
      />
      <c-button-confirm
        :hint="t('workspace.sakuraWorkspace.clearCacheHint')"
        :label="t('workspace.sakuraWorkspace.clearCache')"
        :icon="DeleteOutlineOutlined"
        @action="clearCache"
      />
    </section-header>

    <n-empty
      v-if="workspaceRef.workers.length === 0"
      :description="t('workspace.sakuraWorkspace.noWorkers')"
    />
    <n-list>
      <vue-draggable
        v-model="workspaceRef.workers"
        :animation="150"
        handle=".drag-trigger"
      >
        <n-list-item v-for="worker of workspaceRef.workers" :key="worker.id">
          <job-worker
            :worker="{ translatorId: 'sakura', ...worker }"
            :get-next-job="getNextJob"
            @update:progress="onProgressUpdated"
          />
        </n-list-item>
      </vue-draggable>
    </n-list>

    <section-header :title="t('workspace.sakuraWorkspace.queueSection')">
      <c-button
        :label="t('workspace.sakuraWorkspace.localShelf')"
        :icon="BookOutlined"
        @action="showLocalVolumeDrawer = true"
      />
      <c-button-confirm
        :hint="t('workspace.sakuraWorkspace.clearQueueHint')"
        :label="t('workspace.sakuraWorkspace.clearQueue')"
        :icon="DeleteOutlineOutlined"
        @action="deleteAllJobs"
      />
    </section-header>
    <n-empty
      v-if="workspaceRef.jobs.length === 0"
      :description="t('workspace.sakuraWorkspace.emptyQueue')"
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

    <job-record-section id="sakura" />
  </div>

  <local-volume-list-specific-translation
    v-model:show="showLocalVolumeDrawer"
    type="sakura"
  />

  <sakura-worker-modal v-model:show="showCreateWorkerModal" />
</template>
