<script lang="ts" setup>
import { UploadOutlined } from '@vicons/material';
import type { FormInst, FormItemRule, FormRules } from 'naive-ui';
import { useI18n } from 'vue-i18n';

import { ArticleRepo } from '@/repos';
import type { ArticleCategory } from '@/model/Article';
import { doAction, useIsWideScreen } from '@/pages/util';
import { useDraftStore, useWhoamiStore } from '@/stores';

const { t } = useI18n();

const { articleId, category } = defineProps<{
  articleId: string | undefined;
  category: ArticleCategory | undefined;
}>();

const router = useRouter();
const isWideScreen = useIsWideScreen();
const message = useMessage();

const whoamiStore = useWhoamiStore();
const { whoami } = storeToRefs(whoamiStore);

const draftStore = useDraftStore();
const draftId = `article-${articleId ?? 'new'}`;

const articleCategoryOptions = computed(() =>
  whoami.value.asAdmin
    ? [
        { value: 'General', label: t('forum.categories.general') },
        { value: 'Guide', label: t('forum.categories.guide') },
        { value: 'Support', label: t('forum.categories.support') },
      ]
    : [
        { value: 'General', label: t('forum.categories.general') },
        { value: 'Support', label: t('forum.categories.support') },
      ],
);

const allowSubmit = ref(articleId === undefined);
const formRef = useTemplateRef<FormInst>('form');
const formValue = ref({
  title: '',
  content: '',
  category: category ?? 'General',
});
const formRules: FormRules = {
  title: [
    {
      validator: (_rule: FormItemRule, value: string) =>
        value.trim().length >= 2,
      message: t('forum.articleEdit.titleMinLength'),
      trigger: 'input',
    },
    {
      validator: (_rule: FormItemRule, value: string) => value.length <= 80,
      message: t('forum.articleEdit.titleMaxLength'),
      trigger: 'input',
    },
  ],
  content: [
    {
      validator: (_rule: FormItemRule, value: string) =>
        value.trim().length >= 2,
      message: t('forum.articleEdit.contentMinLength'),
      trigger: 'input',
    },
    {
      validator: (_rule: FormItemRule, value: string) => value.length <= 20_000,
      message: t('forum.articleEdit.contentMaxLength'),
      trigger: 'input',
    },
  ],
  category: [
    {
      validator: (_rule: FormItemRule, value: string | undefined) =>
        value !== undefined,
      message: t('forum.articleEdit.categoryRequired'),
      trigger: 'input',
    },
  ],
};

if (articleId !== undefined) {
  ArticleRepo.useArticle(articleId, true)
    .refresh()
    .then(({ data, error }) => {
      if (data) {
        formValue.value = {
          title: data.title,
          content: data.content,
          category: data.category,
        };
        allowSubmit.value = true;
      } else {
        message.error(t('forum.articleEdit.loadFailed', { message: error?.message }));
      }
    });
}

const submit = async () => {
  if (!allowSubmit.value) {
    message.warning(t('forum.articleEdit.notLoaded'));
    return;
  }

  try {
    await formRef.value?.validate();
  } catch (e) {
    return;
  }

  if (articleId === undefined) {
    await doAction(
      ArticleRepo.createArticle(formValue.value).then((id) => {
        draftStore.removeDraft(draftId);
        router.push({ path: `/forum/${id}` });
      }),
      t('forum.articleEdit.publish'),
      message,
    );
  } else {
    await doAction(
      ArticleRepo.updateArticle(articleId, formValue.value).then(() => {
        draftStore.removeDraft(draftId);
        router.push({ path: `/forum/${articleId}` });
      }),
      t('forum.articleEdit.update'),
      message,
    );
  }
};
</script>

<template>
  <div class="layout-content">
    <n-h1>{{ articleId === undefined ? t('forum.articleEdit.publish') : t('forum.articleEdit.edit') }} {{ t('forum.articleEdit.article') }}</n-h1>
    <n-form
      ref="form"
      :model="formValue"
      :rules="formRules"
      :label-placement="isWideScreen ? 'left' : 'top'"
      label-width="auto"
    >
      <n-form-item-row path="title" :label="t('forum.articleEdit.title')">
        <n-input
          v-model:value="formValue.title"
          :placeholder="t('forum.articleEdit.titlePlaceholder')"
          maxlength="80"
          show-count
          :input-props="{ spellcheck: false }"
        />
      </n-form-item-row>
      <n-form-item-row path="category" :label="t('forum.articleEdit.category')">
        <c-radio-group
          v-model:value="formValue.category"
          :options="articleCategoryOptions"
        />
      </n-form-item-row>
      <n-form-item-row path="content" :label="t('forum.articleEdit.content')">
        <MarkdownEditor
          mode="article"
          :draft-id="draftId"
          v-model:value="formValue.content"
          :placeholder="t('forum.articleEdit.contentPlaceholder')"
          :autosize="{ minRows: 8 }"
          maxlength="20000"
          style="width: 100%"
        />
      </n-form-item-row>
    </n-form>

    <c-button
      :label="t('forum.articleEdit.submit')"
      :icon="UploadOutlined"
      require-login
      size="large"
      type="primary"
      class="float"
      @action="submit"
    />
  </div>
</template>
