<script lang="ts" setup>
import { useI18n } from 'vue-i18n';

import type { WenkuNovelOutlineDto } from '@/model/WenkuNovel';
import { FavoredRepo } from '@/stores';

const { t } = useI18n();

const props = defineProps<{
  selectedNovels: WenkuNovelOutlineDto[];
  favoredId: string;
}>();
defineEmits<{
  selectAll: [];
  invertSelection: [];
}>();

const message = useMessage();

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
  for (const { id } of novels) {
    try {
      await FavoredRepo.unfavoriteNovel(props.favoredId, {
        type: 'wenku',
        novelId: id,
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
    message.info(t('bookshelf.wenku.moveDisabled'));
    return;
  }

  let failed = 0;
  for (const { id } of novels) {
    try {
      await FavoredRepo.favoriteNovel(props.favoredId, {
        type: 'wenku',
        novelId: id,
      });
    } catch (e) {
      failed += 1;
    }
  }
  const success = novels.length - failed;

  message.info(t('workspace.localVolume.deleteResult', { success, failed }));
  window.location.reload();
};
</script>

<template>
  <n-list bordered>
    <n-list-item>
      <n-flex vertical>
        <n-flex align="baseline">
          <n-button-group size="small">
            <c-button
              :label="t('bookshelf.wenku.selectAll')"
              :round="false"
              @action="$emit('selectAll')"
            />
            <c-button
              :label="t('bookshelf.wenku.invertSelection')"
              :round="false"
              @action="$emit('invertSelection')"
            />
          </n-button-group>

          <c-button
            :label="t('bookshelf.wenku.delete')"
            secondary
            :round="false"
            size="small"
            type="error"
            @click="openDeleteModal"
          />
          <c-modal
            :title="selectedNovels.length === 1
              ? t('bookshelf.wenku.deleteConfirmSingle', { name: selectedNovels[0].titleZh ?? selectedNovels[0].title })
              : t('bookshelf.wenku.deleteConfirmMultiple', { count: selectedNovels.length })"
            v-model:show="showDeleteModal"
          >
            <template #action>
              <c-button :label="t('bookshelf.wenku.confirm')" type="primary" @action="deleteSelected" />
            </template>
          </c-modal>
        </n-flex>
        <n-text depth="3">{{ t('bookshelf.wenku.selected', { count: selectedNovels.length }) }}</n-text>
      </n-flex>
    </n-list-item>

    <n-list-item v-if="favoreds.wenku.length > 1">
      <n-p>{{ t('bookshelf.wenku.moveDisabled') }}</n-p>
      <n-flex v-if="false" vertical>
        <b>{{ t('bookshelf.wenku.moveTitle') }}</b>

        <n-radio-group v-model:value="targetFavoredId">
          <n-flex align="center">
            <c-button
              :label="t('bookshelf.wenku.move')"
              size="small"
              :round="false"
              @action="moveToFavored"
            />

            <n-radio
              v-for="favored in favoreds.wenku"
              :key="favored.id"
              :value="favored.id"
            >
              {{ favored.title }}
            </n-radio>
          </n-flex>
        </n-radio-group>
      </n-flex>
    </n-list-item>
  </n-list>
</template>
