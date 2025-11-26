<script lang="ts" setup>
import { CommentOutlined } from '@vicons/material';
import { useI18n } from 'vue-i18n';

import { CommentRepo } from '@/repos';
import { useDraftStore } from '@/stores';

const props = defineProps<{
  site: string;
  locked: boolean;
}>();

const page = ref(1);
const { data: commentPage, error } = CommentRepo.useCommentList(
  page,
  () => props.site,
);

const draftStore = useDraftStore();
const draftId = `comment-${props.site}`;

const anchorEl = useTemplateRef('anchor');
watch(page, () => {
  anchorEl.value?.scrollIntoView();
  window.scrollBy({ top: -50, behavior: 'auto' });
});

function onReplied() {
  showInput.value = false;
  draftStore.cancelAddDraft();
  draftStore.removeDraft(draftId);
}

const { t } = useI18n();
const showInput = ref(false);
</script>

<template>
  <div ref="anchor" />
  <SectionHeader
    :title="t('components.commentList.title')"
    ref="commentSectionRef"
    style="margin-bottom: 32px"
  >
    <c-button
      v-if="!locked"
      :label="t('components.commentList.create')"
      :icon="CommentOutlined"
      require-login
      @action="showInput = !showInput"
    />
  </SectionHeader>

  <n-p v-if="locked">{{ t('components.commentList.locked') }}</n-p>

  <template v-if="showInput">
    <CommentEditor
      :site="site"
      :draft-id="draftId"
      :placeholder="t('components.commentList.replyPlaceholder')"
      @replied="onReplied()"
      @cancel="showInput = false"
    />
    <n-divider />
  </template>

  <CPage v-model:page="page" :page-number="commentPage?.pageNumber" disable-top>
    <template v-if="commentPage">
      <template v-for="comment in commentPage.items" :key="comment.id">
        <CommentThread :site="site" :comment="comment" :locked="locked" />
        <n-divider />
      </template>
      <n-empty
        v-if="commentPage.items.length === 0 && !locked"
        :description="t('components.commentList.empty')"
      />
    </template>

    <CResultX
      v-else
      :error="error"
      :title="t('components.commentList.loadError')"
    />
  </CPage>
</template>
