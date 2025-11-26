<script lang="ts" setup>
import { MoreVertOutlined } from '@vicons/material';
import type { FormInst, FormItemRule, FormRules } from 'naive-ui';
import { useI18n } from 'vue-i18n';

import { doAction } from '@/pages/util';
import { FavoredRepo } from '@/stores';
import { useBookshelfLocalStore } from '../BookshelfLocalStore';

const { t } = useI18n();

const { id, type, title } = defineProps<{
  id: string;
  title: string;
  type: 'web' | 'wenku' | 'local';
}>();

const store = useBookshelfLocalStore();

const message = useMessage();

const getOptions = () => {
  if (id === 'all') {
    return [];
  } else if (id === 'default') {
    return [{ label: t('bookshelf.menu.editInfo'), key: 'edit' }];
  } else {
    return [
      { label: t('bookshelf.menu.editInfo'), key: 'edit' },
      { label: t('bookshelf.menu.delete'), key: 'delete' },
    ];
  }
};

const options = getOptions();

const onSelect = (key: string) => {
  if (key === 'edit') {
    showEditModal.value = true;
  } else if (key === 'delete') {
    showDeleteModal.value = true;
  }
};

const showEditModal = ref(false);
const formRef = useTemplateRef<FormInst>('form');
const formValue = ref({ title });
const formRules: FormRules = {
  title: [
    {
      validator: (_rule: FormItemRule, value: string) => value.length > 0,
      message: t('bookshelf.menu.titleRequired'),
      trigger: 'input',
    },
  ],
};
const updateFavored = async () => {
  if (formRef.value == null) {
    return;
  } else {
    try {
      await formRef.value.validate();
    } catch (e) {
      return;
    }
  }

  const title = formValue.value.title;

  await doAction(
    FavoredRepo.updateFavored(type, id, title).then(() => {
      showEditModal.value = false;
    }),
    t('bookshelf.menu.updateAction'),
    message,
  );
};

const deleteFavoredNovels = async () => {
  if (type === 'local') {
    const { failed } = await store.deleteVolumes(
      store.volumes.filter((it) => it.favoredId === id).map(({ id }) => id),
    );
    if (failed > 0) {
      throw new Error(t('bookshelf.menu.deleteAction', { failed }));
    }
  }
};

const showDeleteModal = ref(false);
const deleteFavored = () =>
  doAction(
    deleteFavoredNovels()
      .then(() => FavoredRepo.deleteFavored(type, id))
      .then(() => (showDeleteModal.value = false)),
    t('bookshelf.menu.deleteAction'),
    message,
  );
</script>

<template>
  <RouterLink :to="`/favorite/${type}/${id}`">
    <n-flex align="center" justify="space-between">
      {{ title }}
      <n-dropdown
        v-if="options.length > 0"
        trigger="hover"
        :options="options"
        :keyboard="false"
        @select="onSelect"
      >
        <n-button quaternary circle>
          <n-icon :component="MoreVertOutlined" />
        </n-button>
      </n-dropdown>
    </n-flex>
  </RouterLink>

  <c-modal v-model:show="showEditModal" :title="t('bookshelf.menu.editTitle')">
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
    </n-form>

    <template #action>
      <c-button
        :label="t('bookshelf.menu.confirm')"
        require-login
        type="primary"
        @action="updateFavored"
      />
    </template>
  </c-modal>

  <c-modal v-model:show="showDeleteModal" :title="t('bookshelf.menu.deleteTitle')">
    {{ t('bookshelf.menu.deleteConfirm', { title }) }}
    <n-text v-if="type === 'local'">
      <br />
      {{ t('bookshelf.menu.deleteConfirmLocal') }}
    </n-text>

    <template #action>
      <c-button
        :label="t('bookshelf.menu.confirm')"
        require-login
        type="primary"
        @action="deleteFavored"
      />
    </template>
  </c-modal>
</template>
