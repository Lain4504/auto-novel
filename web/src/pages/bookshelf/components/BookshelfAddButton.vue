<script lang="ts" setup>
import { PlusOutlined } from '@vicons/material';
import type { FormInst, FormItemRule, FormRules } from 'naive-ui';
import { useI18n } from 'vue-i18n';

import { doAction } from '@/pages/util';
import { FavoredRepo } from '@/stores';

const { t } = useI18n();

const message = useMessage();

const showAddModal = ref(false);

const formRef = useTemplateRef<FormInst>('form');
const formValue = ref<{
  title: string;
  type: 'web' | 'wenku' | 'local';
}>({
  title: '',
  type: 'web',
});
const formRules: FormRules = {
  title: [
    {
      validator: (_rule: FormItemRule, value: string) => value.length > 0,
      message: t('bookshelf.menu.titleRequired'),
      trigger: 'input',
    },
    {
      validator: (_rule: FormItemRule, value: string) => value.length <= 20,
      message: t('bookshelf.menu.titleMaxLength'),
      trigger: 'input',
    },
  ],
};

const addFavorite = async () => {
  try {
    await formRef.value?.validate();
  } catch (e) {
    return;
  }

  const { type, title } = formValue.value;
  await doAction(
    FavoredRepo.createFavored(type, title).then(() => {
      showAddModal.value = false;
    }),
    t('bookshelf.menu.createAction'),
    message,
  );
};
</script>

<template>
  <c-button :label="t('bookshelf.menu.create')" :icon="PlusOutlined" @action="showAddModal = true" />

  <c-modal :title="t('bookshelf.menu.createTitle')" v-model:show="showAddModal">
    <n-form
      ref="form"
      :model="formValue"
      :rules="formRules"
      label-placement="left"
      label-width="auto"
    >
      <n-form-item-row :label="t('bookshelf.menu.title')" path="title">
        <n-input
          v-model:value="formValue.title"
          :placeholder="t('bookshelf.menu.titlePlaceholder')"
          :input-props="{ spellcheck: false }"
        />
      </n-form-item-row>

      <n-form-item-row :label="t('bookshelf.menu.type')">
        <c-radio-group
          v-model:value="formValue.type"
          :options="[
            { label: t('bookshelf.menu.typeWeb'), value: 'web' },
            { label: t('bookshelf.menu.typeWenku'), value: 'wenku' },
            { label: t('bookshelf.menu.typeLocal'), value: 'local' },
          ]"
        />
      </n-form-item-row>
    </n-form>

    <template #action>
      <c-button
        :label="t('bookshelf.menu.confirm')"
        require-login
        type="primary"
        @action="addFavorite"
      />
    </template>
  </c-modal>
</template>
