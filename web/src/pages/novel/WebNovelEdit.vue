<script lang="ts" setup>
import { UploadOutlined } from '@vicons/material';
import { useI18n } from 'vue-i18n';

import { WebNovelRepo } from '@/repos';
import { doAction, useIsWideScreen } from '@/pages/util';

const { t } = useI18n();

const { providerId, novelId } = defineProps<{
  providerId: string;
  novelId: string;
}>();

const router = useRouter();
const isWideScreen = useIsWideScreen();
const message = useMessage();

const allowSubmit = ref(false);
const formValue = ref({
  titleJp: '',
  title: '',
  introductionJp: '',
  introduction: '',
  wenkuId: '',
  toc: <{ jp: string; zh: string }[]>[],
});

WebNovelRepo.useWebNovel(providerId, novelId, false)
  .refresh()
  .then(({ data, error }) => {
    if (data) {
      const tocSet = new Set();
      formValue.value = {
        titleJp: data.titleJp,
        title: data.titleZh ?? '',
        introductionJp: data.introductionJp,
        introduction: data.introductionZh ?? '',
        wenkuId: data.wenkuId ?? '',
        toc: data.toc
          .filter((item) => {
            const inSet = tocSet.has(item.titleJp);
            if (!inSet) tocSet.add(item.titleJp);
            return !inSet;
          })
          .map((item) => ({
            jp: item.titleJp,
            zh: item.titleZh ?? '',
          })),
      };
      allowSubmit.value = true;
    } else {
      message.error(t('novel.webNovelEdit.loadFailed', { message: error?.message }));
    }
  });

const submit = async () => {
  if (!allowSubmit.value) {
    message.warning(t('novel.webNovelEdit.notLoaded'));
    return;
  }

  await doAction(
    WebNovelRepo.updateNovel(providerId, novelId, {
      title: formValue.value.title.trim(),
      introduction: formValue.value.introduction.trim(),
      wenkuId: formValue.value.wenkuId.trim(),
      toc: Object.assign(
        {},
        ...formValue.value.toc.map((item) => ({ [item.jp]: item.zh })),
      ),
    }).then(() => {
      router.push({ path: `/novel/${providerId}/${novelId}` });
    }),
    t('novel.webNovelEdit.editAction'),
    message,
  );
};
</script>

<template>
  <div class="layout-content">
    <n-h1>{{ t('novel.webNovelEdit.editTitle') }}</n-h1>

    <n-form
      :model="formValue"
      :label-placement="isWideScreen ? 'left' : 'top'"
      label-width="auto"
    >
      <n-form-item path="wenkuId" :label="t('novel.webNovelEdit.wenkuLink')">
        <n-input-group>
          <n-input-group-label>wenku/</n-input-group-label>
          <n-input
            v-model:value="formValue.wenkuId"
            :placeholder="t('novel.webNovelEdit.wenkuIdPlaceholder')"
            :input-props="{ spellcheck: false }"
          />
        </n-input-group>
      </n-form-item>

      <n-form-item :label="t('novel.webNovelEdit.titleJp')">
        {{ formValue.titleJp }}
      </n-form-item>
      <n-form-item path="title" :label="t('novel.webNovelEdit.titleZh')">
        <n-input
          v-model:value="formValue.title"
          :placeholder="formValue.titleJp"
          :input-props="{ spellcheck: false }"
        />
      </n-form-item>

      <n-form-item :label="t('novel.webNovelEdit.introductionJp')">
        {{ formValue.introductionJp }}
      </n-form-item>
      <n-form-item path="introduction" :label="t('novel.webNovelEdit.introductionZh')">
        <n-input
          v-model:value="formValue.introduction"
          :placeholder="formValue.introductionJp"
          :input-props="{ spellcheck: false }"
          :autosize="{ minRows: 3, maxRows: 10 }"
          type="textarea"
        />
      </n-form-item>
    </n-form>

    <n-h2 prefix="bar">{{ t('novel.webNovelEdit.catalog') }}</n-h2>
    <n-p>
      <n-text type="error">
        {{ t('novel.webNovelEdit.catalogWarning') }}
      </n-text>
    </n-p>
    <n-table :bordered="false" :bottom-bordered="false" style="width: 100%">
      <tr v-for="token in formValue.toc" :key="token.jp">
        <td style="width: 50%; padding: 4px">
          {{ token.jp }}
          <br />
          <n-input
            v-if="!isWideScreen"
            v-model:value="token.zh"
            :placeholder="token.jp"
            :input-props="{ spellcheck: false }"
          />
        </td>
        <td v-if="isWideScreen" style="padding: 4px">
          <n-input
            v-model:value="token.zh"
            :placeholder="token.jp"
            :input-props="{ spellcheck: false }"
          />
        </td>
      </tr>
    </n-table>

    <n-divider />

    <c-button
      :label="t('novel.webNovelEdit.submit')"
      :icon="UploadOutlined"
      require-login
      size="large"
      type="primary"
      class="float"
      @action="submit"
    />
  </div>
</template>
