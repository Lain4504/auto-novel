<script lang="ts" setup>
import { useKeyModifier } from '@vueuse/core';
import { useI18n } from 'vue-i18n';

import { useIsWideScreen } from '@/pages/util';
import {
  FavoredRepo,
  Setting,
  useLocalVolumeStore,
  useSettingStore,
} from '@/stores';
import { useBookshelfLocalStore } from '../BookshelfLocalStore';

const { t } = useI18n();

const props = defineProps<{
  selectedIds: string[];
  favoredId: string;
}>();
defineEmits<{
  selectAll: [];
  invertSelection: [];
}>();

const message = useMessage();
const isWideScreen = useIsWideScreen(600);

const settingStore = useSettingStore();
const { setting } = storeToRefs(settingStore);

const store = useBookshelfLocalStore();

// 删除小说
const showDeleteModal = ref(false);

const openDeleteModal = () => {
  const ids = props.selectedIds;
  if (ids.length === 0) {
    message.info(t('workspace.localVolume.noneSelected'));
    return;
  }
  showDeleteModal.value = true;
};

const deleteSelected = async () => {
  const ids = props.selectedIds;
  const { success, failed } = await store.deleteVolumes(ids);
  message.info(t('workspace.localVolume.deleteResult', { success, failed }));
};

// 下载小说
const showDownloadModal = ref(false);

const downloadSelected = async () => {
  const ids = props.selectedIds;
  if (ids.length === 0) {
    message.info(t('workspace.localVolume.noneSelected'));
    return;
  }
  const { success, failed } = await store.downloadVolumes(ids);
  message.info(t('workspace.localVolume.downloadResult', { success, failed }));
};

const downloadRawSelected = async () => {
  const ids = props.selectedIds;
  if (ids.length === 0) {
    message.info(t('workspace.localVolume.noneSelected'));
    return;
  }
  const { success, failed } = await store.downloadRawVolumes(ids);
  message.info(t('workspace.localVolume.downloadResult', { success, failed }));
};

// 移动小说
const favoredStore = FavoredRepo.useFavoredStore();
const { favoreds } = storeToRefs(favoredStore);

const targetFavoredId = ref(props.favoredId);

const moveToFavored = async () => {
  const novels = props.selectedIds;
  if (novels.length === 0) {
    message.info(t('workspace.localVolume.noneSelected'));
    return;
  }

  if (targetFavoredId.value === props.favoredId) {
    message.info(t('bookshelf.local.moveDisabled'));
    return;
  }

  const localVolumeRepository = await useLocalVolumeStore();

  let failed = 0;
  for (const volumeId of novels) {
    try {
      await localVolumeRepository.updateFavoredId(
        volumeId,
        targetFavoredId.value,
      );
    } catch (e) {
      failed += 1;
    }
  }
  const success = novels.length - failed;

  message.info(t('workspace.localVolume.deleteResult', { success, failed }));
  await store.loadVolumes();
};

// 生成翻译任务
const translateLevel = ref<'expire' | 'all'>('expire');
const reverseOrder = ref(false);
const shouldTopJob = useKeyModifier('Control');

const queueJobs = (type: 'gpt' | 'sakura') => {
  let ids = props.selectedIds;
  if (ids.length === 0) {
    message.info(t('workspace.localVolume.noneSelected'));
    return;
  }

  if (reverseOrder.value) {
    ids = ids.slice().reverse();
  }

  const { success, failed } = store.queueJobsToWorkspace(ids, {
    level: translateLevel.value,
    type,
    shouldTop: shouldTopJob.value ?? false,
  });
  message.info(t('workspace.specific.queueResult', { success, failed }));
};
</script>

<template>
  <n-list bordered>
    <n-list-item>
      <n-flex vertical>
        <n-flex align="baseline">
          <n-button-group size="small">
            <c-button
              :label="t('bookshelf.local.selectAll')"
              :round="false"
              @action="$emit('selectAll')"
            />
            <c-button
              :label="t('bookshelf.local.invertSelection')"
              :round="false"
              @action="$emit('invertSelection')"
            />
          </n-button-group>

          <n-button-group size="small">
            <c-button
              :label="t('bookshelf.local.downloadOriginal')"
              :round="false"
              @action="downloadRawSelected"
            />
            <c-button
              :label="t('bookshelf.local.downloadMachine')"
              :round="false"
              @action="downloadSelected"
            />
            <c-button
              :label="t('bookshelf.local.downloadSettings')"
              :round="false"
              @action="showDownloadModal = true"
            />
          </n-button-group>

          <c-button
            :label="t('bookshelf.local.delete')"
            secondary
            :round="false"
            size="small"
            type="error"
            @click="openDeleteModal"
          />
          <c-modal
            :title="selectedIds.length === 1
              ? t('bookshelf.local.deleteConfirmSingle', { name: selectedIds[0] })
              : t('bookshelf.local.deleteConfirmMultiple', { count: selectedIds.length })"
            v-model:show="showDeleteModal"
          >
            <template #action>
              <c-button :label="t('bookshelf.local.confirm')" type="primary" @action="deleteSelected" />
            </template>
          </c-modal>
        </n-flex>

        <n-text depth="3">{{ t('bookshelf.local.selected', { count: selectedIds.length }) }}</n-text>
      </n-flex>
    </n-list-item>

    <n-list-item v-if="favoreds.local.length > 1">
      <n-p>{{ t('bookshelf.local.moveDisabled') }}</n-p>
      <n-flex v-if="false" vertical>
        <b>{{ t('bookshelf.local.moveTitle') }}</b>

        <n-radio-group v-model:value="targetFavoredId">
          <n-flex align="center">
            <c-button
              :label="t('bookshelf.local.move')"
              size="small"
              :round="false"
              @action="moveToFavored"
            />

            <n-radio
              v-for="favored in favoreds.local"
              :key="favored.id"
              :value="favored.id"
            >
              {{ favored.title }}
            </n-radio>
          </n-flex>
        </n-radio-group>
      </n-flex>
    </n-list-item>

    <n-list-item
      v-if="
        setting.enabledTranslator.includes('gpt') ||
        setting.enabledTranslator.includes('sakura')
      "
    >
      <n-flex vertical>
        <b>{{ t('bookshelf.local.generateTask') }}</b>

        <c-action-wrapper :title="t('bookshelf.local.options')">
          <n-flex size="small">
            <n-tooltip trigger="hover">
              <template #trigger>
                <n-flex :size="0" :wrap="false">
                  <tag-button
                    :label="t('bookshelf.local.expire')"
                    :checked="translateLevel === 'expire'"
                    @update:checked="translateLevel = 'expire'"
                  />
                  <tag-button
                    :label="t('bookshelf.local.retranslate')"
                    type="warning"
                    :checked="translateLevel === 'all'"
                    @update:checked="translateLevel = 'all'"
                  />
                </n-flex>
              </template>
              {{ t('novel.translateOptions.expireDesc') }}
              <br />
              {{ t('novel.translateOptions.retranslateDesc') }}
              <br />
            </n-tooltip>

            <tag-button :label="t('bookshelf.local.reverseOrder')" v-model:checked="reverseOrder" />
          </n-flex>
        </c-action-wrapper>

        <c-action-wrapper :title="t('bookshelf.local.actions')">
          <n-button-group size="small">
            <c-button
              v-if="setting.enabledTranslator.includes('gpt')"
              :label="t('bookshelf.local.queueGpt')"
              :round="false"
              @action="queueJobs('gpt')"
            />
            <c-button
              v-if="setting.enabledTranslator.includes('sakura')"
              :label="t('bookshelf.local.queueSakura')"
              :round="false"
              @action="queueJobs('sakura')"
            />
          </n-button-group>
        </c-action-wrapper>
      </n-flex>
    </n-list-item>
  </n-list>

  <c-modal :title="t('bookshelf.local.downloadSettings')" v-model:show="showDownloadModal">
    <n-flex vertical size="large">
      <c-action-wrapper :title="t('bookshelf.local.language')">
        <c-radio-group
          v-model:value="setting.downloadFormat.mode"
          :options="Setting.downloadModeOptions"
        />
      </c-action-wrapper>

      <c-action-wrapper :title="t('bookshelf.local.translation')">
        <n-flex>
          <c-radio-group
            v-model:value="setting.downloadFormat.translationsMode"
            :options="Setting.downloadTranslationModeOptions"
          />
          <translator-check
            v-model:value="setting.downloadFormat.translations"
            show-order
            :two-line="!isWideScreen"
          />
        </n-flex>
      </c-action-wrapper>

      <n-text depth="3" style="font-size: 12px">
        {{ t('novel.translateOptions.epubNote') }}
      </n-text>
    </n-flex>
  </c-modal>
</template>
