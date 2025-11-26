<script lang="ts" setup>
import { useI18n } from 'vue-i18n';

import { GenericNovelId } from '@/model/Common';
import type { LocalVolumeMetadata } from '@/model/LocalVolume';
import type { TranslatorId } from '@/model/Translator';
import { useSettingStore } from '@/stores';

const { t } = useI18n();

const props = defineProps<{
  volume: LocalVolumeMetadata;
}>();

const settingStore = useSettingStore();
const { setting } = storeToRefs(settingStore);

const calculateFinished = (translatorId: TranslatorId) =>
  props.volume.toc.filter((it) => it[translatorId]).length;

const baidu = ref(calculateFinished('baidu'));
const youdao = ref(calculateFinished('youdao'));
const gpt = ref(calculateFinished('gpt'));
const sakura = ref(calculateFinished('sakura'));

const translateTask = useTemplateRef('translateTask');
const startTranslateTask = (translatorId: 'baidu' | 'youdao') =>
  translateTask?.value?.startTask(
    { type: 'local', volumeId: props.volume.id },
    {
      level: 'expire',
      forceMetadata: false,
      startIndex: 0,
      endIndex: 65535,
    },
    { id: translatorId },
  );
</script>

<template>
  <n-flex :size="4" vertical>
    <c-a
      v-if="!volume.id.endsWith('.epub')"
      :to="`/workspace/reader/${encodeURIComponent(volume.id)}/0`"
    >
      {{ volume.id }}
    </c-a>
    <n-text v-else>{{ volume.id }}</n-text>

    <n-text depth="3">
      {{ t('bookshelf.local.stats', { total: volume.toc.length, baidu, youdao, gpt, sakura }) }}
    </n-text>

    <n-flex :size="8">
      <c-button
        v-if="setting.enabledTranslator.includes('baidu')"
        :label="t('bookshelf.local.updateBaidu')"
        size="tiny"
        secondary
        @action="startTranslateTask('baidu')"
      />
      <c-button
        v-if="setting.enabledTranslator.includes('youdao')"
        :label="t('bookshelf.local.updateYoudao')"
        size="tiny"
        secondary
        @action="startTranslateTask('youdao')"
      />
      <glossary-button
        :gnid="GenericNovelId.local(volume.id)"
        :value="volume.glossary"
        size="tiny"
        secondary
      />
    </n-flex>
  </n-flex>
  <TranslateTask
    ref="translateTask"
    style="margin-top: 20px"
    @update:baidu="(zh) => (baidu = zh)"
    @update:youdao="(zh) => (youdao = zh)"
  />
</template>
