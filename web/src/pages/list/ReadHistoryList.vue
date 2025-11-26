<script lang="ts" setup>
import { DeleteOutlineOutlined } from '@vicons/material';
import { useI18n } from 'vue-i18n';

import { ReadHistoryApi } from '@/api';
import { WebNovelRepo } from '@/repos';
import router from '@/router';
import { useReadHistoryStore } from '@/stores';
import { doAction } from '../util';

const { t } = useI18n();

const route = useRoute();

const onUpdatePage = (page: number) => {
  const query = { ...route.query, page };
  router.push({ path: route.path, query });
};

const props = defineProps<{
  page: number;
}>();

const message = useMessage();

const readHistoryStore = useReadHistoryStore();
const { readHistoryPaused } = storeToRefs(readHistoryStore);

onMounted(() => {
  readHistoryStore.loadReadHistoryPausedState();
});

const { data: novelPage, error } = WebNovelRepo.useWebNovelHistoryList(
  () => props.page,
);

const clearHistory = () =>
  doAction(
    ReadHistoryApi.clearReadHistoryWeb().then(() => {
      window.location.reload();
    }),
    t('list.readHistory.clear'),
    message,
  );

const deleteHistory = (providerId: string, novelId: string) =>
  doAction(
    ReadHistoryApi.deleteReadHistoryWeb(providerId, novelId).then(() => {
      window.location.reload();
    }),
    t('list.readHistory.delete'),
    message,
  );
</script>

<template>
  <div class="layout-content">
    <n-h1>{{ t('list.readHistory.title') }}</n-h1>

    <n-flex style="margin-bottom: 24px">
      <c-button-confirm
        :hint="t('list.readHistory.clearConfirm')"
        :label="t('list.readHistory.clear')"
        :icon="DeleteOutlineOutlined"
        @action="clearHistory()"
      />
      <c-button
        v-if="readHistoryPaused"
        :label="t('list.readHistory.continueRecording')"
        @action="readHistoryStore.resumeReadHistory()"
      />
      <c-button
        v-else
        :label="t('list.readHistory.pauseRecording')"
        @action="readHistoryStore.pauseReadHistory()"
      />
    </n-flex>

    <n-text v-if="readHistoryPaused" type="warning">
      {{ t('list.readHistory.pausedWarning') }}
    </n-text>

    <CPage
      :page="page"
      :page-number="novelPage?.pageNumber"
      @update:page="onUpdatePage"
    >
      <template v-if="novelPage">
        <n-divider />
        <NovelListWeb :items="novelPage.items" :option="[]" simple>
          <template #action="item">
            <c-button
              size="tiny"
              :label="t('list.readHistory.delete')"
              style="margin-top: 2px"
              @action="deleteHistory(item.providerId, item.novelId)"
            />
          </template>
        </NovelListWeb>
        <n-empty v-if="novelPage.items.length === 0" :description="t('list.readHistory.empty')" />
        <n-divider />
      </template>

      <CResultX v-else :error="error" :title="t('list.readHistory.loadError')" />
    </CPage>
  </div>
</template>
