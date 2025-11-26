<script lang="ts" setup>
import type { FormInst, FormItemRule, FormRules } from 'naive-ui';
import { useI18n } from 'vue-i18n';

import type { SakuraWorker } from '@/model/Translator';
import { useSakuraWorkspaceStore } from '@/stores';

const props = defineProps<{
  show: boolean;
  worker?: SakuraWorker;
}>();
const emit = defineEmits<{
  'update:show': [boolean];
}>();

const workspace = useSakuraWorkspaceStore();
const workspaceRef = workspace.ref;
const { t } = useI18n();

const initFormValue = () => {
  const worker = props.worker;
  if (worker === undefined) {
    return {
      id: '',
      endpoint: '',
      segLength: 500,
      prevSegLength: 500,
    };
  } else {
    return { ...worker };
  }
};

const formRef = useTemplateRef<FormInst>('form');
const formValue = ref(initFormValue());
const formRules: FormRules = {
  id: [
    {
      validator: (rule: FormItemRule, value: string) => value.trim().length > 0,
      message: t('workspace.sakuraWorker.validation.nameRequired'),
      trigger: 'input',
    },
    {
      validator: (rule: FormItemRule, value: string) =>
        workspaceRef.value.workers
          .filter(({ id }) => id !== props.worker?.id)
          .find(({ id }) => id === value) === undefined,
      message: t('workspace.sakuraWorker.validation.nameDuplicate'),
      trigger: 'input',
    },
  ],
  endpoint: [
    {
      validator: (rule: FormItemRule, value: string) => value.trim().length > 0,
      message: t('workspace.sakuraWorker.validation.endpointRequired'),
      trigger: 'input',
    },
    {
      validator: (rule: FormItemRule, value: string) => {
        try {
          const url = new URL(value);
          return url.protocol === 'http:' || url.protocol === 'https:';
        } catch (_) {
          return false;
        }
      },
      message: t('workspace.sakuraWorker.validation.endpointInvalid'),
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
  const worker = { ...formValue.value };
  worker.id = worker.id.trim();
  worker.endpoint = worker.endpoint.trim();

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
    ? t('workspace.sakuraWorker.add')
    : t('workspace.sakuraWorker.update'),
);
const modalTitle = computed(() =>
  props.worker === undefined
    ? t('workspace.sakuraWorker.modalTitleAdd')
    : t('workspace.sakuraWorker.modalTitleUpdate'),
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
      <n-form-item-row path="id" :label="t('workspace.sakuraWorker.fieldName')">
        <n-input
          v-model:value="formValue.id"
          :placeholder="t('workspace.sakuraWorker.placeholderName')"
          :input-props="{ spellcheck: false }"
        />
      </n-form-item-row>
      <n-form-item-row
        path="endpoint"
        :label="t('workspace.sakuraWorker.fieldEndpoint')"
      >
        <n-input
          v-model:value="formValue.endpoint"
          :placeholder="t('workspace.sakuraWorker.placeholderEndpoint')"
          :input-props="{ spellcheck: false }"
        />
      </n-form-item-row>

      <n-form-item-row
        path="segLength"
        :label="t('workspace.sakuraWorker.fieldSegLength')"
      >
        <n-input-number
          v-model:value="formValue.segLength"
          :show-button="false"
          :min="100"
        />
      </n-form-item-row>

      <n-form-item-row
        path="prevSegLength"
        :label="t('workspace.sakuraWorker.fieldPrevSegLength')"
      >
        <n-input-number
          v-model:value="formValue.prevSegLength"
          :show-button="false"
          :min="0"
        />
      </n-form-item-row>

      <n-text type="error" style="font-size: 12px">
        {{ t('workspace.sakuraWorker.tipPrev') }}
      </n-text>
      <br />
      <n-text depth="3" style="font-size: 12px">
        {{ t('workspace.sakuraWorker.tipSeg') }}
        <br />
        {{ t('workspace.sakuraWorker.tipExample') }}
      </n-text>
    </n-form>

    <template #action>
      <c-button
        :label="t('workspace.sakuraWorker.submit')"
        type="primary"
        @action="submit"
      />
    </template>
  </c-modal>
</template>
