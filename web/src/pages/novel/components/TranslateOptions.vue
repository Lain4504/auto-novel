<script lang="ts" setup>
import { InfoOutlined } from '@vicons/material';
import { useI18n } from 'vue-i18n';

import type { GenericNovelId } from '@/model/Common';
import type { Glossary } from '@/model/Glossary';
import type { TranslateTaskParams } from '@/model/Translator';
import { useIsWideScreen } from '@/pages/util';
import { Setting, useSettingStore } from '@/stores';

const { t } = useI18n();

const probs = defineProps<{
  gnid: GenericNovelId;
  glossary: Glossary;
}>();
const isWideScreen = useIsWideScreen(600);

const settingStore = useSettingStore();
const { setting } = storeToRefs(settingStore);

// 翻译设置
const translateLevel = ref<'normal' | 'expire' | 'all' | 'sync'>(
  probs.gnid.type === 'local' ? 'expire' : 'normal',
);
const forceMetadata = ref(false);
const startIndex = ref<number | null>(0);
const endIndex = ref<number | null>(65536);
const taskNumber = ref<number | null>(1);

defineExpose({
  getTranslateTaskParams: (): TranslateTaskParams => ({
    level: translateLevel.value,
    forceMetadata: forceMetadata.value,
    startIndex: startIndex.value ?? 0,
    endIndex: endIndex.value ?? 65536,
  }),
  getTaskNumber: () => taskNumber.value ?? 1,
});

const showDownloadModal = ref(false);
</script>

<template>
  <n-flex vertical>
    <c-action-wrapper :title="t('novel.translateOptions.title')">
      <n-flex size="small">
        <n-tooltip trigger="hover" style="max-width: 200px">
          <template #trigger>
            <n-flex :size="0" :wrap="false">
              <tag-button
                :label="t('novel.translateOptions.normal')"
                :checked="translateLevel === 'normal'"
                @update:checked="translateLevel = 'normal'"
              />
              <tag-button
                :label="t('novel.translateOptions.expire')"
                :checked="translateLevel === 'expire'"
                @update:checked="translateLevel = 'expire'"
              />
              <tag-button
                :label="t('novel.translateOptions.retranslate')"
                type="warning"
                :checked="translateLevel === 'all'"
                @update:checked="translateLevel = 'all'"
              />
              <tag-button
                v-if="gnid.type === 'web'"
                :label="t('novel.translateOptions.sync')"
                type="warning"
                :checked="translateLevel === 'sync'"
                @update:checked="translateLevel = 'sync'"
              />
            </n-flex>
          </template>
          {{ t('novel.translateOptions.normalDesc') }}
          <br />
          {{ t('novel.translateOptions.expireDesc') }}
          <br />
          {{ t('novel.translateOptions.retranslateDesc') }}
          <br />
          <template v-if="gnid.type === 'web'">
            {{ t('novel.translateOptions.syncDesc') }}
          </template>
        </n-tooltip>

        <tag-button
          v-if="gnid.type === 'web'"
          :label="t('novel.translateOptions.retranslateMetadata')"
          v-model:checked="forceMetadata"
        />

        <n-text
          v-if="translateLevel === 'all' || translateLevel === 'sync'"
          type="warning"
          style="font-size: 12px; flex-basis: 100%"
        >
          <b>{{ t('novel.translateOptions.warning') }}</b>
        </n-text>
      </n-flex>
    </c-action-wrapper>

    <c-action-wrapper
      v-if="gnid.type === 'web' || gnid.type === 'local'"
      :title="t('novel.translateOptions.range')"
    >
      <n-flex style="text-align: center">
        <div>
          <n-input-group>
            <n-input-group-label size="small">{{ t('novel.translateOptions.from') }}</n-input-group-label>
            <n-input-number
              size="small"
              v-model:value="startIndex"
              :show-button="false"
              button-placement="both"
              :min="0"
              style="width: 60px"
            />
            <n-input-group-label size="small">{{ t('novel.translateOptions.to') }}</n-input-group-label>
            <n-input-number
              size="small"
              v-model:value="endIndex"
              :show-button="false"
              :min="0"
              style="width: 60px"
            />
          </n-input-group>
        </div>
        <div>
          <n-input-group>
            <n-input-group-label size="small">{{ t('novel.translateOptions.split') }}</n-input-group-label>
            <n-input-number
              size="small"
              v-model:value="taskNumber"
              :show-button="false"
              :min="1"
              :max="gnid.type === 'local' ? 65536 : 10"
              style="width: 40px"
            />
            <n-input-group-label size="small">{{ t('novel.translateOptions.tasks') }}</n-input-group-label>
          </n-input-group>
        </div>

        <n-tooltip trigger="hover" placement="top" style="max-width: 200px">
          <template #trigger>
            <n-button text>
              <n-icon depth="4" :component="InfoOutlined" />
            </n-button>
          </template>
          {{ t('novel.translateOptions.rangeHint') }}
        </n-tooltip>
      </n-flex>
    </c-action-wrapper>

    <c-action-wrapper v-if="gnid.type !== 'local'" :title="t('novel.translateOptions.actions')">
      <n-button-group size="small">
        <c-button
          :label="t('novel.translateOptions.downloadSettings')"
          :round="false"
          @action="showDownloadModal = true"
        />
        <glossary-button :gnid="gnid" :value="glossary" :round="false" />
      </n-button-group>
    </c-action-wrapper>

    <c-modal :title="t('novel.translateOptions.downloadSettingsTitle')" v-model:show="showDownloadModal">
      <n-flex vertical size="large">
        <c-action-wrapper :title="t('novel.translateOptions.language')">
          <c-radio-group
            v-model:value="setting.downloadFormat.mode"
            :options="Setting.downloadModeOptions"
          />
        </c-action-wrapper>

        <c-action-wrapper :title="t('novel.translateOptions.translation')">
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

        <c-action-wrapper v-if="gnid.type === 'web'" :title="t('novel.translateOptions.file')">
          <c-radio-group
            v-model:value="setting.downloadFormat.type"
            :options="Setting.downloadTypeOptions"
          />
        </c-action-wrapper>

        <c-action-wrapper
          v-if="gnid.type === 'web'"
          :title="t('novel.translateOptions.chineseFilename')"
          align="center"
        >
          <n-switch
            size="small"
            :value="setting.downloadFilenameType === 'zh'"
            @update-value="
              (it: boolean) => (setting.downloadFilenameType = it ? 'zh' : 'jp')
            "
          />
        </c-action-wrapper>

        <n-text depth="3" style="font-size: 12px">
          {{ t('novel.translateOptions.epubNote') }}
        </n-text>
      </n-flex>
    </c-modal>
  </n-flex>
</template>
