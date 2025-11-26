<script lang="ts" setup>
import type { FormInst, FormItemRule, FormRules } from 'naive-ui';
import { useI18n } from 'vue-i18n';

import type { GptWorker } from '@/model/Translator';
import { useGptWorkspaceStore } from '@/stores';

const props = defineProps<{
  show: boolean;
  worker?: GptWorker;
}>();
const emit = defineEmits<{
  'update:show': [boolean];
}>();

const workspace = useGptWorkspaceStore();
const workspaceRef = workspace.ref;
const { t } = useI18n();

const initFormValue = (): {
  id: string;
  model: string;
  endpoint: string;
  key: string;
} => {
  const worker = props.worker;
  if (worker === undefined) {
    return {
      id: '',
      model: 'deepseek-chat',
      endpoint: 'https://api.deepseek.com',
      key: '',
    };
  } else {
    return {
      id: worker.id,
      model: worker.model,
      endpoint: worker.endpoint,
      key: worker.key,
    };
  }
};

const formRef = useTemplateRef<FormInst>('form');
const formValue = ref(initFormValue());

const fieldLabel = (field: 'name' | 'model' | 'endpoint' | 'key') => {
  switch (field) {
    case 'name':
      return t('workspace.gptWorker.fieldName');
    case 'model':
      return t('workspace.gptWorker.fieldModel');
    case 'endpoint':
      return t('workspace.gptWorker.fieldEndpoint');
    case 'key':
      return t('workspace.gptWorker.fieldKey');
  }
};

const emptyCheck = (field: 'name' | 'model' | 'endpoint' | 'key') => ({
  validator: (rule: FormItemRule, value: string) => value.trim().length > 0,
  message: t('workspace.gptWorker.validation.required', {
    field: fieldLabel(field),
  }),
  trigger: 'input',
});

const formRules: FormRules = {
  id: [
    emptyCheck('name'),
    {
      validator: (rule: FormItemRule, value: string) =>
        workspaceRef.value.workers
          .filter(({ id }) => id !== props.worker?.id)
          .find(({ id }) => id === value) === undefined,
      message: t('workspace.gptWorker.validation.nameDuplicate'),
      trigger: 'input',
    },
  ],
  model: [emptyCheck('model')],
  endpoint: [
    emptyCheck('endpoint'),
    {
      validator: (rule: FormItemRule, value: string) => {
        try {
          const url = new URL(value);
          return url.protocol === 'http:' || url.protocol === 'https:';
        } catch (_) {
          return false;
        }
      },
      message: t('workspace.gptWorker.validation.endpointInvalid'),
      trigger: 'input',
    },
  ],
  key: [
    emptyCheck('key'),
    {
      level: 'warning',
      validator: (rule: FormItemRule, value: string) =>
        workspaceRef.value.workers
          .filter(({ id }) => id !== props.worker?.id)
          .find(({ key }) => key === value) === undefined,
      message: t('workspace.gptWorker.validation.keyDuplicate'),
      trigger: 'input',
    },
  ],
};

const submit = async () => {
  const validated = await new Promise<boolean>(function (resolve, _reject) {
    formRef.value?.validate((errors) => {
      if (errors) resolve(false);
      else resolve(true);
    });
  });
  if (!validated) return;

  const { id, model, endpoint, key } = formValue.value;
  const worker = {
    id: id.trim(),
    type: 'api' as const,
    model: model.trim(),
    endpoint: endpoint.trim(),
    key: key.trim(),
  };

  if (props.worker === undefined) {
    workspace.addWorker(worker);
  } else {
    const index = workspaceRef.value.workers.findIndex(
      ({ id }) => id === props.worker?.id,
    );
    workspaceRef.value.workers[index] = worker;
    emit('update:show', false);
  }
};

const verb = computed(() =>
  props.worker === undefined
    ? t('workspace.gptWorker.add')
    : t('workspace.gptWorker.update'),
);
const modalTitle = computed(() =>
  props.worker === undefined
    ? t('workspace.gptWorker.modalTitleAdd')
    : t('workspace.gptWorker.modalTitleUpdate'),
);
</script>

<template>
  <c-modal
    :show="show"
    @update:show="$emit('update:show', $event)"
    :title="modalTitle"
  >
    <n-form
      ref="form"
      :model="formValue"
      :rules="formRules"
      label-placement="left"
      label-width="auto"
    >
      <n-form-item-row path="id" :label="t('workspace.gptWorker.fieldName')">
        <n-input
          v-model:value="formValue.id"
          :placeholder="t('workspace.gptWorker.placeholderName')"
          :input-props="{ spellcheck: false }"
        />
      </n-form-item-row>

      <n-form-item-row
        path="model"
        :label="t('workspace.gptWorker.fieldModel')"
      >
        <n-input
          v-model:value="formValue.model"
          :placeholder="t('workspace.gptWorker.placeholderModel')"
          :input-props="{ spellcheck: false }"
        />
      </n-form-item-row>
      <n-form-item-row
        path="endpoint"
        :label="t('workspace.gptWorker.fieldEndpoint')"
      >
        <n-input
          v-model:value="formValue.endpoint"
          :placeholder="t('workspace.gptWorker.placeholderEndpoint')"
          :input-props="{ spellcheck: false }"
        />
      </n-form-item-row>

      <n-form-item-row path="key" :label="t('workspace.gptWorker.fieldKey')">
        <n-input
          v-model:value="formValue.key"
          :placeholder="t('workspace.gptWorker.placeholderKey')"
          :input-props="{ spellcheck: false }"
        />
      </n-form-item-row>

      <n-text depth="3" style="font-size: 12px">
        {{ t('workspace.gptWorker.tipExample') }}
      </n-text>
    </n-form>

    <template #action>
      <c-button
        :label="t('workspace.gptWorker.submit')"
        type="primary"
        @action="submit"
      />
    </template>
  </c-modal>
</template>
