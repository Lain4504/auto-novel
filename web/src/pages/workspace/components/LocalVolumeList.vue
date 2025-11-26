<script lang="ts" setup>
import { FileDownloadOutlined, MoreVertOutlined } from '@vicons/material';
import { useI18n } from 'vue-i18n';

import type { LocalVolumeMetadata } from '@/model/LocalVolume';
import {
  BookshelfLocalUtil,
  useBookshelfLocalStore,
} from '@/pages/bookshelf/BookshelfLocalStore';
import { FavoredRepo, Setting, useSettingStore } from '@/stores';

const props = defineProps<{
  options?: { [key: string]: (volumes: LocalVolumeMetadata[]) => void };
  filter?: (volume: LocalVolumeMetadata) => boolean;
}>();

const emit = defineEmits<{
  volumeAdd: [File];
}>();

const message = useMessage();
const { t } = useI18n();

const settingStore = useSettingStore();
const { setting } = storeToRefs(settingStore);

const store = useBookshelfLocalStore();
const { volumes } = storeToRefs(store);

store.loadVolumes();

const BULK_DELETE_KEY = 'bulk-delete';

const options = computed(() => {
  const propOptions = Object.keys(props.options ?? {}).map((key) => ({
    label: key,
    key,
  }));
  return [
    ...propOptions,
    { label: t('workspace.localVolume.bulkDelete'), key: BULK_DELETE_KEY },
  ];
});
const handleSelect = (key: string) => {
  switch (key) {
    case BULK_DELETE_KEY:
      openDeleteModal();
      break;
    default:
      props.options?.[key]?.(volumes.value ?? []);
      break;
  }
};

const downloadVolumes = async () => {
  if (sortedVolumes.value.length === 0) {
    message.info(t('workspace.localVolume.noneSelected'));
    return;
  }
  const ids = sortedVolumes.value.map((it) => it.id);
  const { success, failed } = await store.downloadVolumes(ids);
  message.info(t('workspace.localVolume.downloadResult', { success, failed }));
};

const showDeleteModal = ref(false);

const openDeleteModal = () => {
  if (sortedVolumes.value.length === 0) {
    message.info(t('workspace.localVolume.noneSelected'));
    return;
  }
  showDeleteModal.value = true;
};

const deleteAllVolumes = async () => {
  const ids = sortedVolumes.value.map((it) => it.id);
  const { success, failed } = await store.deleteVolumes(ids);
  showDeleteModal.value = false;
  message.info(t('workspace.localVolume.deleteResult', { success, failed }));
};

const search = reactive({
  query: '',
  enableRegexMode: false,
});

const favoredStore = FavoredRepo.useFavoredStore();
const { favoreds } = storeToRefs(favoredStore);

const selectedFavored = ref<string | undefined>(favoreds.value.local[0]?.id);
const favoredsOptions = computed(() => {
  return favoreds.value.local.map(({ id, title }) => ({
    label: title,
    value: id,
  }));
});

const sortedVolumes = computed(() => {
  const filteredVolumes =
    props.filter === undefined
      ? volumes.value
      : volumes.value.filter(props.filter);
  return BookshelfLocalUtil.filterAndSortVolumes(filteredVolumes, {
    ...search,
    favoredId: selectedFavored.value,
    order: setting.value.localVolumeOrder,
  });
});
</script>

<template>
  <c-drawer-right :title="t('workspace.localVolume.drawerTitle')">
    <template #action>
      <bookshelf-local-add-button
        :favored-id="selectedFavored"
        @done="emit('volumeAdd', $event)"
      />
      <c-button
        :label="t('workspace.localVolume.download')"
        :icon="FileDownloadOutlined"
        @click="downloadVolumes"
      />
      <n-dropdown
        trigger="click"
        :options="options"
        :keyboard="false"
        @select="handleSelect"
      >
        <n-button circle>
          <n-icon :component="MoreVertOutlined" />
        </n-button>
      </n-dropdown>
    </template>

    <div style="padding: 24px 16px">
      <n-flex vertical>
        <c-action-wrapper :title="t('workspace.localVolume.searchTitle')">
          <search-input
            v-model:value="search"
            :placeholder="t('workspace.localVolume.searchPlaceholder')"
            style="max-width: 400px"
          />
        </c-action-wrapper>

        <c-action-wrapper
          v-if="favoreds.local.length > 1"
          :title="t('workspace.localVolume.favoriteTitle')"
        >
          <n-select
            v-model:value="selectedFavored"
            :options="favoredsOptions"
            style="max-width: 400px"
          />
        </c-action-wrapper>

        <c-action-wrapper
          :title="t('workspace.localVolume.sortTitle')"
          align="center"
        >
          <order-sort
            v-model:value="setting.localVolumeOrder"
            :options="Setting.localVolumeOrderOptions"
          />
        </c-action-wrapper>
        <slot name="extra" />
      </n-flex>

      <n-divider style="margin: 16px 0 8px" />

      <n-spin v-if="sortedVolumes === undefined" style="margin-top: 20px" />

      <n-empty
        v-else-if="sortedVolumes.length === 0"
        :description="t('workspace.localVolume.empty')"
        style="margin-top: 20px"
      />

      <n-scrollbar v-else trigger="none" :size="24" style="flex: auto">
        <n-list style="padding-bottom: 48px; padding-right: 12px">
          <n-list-item v-for="volume of sortedVolumes ?? []" :key="volume.id">
            <slot name="volume" v-bind="volume" />
          </n-list-item>
        </n-list>
      </n-scrollbar>

      <c-modal
        :title="t('workspace.localVolume.modalTitle')"
        v-model:show="showDeleteModal"
      >
        <n-p>{{ t('workspace.localVolume.modalWarning') }}</n-p>

        <template #action>
          <c-button
            :label="t('workspace.localVolume.modalConfirm')"
            type="primary"
            @action="deleteAllVolumes"
          />
        </template>
      </c-modal>
    </div>
  </c-drawer-right>
</template>
