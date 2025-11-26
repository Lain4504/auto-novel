<script lang="ts" setup>
import {
  DeleteOutlineOutlined,
  KeyboardDoubleArrowDownOutlined,
  KeyboardDoubleArrowUpOutlined,
  UploadOutlined,
} from '@vicons/material';
import type { FormInst, FormItemRule, FormRules } from 'naive-ui';
import { VueDraggable } from 'vue-draggable-plus';
import { useI18n } from 'vue-i18n';

import { WenkuNovelApi } from '@/api';
import { prettyCover, smartImport } from '@/domain/smart-import';
import { WenkuNovelRepo } from '@/repos';
import coverPlaceholder from '@/image/cover_placeholder.png';
import type { WenkuNovelOutlineDto, WenkuVolumeDto } from '@/model/WenkuNovel';
import { presetKeywordsNonR18, presetKeywordsR18 } from '@/model/WenkuNovel';
import { doAction, useIsWideScreen } from '@/pages/util';
import { useWhoamiStore } from '@/stores';
import { RegexUtil, delay } from '@/util';
import { runCatching } from '@/util/result';

const { t } = useI18n();

const { novelId } = defineProps<{
  novelId: string | undefined;
}>();

const router = useRouter();
const isWideScreen = useIsWideScreen();
const message = useMessage();

const whoamiStore = useWhoamiStore();
const { whoami } = storeToRefs(whoamiStore);

const allowSubmit = ref(novelId === undefined);
const formRef = useTemplateRef<FormInst>('form');
const formValue = ref({
  title: '',
  titleZh: '',
  cover: '',
  authors: <string[]>[],
  artists: <string[]>[],
  level: '一般向',
  keywords: <string[]>[],
  introduction: '',
  volumes: <WenkuVolumeDto[]>[],
});
const formRules: FormRules = {
  title: [
    {
      validator: (_rule: FormItemRule, value: string) =>
        value.trim().length > 0,
      message: t('novel.wenkuNovelEdit.titleRequired'),
      trigger: 'input',
    },
    {
      validator: (_rule: FormItemRule, value: string) => value.length <= 80,
      message: t('novel.wenkuNovelEdit.titleMaxLength'),
      trigger: 'input',
    },
  ],
  titleZh: [
    {
      validator: (_rule: FormItemRule, value: string) =>
        value.trim().length > 0,
      message: t('novel.wenkuNovelEdit.titleRequired'),
      trigger: 'input',
    },
    {
      validator: (_rule: FormItemRule, value: string) => value.length <= 80,
      message: t('novel.wenkuNovelEdit.titleMaxLength'),
      trigger: 'input',
    },
    {
      validator: (_rule: FormItemRule, value: string) =>
        !RegexUtil.hasKanaChars(value),
      message: t('novel.wenkuNovelEdit.titleZhNoJapanese'),
      trigger: 'input',
    },
  ],
  cover: [
    {
      validator: (_rule: FormItemRule, value: string) => RegexUtil.isUrl(value),
      message: t('novel.wenkuNovelEdit.coverUrlRequired'),
      trigger: 'input',
    },
  ],
  level: [
    {
      validator: (_rule: FormItemRule, value: string) =>
        value !== '成人向' || whoami.value.allowNsfw,
      message: t('novel.wenkuNovelEdit.levelAdultRestricted'),
      trigger: 'input',
    },
  ],
  introduction: [
    {
      validator: (_rule: FormItemRule, value: string) => value.length <= 500,
      message: t('novel.wenkuNovelEdit.introductionMaxLength'),
      trigger: 'input',
    },
  ],
};

const amazonUrl = ref('');

if (novelId !== undefined) {
  WenkuNovelRepo.useWenkuNovel(novelId, false)
    .refresh()
    .then(({ data, error }) => {
      if (data) {
        const {
          title,
          titleZh,
          cover,
          authors,
          artists,
          level,
          keywords,
          introduction,
        } = data;
        formValue.value = {
          title,
          titleZh,
          cover: prettyCover(cover ?? ''),
          authors,
          artists,
          level,
          keywords,
          introduction,
          volumes: data.volumes.map((it) => {
            it.cover = prettyCover(it.cover);
            return it;
          }),
        };
        amazonUrl.value = data.title.replace(/[?？。!！]$/, '');
        allowSubmit.value = true;
      } else {
        message.error(t('novel.wenkuNovelEdit.loadFailed', { message: error?.message }));
      }
    });
}

const submit = async () => {
  if (!allowSubmit.value) {
    message.warning(t('novel.wenkuNovelEdit.notLoaded'));
    return;
  }

  try {
    await formRef.value?.validate();
  } catch (e) {
    return;
  }

  const allPresetKeywords = presetKeywords.value.groups.flatMap(
    (it) => it.presetKeywords,
  );

  const body = {
    title: formValue.value.title,
    titleZh: formValue.value.titleZh,
    cover: formValue.value.cover,
    authors: formValue.value.authors,
    artists: formValue.value.artists,
    level: formValue.value.level,
    introduction: formValue.value.introduction,
    keywords: formValue.value.keywords.filter((it) =>
      allPresetKeywords.includes(it),
    ),
    volumes: formValue.value.volumes,
  };

  if (novelId === undefined) {
    await doAction(
      WenkuNovelRepo.createNovel(body).then((id) => {
        router.push({ path: `/wenku/${id}` });
      }),
      t('novel.wenkuNovelEdit.createAction'),
      message,
    );
  } else {
    await doAction(
      WenkuNovelRepo.updateNovel(novelId, body).then(() => {
        router.push({ path: `/wenku/${novelId}` });
      }),
      t('novel.wenkuNovelEdit.editAction'),
      message,
    );
  }
};

const populateNovelFromAmazon = async (
  urlOrQuery: string,
  forcePopulateVolumes: boolean,
) => {
  const msgReactive = message.create('', {
    type: 'loading',
    duration: 0,
  });

  await smartImport(
    urlOrQuery.trim(),
    formValue.value.volumes,
    forcePopulateVolumes,
    {
      log: (message) => {
        msgReactive.content = message;
      },
      populateNovel: (novel) => {
        formValue.value = {
          title: formValue.value.title ? formValue.value.title : novel.title,
          titleZh: formValue.value.titleZh
            ? formValue.value.titleZh
            : novel.titleZh ?? '',
          cover: novel.volumes[0]?.cover,
          authors:
            formValue.value.authors.length > 0
              ? formValue.value.authors
              : novel.authors,
          artists:
            formValue.value.artists.length > 0
              ? formValue.value.artists
              : novel.artists,
          level: novel.r18 ? '成人向' : '一般向',
          keywords: formValue.value.keywords,
          introduction: formValue.value.introduction
            ? formValue.value.introduction
            : novel.introduction,
          volumes: novel.volumes,
        };
      },
      populateVolume: (volume) => {
        const index = formValue.value.volumes.findIndex(
          (it) => it.asin === volume.asin,
        );
        if (index >= 0) {
          formValue.value.volumes[index] = volume;
        }
      },
    },
  );

  formValue.value.cover = formValue.value.volumes[0]?.cover;
  msgReactive.content = t('novel.wenkuNovelEdit.smartImportComplete');
  msgReactive.type = 'info';
  delay(3000).then(() => msgReactive.destroy());
};

const submitCurrentStep = ref(1);
const title = computed(() => formValue.value.title);
const similarNovels = ref<WenkuNovelOutlineDto[] | null>(null);

watch(title, () => {
  similarNovels.value = null;
  submitCurrentStep.value = 1;
});
const findSimilarNovels = async () => {
  const query = title.value.split(
    /[^\u3040-\u309f\u30a0-\u30ff\u4e00-\u9faf\u3400-\u4dbf]/,
    2,
  )[0];
  const result = await runCatching(
    WenkuNovelApi.listNovel({
      page: 0,
      pageSize: 6,
      query,
      level: 99,
    }),
  );
  if (result.ok) {
    similarNovels.value = result.value.items;
  } else {
    message.error(t('novel.wenkuNovelEdit.searchSimilarFailed', { message: result.error.message }));
  }
};
const moveToPrevStep = () => {
  if (submitCurrentStep.value > 1) {
    submitCurrentStep.value -= 1;
  }
};
const moveToNextStep = () => {
  if (submitCurrentStep.value < 3) {
    submitCurrentStep.value += 1;
  }
};
const topVolume = (asin: string) => {
  formValue.value.volumes.sort((a, b) => {
    return a.asin == asin ? -1 : b.asin == asin ? 1 : 0;
  });
};
const bottomVolume = (asin: string) => {
  formValue.value.volumes.sort((a, b) => {
    return a.asin == asin ? 1 : b.asin == asin ? -1 : 0;
  });
};
const deleteVolume = (asin: string) => {
  formValue.value.volumes = formValue.value.volumes.filter(
    (it) => it.asin !== asin,
  );
};

const markAsDuplicate = () => {
  formValue.value = {
    title: t('novel.wenkuNovelEdit.duplicateTitle'),
    titleZh: t('novel.wenkuNovelEdit.duplicateTitle'),
    cover: '',
    authors: [],
    artists: [],
    level: formValue.value.level,
    keywords: [],
    introduction: '',
    volumes: [],
  };
};

const presetKeywords = computed(() => {
  if (formValue.value.level === '一般向') {
    return presetKeywordsNonR18;
  } else {
    return presetKeywordsR18;
  }
});
const showKeywordsModal = ref(false);

const togglePresetKeyword = (checked: boolean, keyword: string) => {
  if (checked) {
    formValue.value.keywords.push(keyword);
  } else {
    formValue.value.keywords = formValue.value.keywords.filter(
      (it) => it !== keyword,
    );
  }
};

const levelOptions = computed(() => [
  { label: t('novel.wenkuNovelEdit.levelLightNovel'), value: '一般向' },
  { label: t('novel.wenkuNovelEdit.levelLiterature'), value: '严肃向' },
  { label: '文学', value: '严肃向' },
  { label: t('novel.wenkuNovelEdit.levelNonFiction'), value: '非小说' },
  { label: t('novel.wenkuNovelEdit.levelR18Male'), value: '成人向' },
  { label: t('novel.wenkuNovelEdit.levelR18Female'), value: '成人向女' },
]);
</script>

<template>
  <div class="layout-content">
    <n-h1>{{ novelId === undefined ? t('novel.wenkuNovelEdit.create') : t('novel.wenkuNovelEdit.edit') }} {{ t('novel.wenkuNovelEdit.wenkuNovel') }}</n-h1>

    <n-card embedded :bordered="false" style="margin-bottom: 20px">
      <n-text type="error">
        <b>{{ t('novel.wenkuNovelEdit.notesTitle') }}</b>
      </n-text>
      <n-ul>
        <n-li>
          {{ t('novel.wenkuNovelEdit.note1') }}
        </n-li>
        <n-li>
          {{ t('novel.wenkuNovelEdit.note2') }}
        </n-li>
        <n-li>
          {{ t('novel.wenkuNovelEdit.note3') }}
        </n-li>
        <n-li>
          {{ t('novel.wenkuNovelEdit.note4') }}
        </n-li>
      </n-ul>
    </n-card>

    <n-flex style="margin-bottom: 48px; width: 100%">
      <div v-if="isWideScreen">
        <n-image
          width="160"
          :src="formValue.cover ? formValue.cover : coverPlaceholder"
          alt="cover"
        />
      </div>

      <n-flex size="large" vertical style="flex: auto">
        <n-input-group>
          <n-input
            v-model:value="amazonUrl"
            :placeholder="formValue.title"
            :input-props="{ spellcheck: false }"
          />
          <c-button
            :label="t('novel.wenkuNovelEdit.import')"
            :round="false"
            type="primary"
            @action="populateNovelFromAmazon(amazonUrl, false)"
          />
        </n-input-group>
        <n-flex>
          <c-button
            :label="t('novel.wenkuNovelEdit.searchAmazon')"
            secondary
            tag="a"
            :href="`https://www.amazon.co.jp/s?k=${encodeURIComponent(
              formValue.title,
            )}&i=stripbooks`"
            target="_blank"
          />
          <c-button
            secondary
            :label="t('novel.wenkuNovelEdit.refreshVolumes')"
            @action="populateNovelFromAmazon('', true)"
          />
          <c-button
            v-if="whoami.isAdmin"
            type="error"
            secondary
            :label="t('novel.wenkuNovelEdit.markDuplicate')"
            @action="markAsDuplicate"
          />
        </n-flex>
      </n-flex>
    </n-flex>

    <n-form
      ref="form"
      :model="formValue"
      :rules="formRules"
      :label-placement="isWideScreen ? 'left' : 'top'"
      label-width="auto"
    >
      <n-form-item-row path="title" :label="t('novel.wenkuNovelEdit.titleJp')">
        <n-input
          v-model:value="formValue.title"
          :placeholder="t('novel.wenkuNovelEdit.titleJpPlaceholder')"
          maxlength="80"
          show-count
          :input-props="{ spellcheck: false }"
        />
      </n-form-item-row>

      <n-form-item-row path="titleZh" :label="t('novel.wenkuNovelEdit.titleZh')">
        <n-input
          v-model:value="formValue.titleZh"
          :placeholder="t('novel.wenkuNovelEdit.titleZhPlaceholder')"
          maxlength="80"
          show-count
          :input-props="{ spellcheck: false }"
        />
      </n-form-item-row>

      <n-form-item-row path="cover" :label="t('novel.wenkuNovelEdit.cover')">
        <n-input
          v-model:value="formValue.cover"
          :placeholder="t('novel.wenkuNovelEdit.coverPlaceholder')"
          :input-props="{ spellcheck: false }"
        />
      </n-form-item-row>

      <n-form-item-row path="authors" :label="t('novel.wenkuNovelEdit.authors')">
        <n-dynamic-tags v-model:value="formValue.authors" />
      </n-form-item-row>

      <n-form-item-row path="artists" :label="t('novel.wenkuNovelEdit.artists')">
        <n-dynamic-tags v-model:value="formValue.artists" />
      </n-form-item-row>

      <n-form-item-row path="level" :label="t('novel.wenkuNovelEdit.level')">
        <c-radio-group
          v-model:value="formValue.level"
          :options="levelOptions"
        />
      </n-form-item-row>

      <n-form-item-row path="content" :label="t('novel.wenkuNovelEdit.introduction')">
        <n-input
          v-model:value="formValue.introduction"
          type="textarea"
          :placeholder="t('novel.wenkuNovelEdit.introductionPlaceholder')"
          :autosize="{
            minRows: 8,
            maxRows: 24,
          }"
          maxlength="500"
          show-count
          :input-props="{ spellcheck: false }"
        />
      </n-form-item-row>

      <n-form-item-row :label="t('novel.wenkuNovelEdit.keywords')">
        <n-list bordered style="width: 100%">
          <n-list-item>
            <c-button
              v-if="presetKeywords.groups.length > 0"
              :label="t('novel.wenkuNovelEdit.keywordsReadFirst')"
              @action="showKeywordsModal = true"
              text
              type="error"
            />
            <n-p v-else>{{ t('novel.wenkuNovelEdit.keywordsNotSupported') }}</n-p>
          </n-list-item>
          <n-list-item
            v-for="group of presetKeywords.groups"
            :key="group.title"
          >
            <n-flex size="small">
              <n-tag :bordered="false" size="small">
                <b>{{ group.title }}</b>
              </n-tag>
              <n-tag
                v-for="keyword of group.presetKeywords"
                :key="keyword"
                size="small"
                checkable
                :checked="formValue.keywords.includes(keyword)"
                @update:checked="
                  (checked: boolean) => togglePresetKeyword(checked, keyword)
                "
              >
                {{ keyword }}
              </n-tag>
            </n-flex>
          </n-list-item>
        </n-list>
      </n-form-item-row>

      <n-form-item-row :label="t('novel.wenkuNovelEdit.volumes')" v-if="formValue.volumes.length > 0">
        <n-list style="width: 100%; font-size: 12px">
          <vue-draggable
            v-model="formValue.volumes"
            :animation="150"
            handle=".drag-trigger"
          >
            <n-list-item v-for="volume of formValue.volumes" :key="volume.asin">
              <n-thing>
                <template #avatar>
                  <div>
                    <n-image
                      class="drag-trigger"
                      width="88"
                      :src="volume.cover"
                      :preview-src="volume.coverHires ?? volume.cover"
                      :alt="volume.asin"
                      lazy
                      style="border-radius: 2px; cursor: move"
                    />
                  </div>
                </template>

                <template #header>
                  <n-text style="font-size: 12px">
                    ASIN：
                    <n-a
                      :href="`https://www.amazon.co.jp/zh/dp/${volume.asin}`"
                    >
                      {{ volume.asin }}
                    </n-a>
                  </n-text>
                </template>

                <template #header-extra>
                  <n-flex :size="6" :wrap="false">
                    <c-icon-button
                      :tooltip="t('novel.wenkuNovelEdit.top')"
                      :icon="KeyboardDoubleArrowUpOutlined"
                      @action="topVolume(volume.asin)"
                    />

                    <c-icon-button
                      :tooltip="t('novel.wenkuNovelEdit.bottom')"
                      :icon="KeyboardDoubleArrowDownOutlined"
                      @action="bottomVolume(volume.asin)"
                    />

                    <c-icon-button
                      :tooltip="t('novel.wenkuNovelEdit.delete')"
                      :icon="DeleteOutlineOutlined"
                      type="error"
                      @action="deleteVolume(volume.asin)"
                    />
                  </n-flex>
                </template>

                <template #description>
                  <n-flex align="center" :size="0" :wrap="false">
                    <n-text style="word-break: keep-all; font-size: 12px">
                      {{ t('novel.wenkuNovelEdit.title') }}：
                    </n-text>
                    <n-input
                      v-model:value="volume.title"
                      :placeholder="t('novel.wenkuNovelEdit.title')"
                      :input-props="{ spellcheck: false }"
                      size="small"
                      style="font-size: 12px"
                    />
                  </n-flex>
                  <n-text style="font-size: 12px">
                    {{ t('novel.wenkuNovelEdit.thumbnail') }}：{{ volume.cover }}
                    <br />
                    {{ t('novel.wenkuNovelEdit.highRes') }}：{{ volume.coverHires }}
                    <br />
                    {{ t('novel.wenkuNovelEdit.publish') }}：
                    {{ volume.publisher ?? t('novel.wenkuNovelEdit.unknownPublisher') }}
                    /
                    {{ volume.imprint ?? t('novel.wenkuNovelEdit.unknownImprint') }}
                    /
                    <n-time
                      v-if="volume.publishAt"
                      :time="volume.publishAt * 1000"
                      type="date"
                    />
                  </n-text>
                </template>
              </n-thing>
            </n-list-item>
          </vue-draggable>
        </n-list>
      </n-form-item-row>
    </n-form>

    <n-divider />

    <c-button
      v-if="novelId"
      :label="t('novel.wenkuNovelEdit.submit')"
      :icon="UploadOutlined"
      require-login
      size="large"
      type="primary"
      class="float"
      @action="submit"
    />

    <n-steps
      v-else
      :current="submitCurrentStep"
      vertical
      style="margin-left: 8px"
    >
      <n-step :title="t('novel.wenkuNovelEdit.step1Title')">
        <p>
          {{ t('novel.wenkuNovelEdit.step1Desc1') }}
        </p>
        <p>
          {{ t('novel.wenkuNovelEdit.step1Desc2') }}
          <b>
            {{
              title.split(
                /[^\u3040-\u309f\u30a0-\u30ff\u4e00-\u9faf\u3400-\u4dbf]/,
                2,
              )[0]
            }}
          </b>
        </p>
        <p v-if="similarNovels !== null">
          <template v-if="similarNovels.length === 0">{{ t('novel.wenkuNovelEdit.noSimilarNovels') }}</template>
          <n-grid v-else :x-gap="12" :y-gap="12" cols="3 600:6">
            <n-grid-item v-for="item in similarNovels" :key="item.id">
              <router-link :to="`/wenku/${item.id}`">
                <ImageCard
                  :src="item.cover"
                  :title="item.titleZh ? item.titleZh : item.title"
                />
              </router-link>
            </n-grid-item>
          </n-grid>
        </p>
        <n-button-group v-if="submitCurrentStep === 1">
          <c-button
            :label="t('novel.wenkuNovelEdit.confirmNotExists')"
            type="warning"
            @click="moveToNextStep"
          />
          <c-button :label="t('novel.wenkuNovelEdit.searchSimilar')" @click="findSimilarNovels" />
        </n-button-group>
      </n-step>

      <n-step :title="t('novel.wenkuNovelEdit.step2Title')">
        <p>
          {{ t('novel.wenkuNovelEdit.step2Desc1') }}
        </p>
        <p>{{ t('novel.wenkuNovelEdit.step2Desc2') }}</p>

        <n-button-group v-if="submitCurrentStep === 2">
          <c-button
            :label="t('novel.wenkuNovelEdit.confirmHasFiles')"
            type="warning"
            @click="moveToNextStep"
          />
          <c-button :label="t('novel.wenkuNovelEdit.prevStep')" @click="moveToPrevStep" />
        </n-button-group>
      </n-step>

      <n-step :title="t('novel.wenkuNovelEdit.step3Title')">
        <n-button-group v-if="submitCurrentStep === 3" style="margin-top: 16px">
          <c-button
            :label="t('novel.wenkuNovelEdit.submit')"
            :icon="UploadOutlined"
            require-login
            type="primary"
            @action="submit"
          />
          <c-button :label="t('novel.wenkuNovelEdit.prevStep')" @click="moveToPrevStep" />
        </n-button-group>
      </n-step>
    </n-steps>
  </div>

  <c-modal :title="t('novel.wenkuNovelEdit.keywordsModalTitle')" v-model:show="showKeywordsModal">
    <n-p>
      {{ t('novel.wenkuNovelEdit.keywordsModalDesc1') }}
    </n-p>
    <n-p>
      {{ t('novel.wenkuNovelEdit.keywordsModalDesc2') }}
    </n-p>
    <n-divider />
    <n-p v-for="row of presetKeywords.explanations" :key="row.word">
      <b>{{ row.word }}</b>
      <br />
      {{ row.explanation }}
    </n-p>
  </c-modal>
</template>
