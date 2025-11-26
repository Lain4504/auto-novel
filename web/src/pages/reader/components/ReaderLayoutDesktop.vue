<script lang="ts" setup>
import type { WebNovelChapterDto } from '@/model/WebNovel';
import {
  FormatListBulletedOutlined,
  LibraryBooksOutlined,
  TuneOutlined,
  ArrowUpwardOutlined,
  ArrowDownwardOutlined,
} from '@vicons/material';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

defineProps<{
  novelUrl?: string;
  chapter: WebNovelChapterDto;
}>();

const emit = defineEmits<{
  nav: [string];
  requireCatalogModal: [];
  requireSettingModal: [];
}>();

const router = useRouter();
</script>

<template>
  <n-flex :wrap="false">
    <div style="flex: auto">
      <slot />
    </div>

    <div style="flex: 0 0 0">
      <n-flex size="large" vertical style="position: fixed; bottom: 20px">
        <side-button
          :disabled="!chapter.prevId"
          :text="t('reader.prevChapter')"
          :icon="ArrowUpwardOutlined"
          @click="emit('nav', chapter.prevId!)"
        />
        <side-button
          :disabled="!chapter.nextId"
          :text="t('reader.nextChapter')"
          :icon="ArrowDownwardOutlined"
          @click="emit('nav', chapter.nextId!)"
        />
        <side-button
          v-if="novelUrl"
          :text="t('reader.details')"
          :icon="LibraryBooksOutlined"
          @click="router.push(novelUrl)"
        />
        <side-button
          :text="t('reader.catalog')"
          :icon="FormatListBulletedOutlined"
          @click="emit('requireCatalogModal')"
        />
        <side-button
          :text="t('reader.settings')"
          :icon="TuneOutlined"
          @click="emit('requireSettingModal')"
        />
      </n-flex>
    </div>
  </n-flex>
</template>
