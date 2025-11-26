<script lang="ts" setup>
import { useI18n } from 'vue-i18n';

import type { ParsedFile, Txt } from '@/util/file';
import { RegexUtil } from '@/util';

import { Toolbox } from './Toolbox';

const props = defineProps<{
  files: ParsedFile[];
}>();

const message = useMessage();
const { t } = useI18n();

const fixOcrForTxt = async (txt: Txt) => {
  const endsCorrectly = (s: string) => {
    if (s.length === 0) {
      return true;
    }
    const lastChar = s.charAt(s.length - 1);
    if (
      lastChar === '，' ||
      lastChar === ',' ||
      RegexUtil.hasHanzi(lastChar) ||
      RegexUtil.hasKanaChars(lastChar) ||
      RegexUtil.hasHangulChars(lastChar) ||
      RegexUtil.hasEnglishChars(lastChar)
    ) {
      return false;
    } else {
      return true;
    }
  };

  const lines: string[] = [];
  let lineProcessing = '';
  for (let line of txt.text.split('\n')) {
    if (lineProcessing.length > 0) {
      line = lineProcessing + line.trim();
      lineProcessing = '';
    } else {
      line = line.trimEnd();
    }
    if (endsCorrectly(line)) {
      lines.push(line);
    } else {
      lineProcessing = line;
    }
  }
  if (lineProcessing.length > 0) {
    lines.push(lineProcessing);
  }
  txt.text = lines.join('\n');
};

const fixOcr = () =>
  Toolbox.modifyFiles(
    props.files.filter((file) => file.type === 'txt'),
    fixOcrForTxt,
    (e) => message.error(t('workspace.fixOcr.error', { error: String(e) })),
  );
</script>

<template>
  <n-flex vertical>
    {{ t('workspace.fixOcr.description') }}
    <n-flex>
      <c-button :label="t('workspace.fixOcr.action')" @action="fixOcr" />
    </n-flex>
  </n-flex>
</template>
