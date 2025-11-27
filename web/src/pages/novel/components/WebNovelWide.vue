<script lang="ts" setup>
import {
  KeyboardArrowDownRound,
  KeyboardArrowUpRound,
  SortOutlined,
} from '@vicons/material';
import { NScrollbar } from 'naive-ui';
import { useI18n } from 'vue-i18n';

import type { WebNovelDto, WebNovelTocItemDto } from '@/model/WebNovel';
import { useSettingStore } from '@/stores';
import { useTocExpansion } from './UseTocExpansion';
import { useLastReadChapter, useToc } from './UseWebNovel';

const { t } = useI18n();

const props = defineProps<{
  providerId: string;
  novelId: string;
  novel: WebNovelDto;
}>();

const settingStore = useSettingStore();
const { setting } = storeToRefs(settingStore);

const sortReverse = computed(() => setting.value.tocSortReverse);

const { toc } = useToc(props.novel);
const { lastReadChapter } = useLastReadChapter(props.novel, toc);

const defaultTocExpanded = computed(() => setting.value.tocExpandAll);

const { expandedNames, hasSeparators, isAnyExpanded, toggleAll, tocSections } =
  useTocExpansion(
    toc,
    defaultTocExpanded,
    computed(() => props.novel.lastReadChapterId),
  );
</script>

<template>
  <c-layout sidebar :sidebar-width="320">
    <web-novel-metadata
      :provider-id="providerId"
      :novel-id="novelId"
      :novel="novel"
    />

    <n-divider />

    <web-translate
      :provider-id="providerId"
      :novel-id="novelId"
      :title-jp="novel.titleJp"
      :title-vi="novel.titleVi"
      :total="novel.toc.filter((it: WebNovelTocItemDto) => it.chapterId).length"
      v-model:jp="novel.jp"
      v-model:baidu="novel.baidu"
      v-model:youdao="novel.youdao"
      v-model:gpt="novel.gpt"
      :sakura="novel.sakura"
      :glossary="novel.glossary"
    />

    <comment-list
      v-if="!setting.hideCommmentWebNovel"
      :site="`web-${providerId}-${novelId}`"
      :locked="false"
    />

    <template #sidebar>
      <section-header :title="t('novel.webNovelWide.catalog')">
        <c-button
          v-if="hasSeparators"
          :label="isAnyExpanded ? t('novel.catalog.collapse') : t('novel.catalog.expand')"
          :icon="isAnyExpanded ? KeyboardArrowUpRound : KeyboardArrowDownRound"
          quaternary
          size="small"
          :round="false"
          @action="toggleAll"
          style="margin-right: 8px"
        />
        <c-button
          :label="setting.tocSortReverse ? t('novel.catalog.reverseOrder') : t('novel.catalog.normalOrder')"
          :icon="SortOutlined"
          quaternary
          size="small"
          :round="false"
          @action="setting.tocSortReverse = !setting.tocSortReverse"
        />
      </section-header>

      <n-scrollbar style="flex: 1; min-height: 0; padding: 0 16px 0 0">
        <ChapterTocList
          :toc-sections="tocSections"
          v-model:expanded-names="expandedNames"
          :last-read-chapter-id="novel.lastReadChapterId"
          :default-scroll-key="lastReadChapter?.key"
          :provider-id="providerId"
          :novel-id="novelId"
          :sort-reverse="sortReverse"
          :mode="{
            narrow: false,
            modal: false,
            collapse: false,
          }"
        />
      </n-scrollbar>
    </template>
  </c-layout>
</template>
