<script setup lang="ts">
import {
  FormatBoldOutlined,
  FormatItalicOutlined,
  HelpOutlineOutlined,
  LinkOutlined,
  MenuOpenOutlined,
  StarOutlineFilled,
  StrikethroughSOutlined,
  WarningAmberOutlined,
} from '@vicons/material';
import type { DropdownOption } from 'naive-ui';
import { useI18n } from 'vue-i18n';

import type { Draft } from '@/stores';
import { useLocaleStore } from '@/stores';

const props = defineProps<{
  elTextarea?: HTMLTextAreaElement;
  drafts: Draft[];
}>();

const emit = defineEmits<{
  clearDraft: [];
}>();

// ==============================
// 草稿
// ==============================

const draftOptions = ref<DropdownOption[]>([]);
const { t } = useI18n();
const localeStore = useLocaleStore();
const { locale } = storeToRefs(localeStore);

watch(
  () => ({ drafts: props.drafts, selectedLocale: locale.value }),
  ({ drafts, selectedLocale }) => {
    const formatter = new Intl.DateTimeFormat(
      selectedLocale === 'vi' ? 'vi-VN' : 'zh-CN',
      { dateStyle: 'short', timeStyle: 'medium' },
    );
    const draftOptionsValue: DropdownOption[] = [];
    for (const draft of drafts.slice().reverse()) {
      draftOptionsValue.push({
        label: formatter.format(draft.createdAt),
        key: draft.createdAt.getTime(),
        draftText: draft.text,
      });
    }
    draftOptionsValue.push(
      { type: 'divider' },
      { label: t('components.markdownToolbar.clear'), key: 'clear' },
    );
    draftOptions.value = draftOptionsValue;
  },
  { immediate: true },
);

const handleSelectDraft = (key: string, option: DropdownOption) => {
  if (key === 'clear') {
    emit('clearDraft');
  } else {
    const { elTextarea } = props;
    if (!elTextarea) return;
    elTextarea.value = option.draftText as string;
    elTextarea.dispatchEvent(new Event('input'));
  }
};

// ==============================
// 编辑
// ==============================

const showGuideModal = ref(false);

type TextSelection = {
  before: string;
  middle: string;
  after: string;
};

type Processor = (selection: TextSelection) => TextSelection;

const applyFormat = (formatter: Processor) => {
  const { elTextarea } = props;
  if (!elTextarea) return;
  elTextarea.focus();

  const { value, selectionStart, selectionEnd } = elTextarea;

  const { before, middle, after } = formatter({
    before: value.slice(0, selectionStart),
    middle: value.slice(selectionStart, selectionEnd),
    after: value.slice(selectionEnd),
  });

  const newValue = before + middle + after;
  const newSelectionStart = before.length;
  const newSelectionEnd = newSelectionStart + middle.length;

  elTextarea.value = newValue;
  elTextarea.setSelectionRange(newSelectionStart, newSelectionEnd);
  elTextarea.dispatchEvent(new Event('input'));
};

const warp = (
  prefix: string,
  suffix: string,
  placeholder: string,
  inline: boolean = true,
) => {
  applyFormat(({ before, middle, after }: TextSelection) => {
    if (before.endsWith(prefix) && after.startsWith(suffix)) {
      before = before.slice(0, -prefix.length);
      after = after.slice(suffix.length);
    } else if (
      middle.length >= prefix.length + suffix.length &&
      middle.startsWith(prefix) &&
      middle.endsWith(suffix)
    ) {
      middle = middle.slice(prefix.length, -suffix.length);
    } else {
      if (!inline) {
        if (!before.endsWith('\n') && before) before = before + '\n';
        if (!after.startsWith('\n')) after = '\n' + after;
      }
      before = before + prefix;
      after = suffix + after;
      if (middle.length === 0) {
        middle = placeholder;
      }
    }
    return { before, middle, after };
  });
};

const insert = (text: string) => {
  applyFormat(({ before, middle, after }: TextSelection) => {
    if (!before.endsWith('\n') && before) before = before + '\n';
    if (!after.startsWith('\n')) after = '\n' + after;
    middle = text;
    return { before, middle, after };
  });
};

const formatBold = () =>
  warp('**', '**', t('components.markdownToolbar.placeholders.bold'));
const formatItalic = () =>
  warp('*', '*', t('components.markdownToolbar.placeholders.italic'));
const formatStrikethrough = () =>
  warp('~~', '~~', t('components.markdownToolbar.placeholders.strike'));
const formatLink = () =>
  warp(
    '[',
    `](${t('components.markdownToolbar.placeholders.linkUrl')})`,
    t('components.markdownToolbar.actions.link'),
  );
const formatSpoiler = () =>
  warp('!!', '!!', t('components.markdownToolbar.placeholders.spoiler'));

const formatStar = () => insert('::: star 5');
const formatCollapsibleBlock = () =>
  warp(
    t('components.markdownToolbar.collapsePrefix'),
    '\n:::',
    t('components.markdownToolbar.placeholders.collapse'),
    false,
  );
</script>

<template>
  <n-dropdown
    v-if="drafts.length"
    :options="draftOptions"
    trigger="click"
    @select="handleSelectDraft"
  >
    <n-button size="small" quaternary>
      <n-badge :value="drafts.length" dot :offset="[8, -4]">
        {{ t('components.markdownToolbar.drafts') }}
      </n-badge>
    </n-button>
  </n-dropdown>

  <MarkdownToolbarButton
    :label="t('components.markdownToolbar.actions.bold')"
    :icon="FormatBoldOutlined"
    @action="formatBold"
  />
  <MarkdownToolbarButton
    :label="t('components.markdownToolbar.actions.italic')"
    :icon="FormatItalicOutlined"
    @action="formatItalic"
  />
  <MarkdownToolbarButton
    :label="t('components.markdownToolbar.actions.strike')"
    :icon="StrikethroughSOutlined"
    @action="formatStrikethrough"
  />
  <MarkdownToolbarButton
    :label="t('components.markdownToolbar.actions.link')"
    :icon="LinkOutlined"
    @action="formatLink"
  />
  <MarkdownToolbarButton
    :label="t('components.markdownToolbar.actions.spoiler')"
    :icon="WarningAmberOutlined"
    @action="formatSpoiler"
  />
  <n-divider vertical />
  <MarkdownToolbarButton
    :label="t('components.markdownToolbar.actions.rating')"
    :icon="StarOutlineFilled"
    @action="formatStar"
  />
  <MarkdownToolbarButton
    :label="t('components.markdownToolbar.actions.collapse')"
    :icon="MenuOpenOutlined"
    @action="formatCollapsibleBlock"
  />
  <MarkdownToolbarButton
    :label="t('components.markdownToolbar.actions.help')"
    :icon="HelpOutlineOutlined"
    @action="() => (showGuideModal = true)"
  />
  <div style="width: 8px" />

  <MarkdownGuideModal v-model:show="showGuideModal" />
</template>
