<script lang="ts" setup>
import { useKeyModifier } from '@vueuse/core';
import { useI18n } from 'vue-i18n';

import { TranslateTaskDescriptor } from '@/model/Translator';
import type { WebNovelOutlineDto } from '@/model/WebNovel';
import { FavoredRepo, useSettingStore, useWorkspaceStore } from '@/stores';

const { t } = useI18n();

const props = defineProps<{
  selectedNovels: WebNovelOutlineDto[];
  favoredId: string;
}>();
defineEmits<{
  selectAll: [];
  invertSelection: [];
}>();

const message = useMessage();

const settingStore = useSettingStore();
const { setting } = storeToRefs(settingStore);

const favoredStore = FavoredRepo.useFavoredStore();
const { favoreds } = storeToRefs(favoredStore);

// 删除小说
const showDeleteModal = ref(false);

const openDeleteModal = () => {
  const novels = props.selectedNovels;
  if (novels.length === 0) {
    message.info(t('workspace.localVolume.noneSelected'));
    return;
  }
  showDeleteModal.value = true;
};

const deleteSelected = async () => {
  const novels = props.selectedNovels;
  let failed = 0;
  for (const { providerId, novelId } of novels) {
    try {
      await FavoredRepo.unfavoriteNovel(props.favoredId, {
        type: 'web',
        providerId,
        novelId,
      });
    } catch (e) {
      failed += 1;
    }
  }
  const success = novels.length - failed;

  message.info(t('workspace.localVolume.deleteResult', { success, failed }));
};

// 移动小说
const targetFavoredId = ref(props.favoredId);

const moveToFavored = async () => {
  const novels = props.selectedNovels;
  if (novels.length === 0) {
    message.info(t('workspace.localVolume.noneSelected'));
    return;
  }

  if (targetFavoredId.value === props.favoredId) {
    message.info(t('bookshelf.web.moveDisabled'));
    return;
  }

  let failed = 0;
  for (const { providerId, novelId } of novels) {
    try {
      await FavoredRepo.unfavoriteNovel(targetFavoredId.value, {
        type: 'web',

        providerId,
        novelId,
      });
    } catch (e) {
      failed += 1;
    }
  }
  const success = novels.length - failed;

  message.info(t('workspace.localVolume.deleteResult', { success, failed }));
  window.location.reload();
};

// 生成翻译任务
const translateLevel = ref<'normal' | 'expire' | 'all'>('normal');
const forceMetadata = ref(false);
const first5 = ref(false);
const reverseOrder = ref(false);
const shouldTopJob = useKeyModifier('Control');

const queueJobs = (type: 'gpt' | 'sakura') => {
  let novels = props.selectedNovels;
  if (novels.length === 0) {
    message.info(t('workspace.localVolume.noneSelected'));
    return;
  }

  const workspace = useWorkspaceStore(type);

  if (reverseOrder.value) {
    novels = novels.slice().reverse();
  }

  let failed = 0;
  novels.forEach(({ providerId, novelId, titleJp }) => {
    const task = TranslateTaskDescriptor.web(providerId, novelId, {
      level: translateLevel.value,
      forceMetadata: forceMetadata.value,
      startIndex: 0,
      endIndex: first5.value ? 5 : 65535,
    });
    const job = {
      task,
      description: titleJp,
      createAt: Date.now(),
    };
    const success = workspace.addJob(job);
    if (success && shouldTopJob.value) {
      workspace.topJob(job);
    }
    if (!success) {
      failed += 1;
    }
  });
  const success = novels.length - failed;
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
              :label="t('bookshelf.web.selectAll')"
              :round="false"
              @action="$emit('selectAll')"
            />
            <c-button
              :label="t('bookshelf.web.invertSelection')"
              :round="false"
              @action="$emit('invertSelection')"
            />
          </n-button-group>

          <c-button
            :label="t('bookshelf.web.delete')"
            secondary
            :round="false"
            size="small"
            type="error"
            @click="openDeleteModal"
          />
          <c-modal
            :title="selectedNovels.length === 1
              ? t('bookshelf.web.deleteConfirmSingle', { name: selectedNovels[0].titleVi ?? selectedNovels[0].titleJp })
              : t('bookshelf.web.deleteConfirmMultiple', { count: selectedNovels.length })"
            v-model:show="showDeleteModal"
          >
            <template #action>
              <c-button :label="t('bookshelf.web.confirm')" type="primary" @action="deleteSelected" />
            </template>
          </c-modal>
        </n-flex>

        <n-text depth="3">{{ t('bookshelf.web.selected', { count: selectedNovels.length }) }}</n-text>
      </n-flex>
    </n-list-item>

    <n-list-item v-if="favoreds.web.length > 1">
      <n-p>{{ t('bookshelf.web.moveDisabled') }}</n-p>
      <n-flex v-if="false" vertical>
        <b>{{ t('bookshelf.web.moveTitleLowEnd') }}</b>

        <n-radio-group v-model:value="targetFavoredId">
          <n-flex align="center">
            <c-button
              :label="t('bookshelf.web.move')"
              size="small"
              :round="false"
              @action="moveToFavored"
            />

            <n-radio
              v-for="favored in favoreds.web"
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
        <b>{{ t('bookshelf.web.generateTask') }}</b>

        <n-flex size="small">
          <n-tooltip trigger="hover">
            <template #trigger>
              <n-flex :size="0" :wrap="false">
                <tag-button
                  :label="t('bookshelf.web.normal')"
                  :checked="translateLevel === 'normal'"
                  @update:checked="translateLevel = 'normal'"
                />
                <tag-button
                  :label="t('bookshelf.web.expire')"
                  :checked="translateLevel === 'expire'"
                  @update:checked="translateLevel = 'expire'"
                />
                <tag-button
                  :label="t('bookshelf.web.retranslate')"
                  type="warning"
                  :checked="translateLevel === 'all'"
                  @update:checked="translateLevel = 'all'"
                />
              </n-flex>
            </template>
            {{ t('novel.translateOptions.normalDesc') }}
            <br />
            {{ t('novel.translateOptions.expireDesc') }}
            <br />
            {{ t('novel.translateOptions.retranslateDesc') }}
            <br />
          </n-tooltip>

          <tag-button :label="t('bookshelf.web.retranslateMetadata')" v-model:checked="forceMetadata" />
          <tag-button :label="t('bookshelf.web.first5')" v-model:checked="first5" />
          <tag-button :label="t('bookshelf.web.reverseOrder')" v-model:checked="reverseOrder" />

          <n-text
            v-if="translateLevel === 'all'"
            type="warning"
            style="font-size: 12px; flex-basis: 100%"
          >
            <b>{{ t('novel.translateOptions.warning') }}</b>
          </n-text>
        </n-flex>

        <n-button-group size="small">
          <c-button
            v-if="setting.enabledTranslator.includes('gpt')"
            :label="t('bookshelf.web.queueGpt')"
            :round="false"
            @action="queueJobs('gpt')"
          />
          <c-button
            v-if="setting.enabledTranslator.includes('sakura')"
            :label="t('bookshelf.web.queueSakura')"
            :round="false"
            @action="queueJobs('sakura')"
          />
        </n-button-group>
      </n-flex>
    </n-list-item>
  </n-list>
</template>
