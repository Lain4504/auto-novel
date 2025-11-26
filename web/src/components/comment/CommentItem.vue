<script lang="ts" setup>
import {
  CommentOutlined,
  DeleteOutlined,
  MoreVertOutlined,
} from '@vicons/material';
import { useI18n } from 'vue-i18n';

import type { Comment1 } from '@/model/Comment';
import { useBlacklistStore, useWhoamiStore } from '@/stores';
import { CommentRepo } from '@/repos';
import { copyToClipBoard, doAction } from '@/pages/util';

const props = defineProps<{
  site: string;
  parentId?: string;
  comment: Comment1;
}>();

const emit = defineEmits<{
  reply: [Comment1];
}>();

const message = useMessage();

const whoamiStore = useWhoamiStore();
const { whoami } = storeToRefs(whoamiStore);

const blacklistStore = useBlacklistStore();
const { blacklist } = storeToRefs(blacklistStore);

const { t } = useI18n();

const options = computed(() => {
  const options = [
    { label: t('components.commentItem.options.copy'), key: 'copy' },
  ];
  if (whoami.value.asAdmin) {
    if (props.comment.hidden) {
      options.push({
        label: t('components.commentItem.options.unhide'),
        key: 'unhide',
      });
    } else {
      options.push({
        label: t('components.commentItem.options.hide'),
        key: 'hide',
      });
    }
  }
  if (blacklist.value.usernames.includes(props.comment.user.username)) {
    options.push({
      label: t('components.commentItem.options.unblock'),
      key: 'unblock',
    });
  } else {
    options.push({
      label: t('components.commentItem.options.block'),
      key: 'block',
    });
  }
  return options;
});

const handleSelect = (key: string) => {
  if (key === 'copy') {
    copyComment(props.comment);
  } else if (key === 'hide') {
    hideComment(props.comment);
  } else if (key === 'unhide') {
    unhideComment(props.comment);
  } else if (key === 'block') {
    blockUser(props.comment);
  } else if (key === 'unblock') {
    unblockUser(props.comment);
  }
};

function deleteComment() {
  doAction(
    CommentRepo.deleteComment(props.comment.id, props.site, props.parentId),
    t('components.commentItem.actions.delete'),
    message,
  );
}

function copyComment(comment: Comment1) {
  doAction(
    copyToClipBoard(comment.content),
    t('components.commentItem.actions.copy'),
    message,
  );
}

function hideComment(comment: Comment1) {
  doAction(
    CommentRepo.hideComment(comment.id).then(() => (comment.hidden = true)),
    t('components.commentItem.actions.hide'),
    message,
  );
}

function unhideComment(comment: Comment1) {
  doAction(
    CommentRepo.unhideComment(comment.id).then(() => (comment.hidden = false)),
    t('components.commentItem.actions.unhide'),
    message,
  );
}

function blockUser(comment: Comment1) {
  doAction(
    (async () => {
      blacklistStore.add(comment.user.username);
    })(),
    t('components.commentItem.actions.block'),
    message,
  );
}

function unblockUser(comment: Comment1) {
  doAction(
    (async () => {
      blacklistStore.remove(comment.user.username);
    })(),
    t('components.commentItem.actions.unblock'),
    message,
  );
}

const isDeletable = computed(() => {
  return (
    whoami.value.asAdmin ||
    (whoami.value.isMe(props.comment.user.username) &&
      Date.now() / 1000 - props.comment.createAt < 3600 * 24)
  );
});

const isBlocked = computed(() => {
  return blacklist.value.usernames.includes(props.comment.user.username);
});
</script>

<template>
  <n-flex align="center" :size="0">
    <n-text>
      <b>{{ comment.user.username }}</b>
    </n-text>
    <n-text depth="3" style="font-size: 12px; margin-left: 12px">
      <n-time :time="comment.createAt * 1000" type="relative" />
    </n-text>

    <div style="flex: 1" />

    <c-button
      v-if="parentId === undefined && whoami.allowAdvancedFeatures"
      :label="t('components.commentItem.reply')"
      :icon="CommentOutlined"
      require-login
      quaternary
      type="tertiary"
      size="tiny"
      style="margin-right: 4px"
      @action="emit('reply', comment)"
    />

    <c-button-confirm
      v-if="isDeletable"
      :hint="t('components.commentItem.deleteHint')"
      :label="t('components.commentItem.delete')"
      :icon="DeleteOutlined"
      require-login
      quaternary
      type="tertiary"
      size="tiny"
      style="margin-right: 4px"
      @action="deleteComment()"
    />

    <n-dropdown trigger="click" :options="options" @select="handleSelect">
      <n-button circle quaternary type="tertiary" size="tiny">
        <n-icon :component="MoreVertOutlined" />
      </n-button>
    </n-dropdown>
  </n-flex>

  <n-card embedded :bordered="false" size="small" style="margin-top: 2px">
    <n-text v-if="comment.hidden" depth="3">
      {{ t('components.commentItem.hiddenTag') }}
    </n-text>
    <n-text v-else-if="isBlocked" depth="3">
      {{ t('components.commentItem.blockedTag') }}
    </n-text>
    <MarkdownView
      v-else
      mode="comment"
      :source="comment.content"
      style="margin-top: -1em; margin-bottom: -1em"
    />
  </n-card>
</template>
