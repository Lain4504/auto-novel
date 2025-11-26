<script lang="ts" setup>
import { useI18n } from 'vue-i18n';

import type { Epub, ParsedFile } from '@/util/file';
import { StandardNovel } from '@/util/file';
import { Toolbox } from './Toolbox';

const props = defineProps<{
  files: ParsedFile[];
}>();

const message = useMessage();
const { t } = useI18n();

const convertEpubToTxt = async (epub: Epub) => {
  const novel = StandardNovel.fromEpub(epub);
  return await StandardNovel.toTxt(novel);
};

const convertAll = () =>
  Toolbox.convertFiles(
    props.files.filter((file) => file.type === 'epub'),
    convertEpubToTxt,
    (e) => message.error(t('workspace.convert.error', { error: String(e) })),
  );
</script>

<template>
  <n-flex vertical>
    <n-flex>
      <c-button
        :label="t('workspace.convert.action')"
        size="small"
        @action="convertAll"
      />
    </n-flex>
  </n-flex>
</template>
