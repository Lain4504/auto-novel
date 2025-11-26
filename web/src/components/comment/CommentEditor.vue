<script lang="ts" setup>
import { useI18n } from 'vue-i18n';

import { CommentRepo } from '@/repos';
import { doAction } from '@/pages/util';

const props = defineProps<{
  site: string;
  draftId: string;
  parent?: string;
  placeholder?: string;
}>();

const emit = defineEmits<{
  cancel: [];
  replied: [];
}>();

const message = useMessage();
const { t } = useI18n();

const content = ref('');

const reply = async () => {
  if (content.value.length === 0) {
    message.info(t('components.commentEditor.empty'));
    return;
  }

  await doAction(
    CommentRepo.createComment({
      site: props.site,
      parent: props.parent,
      content: content.value,
    }).then(() => {
      content.value = '';
      emit('replied');
    }),
    t('components.commentEditor.publishAction'),
    message,
  );
};
</script>

<template>
  <div>
    <MarkdownEditor
      mode="comment"
      :draft-id="draftId"
      v-model:value="content"
      :placeholder="placeholder"
      :autosize="{ minRows: 4, maxRows: 12 }"
      maxlength="1000"
    />
    <n-flex style="margin-top: 10px">
      <c-button
        :label="t('components.commentEditor.submit')"
        require-login
        :round="false"
        type="primary"
        @action="reply()"
      />
      <c-button
        :label="t('components.commentEditor.cancel')"
        :round="false"
        @action="emit('cancel')"
      />
    </n-flex>
  </div>
</template>
