<script lang="ts" setup>
import { DeleteOutlineOutlined } from '@vicons/material';
import { useI18n } from 'vue-i18n';

import { WebNovelApi, WenkuNovelApi } from '@/api';
import { GenericNovelId } from '@/model/Common';
import { Glossary } from '@/model/Glossary';
import { copyToClipBoard, doAction } from '@/pages/util';
import { useLocalVolumeStore, useWhoamiStore } from '@/stores';
import { downloadFile } from '@/util';

const props = defineProps<{
  gnid?: GenericNovelId;
  value: Glossary;
}>();

const message = useMessage();
const { t } = useI18n();

const whoamiStore = useWhoamiStore();
const { whoami } = storeToRefs(whoamiStore);

const glossary = ref<Glossary>({});
const glossaryLabel = computed(() =>
  t('components.glossaryButton.label', {
    count: Object.keys(props.value).length,
  }),
);
const wordPlaceholders = computed(() => [
  t('components.glossaryButton.placeholders.source'),
  t('components.glossaryButton.placeholders.target'),
]);

const showGlossaryModal = ref(false);

const toggleGlossaryModal = () => {
  if (showGlossaryModal.value === false) {
    glossary.value = { ...props.value };
  }
  showGlossaryModal.value = !showGlossaryModal.value;
};

const gnidHint = computed(() => {
  const gnid = props.gnid;
  if (gnid === undefined) {
    return undefined;
  } else {
    return GenericNovelId.toString(gnid);
  }
});

const updateGlossary = async () => {
  const gnid = props.gnid;
  if (gnid === undefined) {
    return;
  }
  const glossaryValue = toRaw(glossary.value);
  if (gnid.type === 'web') {
    await WebNovelApi.updateGlossary(
      gnid.providerId,
      gnid.novelId,
      glossaryValue,
    );
  } else if (gnid.type === 'wenku') {
    await WenkuNovelApi.updateGlossary(gnid.novelId, glossaryValue);
  } else {
    const repo = await useLocalVolumeStore();
    await repo.updateGlossary(gnid.volumeId, glossaryValue);
  }
};

const submitGlossary = () =>
  doAction(
    updateGlossary().then(() => {
      // 触发组件外的术语表本体更新。有点傻，但够用。
      for (const key in props.value) {
        delete props.value[key];
      }
      for (const key in glossary.value) {
        props.value[key] = glossary.value[key];
      }
    }),
    t('components.glossaryButton.messages.submitAction'),
    message,
  );

const importGlossaryRaw = ref('');
const termsToAdd = ref<[string, string]>(['', '']);

const deletedTerms = ref<[string, string][]>([]);

const lastDeletedTerm = computed(() => {
  const last = deletedTerms.value[deletedTerms.value.length - 1];
  if (last === undefined) return undefined;
  return `${last[0]} => ${last[1]}`;
});

const clearTerm = () => {
  glossary.value = {};
};

const undoDeleteTerm = () => {
  if (deletedTerms.value.length === 0) return;
  const [jp, zh] = deletedTerms.value.pop()!;
  glossary.value[jp] = zh;
};

const deleteTerm = (jp: string) => {
  if (jp in glossary.value) {
    deletedTerms.value.push([jp, glossary.value[jp]]);
    delete glossary.value[jp];
  }
};

const addTerm = () => {
  const [jp, zh] = termsToAdd.value;
  if (jp && zh) {
    glossary.value[jp.trim()] = zh.trim();
    termsToAdd.value = ['', ''];
  }
};

const exportGlossary = async (ev: MouseEvent) => {
  const isSuccess = await copyToClipBoard(
    Glossary.toText(glossary.value),
    ev.target as HTMLElement,
  );
  if (isSuccess) {
    message.success(t('components.glossaryButton.messages.exportSuccess'));
  } else {
    message.success(t('components.glossaryButton.messages.exportFailed'));
  }
};

const importGlossary = () => {
  const importedGlossary = Glossary.fromText(importGlossaryRaw.value);
  if (importedGlossary === undefined) {
    message.error(t('components.glossaryButton.messages.importFailed'));
  } else {
    message.success(t('components.glossaryButton.messages.importSuccess'));
    for (const jp in importedGlossary) {
      const zh = importedGlossary[jp];
      glossary.value[jp] = zh;
    }
  }
};

const downloadGlossaryAsJsonFile = async (ev: MouseEvent) => {
  downloadFile(
    `${gnidHint.value ?? 'glossary'}.json`,
    new Blob([Glossary.toJson(glossary.value)], {
      type: 'text/plain',
    }),
  );
};
</script>

<template>
  <c-button
    :label="glossaryLabel"
    v-bind="$attrs"
    @action="toggleGlossaryModal()"
  />

  <c-modal
    :title="t('components.glossaryButton.modalTitle')"
    v-model:show="showGlossaryModal"
    :extra-height="120"
  >
    <template #header-extra>
      <n-flex
        vertical
        size="large"
        style="max-width: 400px; margin-bottom: 16px"
      >
        <template v-if="gnidHint">
          <n-text style="font-size: 12px">{{ gnidHint }}</n-text>

          <n-text>
            {{ t('components.glossaryButton.guidePrefix') }}
            <c-a to="/forum/660ab4da55001f583649a621">
              {{ t('components.glossaryButton.guideLink') }}
            </c-a>
            {{ t('components.glossaryButton.guideSuffix') }}
          </n-text>
        </template>

        <n-input-group>
          <n-input
            pair
            v-model:value="termsToAdd"
            size="small"
            separator="=>"
            :placeholder="wordPlaceholders"
            :input-props="{ spellcheck: false }"
          />
          <c-button
            :label="t('components.glossaryButton.add')"
            :round="false"
            size="small"
            @action="addTerm"
          />
        </n-input-group>

        <n-input
          v-model:value="importGlossaryRaw"
          type="textarea"
          size="small"
          :placeholder="t('components.glossaryButton.bulkPlaceholder')"
          :input-props="{ spellcheck: false }"
          :rows="1"
        />

        <n-flex align="center" :wrap="false">
          <c-button
            :label="t('components.glossaryButton.export')"
            :round="false"
            size="small"
            @action="exportGlossary"
          />
          <c-button
            :label="t('components.glossaryButton.import')"
            :round="false"
            size="small"
            @action="importGlossary"
          />
          <c-button
            :label="t('components.glossaryButton.downloadJson')"
            :round="false"
            size="small"
            @action="downloadGlossaryAsJsonFile"
          />
          <c-button
            v-if="whoami.isAdmin"
            secondary
            type="error"
            :label="t('components.glossaryButton.clear')"
            :round="false"
            size="small"
            @action="clearTerm"
          />
        </n-flex>
        <n-flex align="center" :wrap="false">
          <c-button
            :disabled="deletedTerms.length === 0"
            :label="t('components.glossaryButton.undoDelete')"
            :round="false"
            size="small"
            @action="undoDeleteTerm"
          />
          <n-text
            v-if="lastDeletedTerm !== undefined"
            depth="3"
            style="font-size: 12px"
          >
            {{ lastDeletedTerm }}
          </n-text>
        </n-flex>
      </n-flex>
    </template>

    <n-table
      v-if="Object.keys(glossary).length !== 0"
      striped
      size="small"
      style="font-size: 12px; max-width: 400px"
    >
      <tr v-for="wordJp in Object.keys(glossary).reverse()" :key="wordJp">
        <td>
          <c-button
            :icon="DeleteOutlineOutlined"
            text
            type="error"
            size="small"
            @action="deleteTerm(wordJp)"
          />
        </td>
        <td>{{ wordJp }}</td>
        <td nowrap="nowrap">=></td>
        <td style="padding-right: 16px">
          <n-input
            v-model:value="glossary[wordJp]"
            size="tiny"
            :placeholder="t('components.glossaryButton.inputPlaceholder')"
            :theme-overrides="{
              border: '0',
              color: 'transprent',
            }"
          />
        </td>
      </tr>
    </n-table>

    <template #action>
      <c-button
        :label="t('components.glossaryButton.submit')"
        type="primary"
        @action="submitGlossary()"
      />
    </template>
  </c-modal>
</template>
