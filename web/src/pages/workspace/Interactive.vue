<script lang="ts" setup>
import { useI18n } from 'vue-i18n';

import type { TranslatorConfig } from '@/domain/translate';
import { Translator } from '@/domain/translate';
import type { Glossary } from '@/model/Glossary';
import type { GptWorker, SakuraWorker, TranslatorId } from '@/model/Translator';
import { useGptWorkspaceStore, useSakuraWorkspaceStore } from '@/stores';

const message = useMessage();
const { t } = useI18n();

const textJp = ref('');
const textVi = ref('');

const translatorId = ref<TranslatorId>('sakura');
const translationOptions = computed<{ label: string; value: TranslatorId }[]>(
  () => [
    { label: t('workspace.interactive.translators.baidu'), value: 'baidu' },
    { label: t('workspace.interactive.translators.youdao'), value: 'youdao' },
    { label: t('workspace.interactive.translators.gpt'), value: 'gpt' },
    { label: t('workspace.interactive.translators.sakura'), value: 'sakura' },
  ],
);

watch(textJp, () => {
  textVi.value = '';
});
watch(translatorId, () => {
  textVi.value = '';
});

const gptWorkspaceRef = useGptWorkspaceStore().ref;
const selectedGptWorkerId = ref(gptWorkspaceRef.value.workers[0]?.id);

const sakuraWorkspaceRef = useSakuraWorkspaceStore().ref;
const selectedSakuraWorkerId = ref(sakuraWorkspaceRef.value.workers[0]?.id);

interface SavedTranslation {
  id: TranslatorId;
  workerId?: string;
  endpoint?: string;
  jp: string;
  vi: string;
}
const savedTranslation = ref<SavedTranslation[]>([]);

const glossary = ref<Glossary>({});

const translate = async () => {
  let config: TranslatorConfig;
  let selectedWorker: GptWorker | SakuraWorker | undefined;
  const id = translatorId.value;
  if (id === 'gpt') {
    const worker = gptWorkspaceRef.value.workers.find(
      (it) => it.id === selectedGptWorkerId.value,
    );
    if (worker === undefined) {
      message.error(t('workspace.interactive.missingGpt'));
      return;
    }
    selectedWorker = worker;
    config = {
      id,
      type: worker.type,
      model: worker.model,
      endpoint: worker.endpoint,
      key: worker.key,
    };
  } else if (id === 'sakura') {
    const worker = sakuraWorkspaceRef.value.workers.find(
      (it) => it.id === selectedSakuraWorkerId.value,
    );
    if (worker === undefined) {
      message.error(t('workspace.interactive.missingSakura'));
      return;
    }
    selectedWorker = worker;
    config = {
      id,
      endpoint: worker.endpoint,
      segLength: worker.segLength,
      prevSegLength: worker.prevSegLength,
    };
  } else {
    config = {
      id,
    };
  }

  try {
    const translator = await Translator.create(config, false);
    const linesJp = textJp.value.split('\n');
    const linesVi = await translator.translate(linesJp, {
      glossary: glossary.value,
    });
    textVi.value = linesVi.join('\n');
  } catch (e: unknown) {
    message.error(
      t('workspace.interactive.translatorError', { error: String(e) }),
    );
  }

  savedTranslation.value.push({
    id,
    workerId: selectedWorker?.id,
    endpoint: selectedWorker?.endpoint,
    jp: textJp.value,
    vi: textVi.value,
  });
};
const clearTranslation = () => {
  textJp.value = '';
  textVi.value = '';
};
const copyToClipboard = () => {
  navigator.clipboard.writeText(textVi.value);
  message.info(t('workspace.interactive.copySuccess'));
};
const clearSavedTranslation = () => {
  savedTranslation.value = [];
};
</script>

<template>
  <div class="layout-content">
    <n-h1>{{ t('workspace.interactive.title') }}</n-h1>

    <n-flex vertical>
      <c-action-wrapper :title="t('workspace.interactive.translationTitle')">
        <n-flex vertical>
          <c-radio-group
            v-model:value="translatorId"
            :options="translationOptions"
            size="small"
          />

          <n-radio-group
            v-if="translatorId === 'gpt'"
            v-model:value="selectedGptWorkerId"
          >
            <n-flex vertical>
              <n-radio
                v-for="worker of gptWorkspaceRef.workers"
                :key="worker.id"
                :value="worker.id"
              >
                {{ worker.id }}
                <n-text depth="3">
                  {{ worker.model }}@{{
                    worker.endpoint ? worker.endpoint : 'default'
                  }}
                </n-text>
              </n-radio>
            </n-flex>
          </n-radio-group>

          <n-radio-group
            v-if="translatorId === 'sakura'"
            v-model:value="selectedSakuraWorkerId"
          >
            <n-flex vertical>
              <n-radio
                v-for="worker of sakuraWorkspaceRef.workers"
                :key="worker.id"
                :value="worker.id"
              >
                {{ worker.id }}
                <n-text depth="3">
                  {{ worker.endpoint }}
                </n-text>
              </n-radio>
            </n-flex>
          </n-radio-group>
        </n-flex>
      </c-action-wrapper>

      <c-action-wrapper :title="t('workspace.interactive.actionTitle')">
        <n-flex style="margin-bottom: 16px">
          <n-button-group size="small">
            <c-button
              :label="t('workspace.interactive.translate')"
              :round="false"
              @action="translate"
            />
            <c-button
              :label="t('workspace.interactive.clear')"
              :round="false"
              @action="clearTranslation"
            />
          </n-button-group>

          <glossary-button :value="glossary" :round="false" size="small" />

          <c-button
            :label="t('workspace.interactive.copy')"
            :round="false"
            size="small"
            @action="copyToClipboard"
          />
        </n-flex>
      </c-action-wrapper>
    </n-flex>

    <n-input-group>
      <n-input
        v-model:value="textJp"
        :placeholder="t('workspace.interactive.inputPlaceholder')"
        type="textarea"
        :autosize="{ minRows: 15 }"
        show-count
        :maxlength="5000"
        style="flex: 1"
        :input-props="{ spellcheck: false }"
      />

      <n-input
        v-model:value="textVi"
        readonly
        :placeholder="t('workspace.interactive.outputPlaceholder')"
        type="textarea"
        :autosize="{ minRows: 15 }"
        show-count
        style="flex: 1"
        :input-props="{ spellcheck: false }"
      />
    </n-input-group>

    <section-header :title="t('workspace.interactive.historyTitle')">
      <c-button
        :label="t('workspace.interactive.clear')"
        @action="clearSavedTranslation"
      />
    </section-header>

    <n-empty
      v-if="savedTranslation.length === 0"
      :description="t('workspace.interactive.historyEmpty')"
    />
    <n-list>
      <n-list-item v-for="t of savedTranslation" :key="t.id">
        <n-thing content-indented>
          <template #avatar>
            <n-icon-wrapper
              :size="12"
              :border-radius="0"
              style="margin-top: 5px"
            />
          </template>

          <template #header>
            {{ t.id }}
            {{ t.workerId }}
            <n-text depth="3" style="font-size: 12px; padding-left: 2px">
              {{ t.endpoint }}
            </n-text>
          </template>

          <template #description>
            <n-collapse style="margin-top: 16px">
              <n-collapse-item :title="t('workspace.interactive.historyJp')">
                <template v-for="line of t.jp.split('\n')" :key="line">
                  {{ line }}
                  <br />
                </template>
              </n-collapse-item>
              <n-collapse-item :title="t('workspace.interactive.historyZh')">
                <template v-for="line of t.vi.split('\n')" :key="line">
                  {{ line }}
                  <br />
                </template>
              </n-collapse-item>
            </n-collapse>
          </template>
        </n-thing>
      </n-list-item>
    </n-list>
  </div>
</template>
