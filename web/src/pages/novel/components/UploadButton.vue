<script lang="ts" setup>
import { PlusOutlined } from '@vicons/material';
import type {
  UploadCustomRequestOptions,
  UploadFileInfo,
  UploadInst,
} from 'naive-ui';
import { useI18n } from 'vue-i18n';

import { formatError } from '@/api';
import { WenkuNovelRepo } from '@/repos';
import { useNoticeStore, useWhoamiStore } from '@/stores';
import { RegexUtil } from '@/util';
import { getFullContent } from '@/util/file';

const { t } = useI18n();

const props = defineProps<{
  novelId: string;
  allowZh: boolean;
}>();

const message = useMessage();

const whoamiStore = useWhoamiStore();
const { whoami } = storeToRefs(whoamiStore);

async function beforeUpload({ file }: { file: UploadFileInfo }) {
  if (!whoami.value.isSignedIn) {
    message.info(t('uploadButton.loginRequired'));
    return false;
  }
  if (!file.file) {
    return false;
  }
  if (
    ['jp', 'zh', 'zh-jp', 'jp-zh'].some((prefix) =>
      file.file!.name.startsWith(prefix),
    )
  ) {
    message.error(t('uploadButton.noMachineFile'));
    return false;
  }
  if (file.file.size > 1024 * 1024 * 40) {
    message.error(t('uploadButton.fileTooLarge'));
    return false;
  }

  let content: string;
  try {
    content = await getFullContent(file.file);
  } catch (e) {
    console.error(e);
    message.error(t('uploadButton.parseError', { error: e }));
    return false;
  }
  const charsCount = RegexUtil.countLanguageCharacters(content);
  if (charsCount.total < 500) {
    message.error(t('uploadButton.tooFewChars'));
    return false;
  }

  const p = (charsCount.jp + charsCount.ko) / charsCount.total;
  if (p < 0.33) {
    if (!props.allowZh) {
      message.error(t('uploadButton.chineseNotAllowed'));
      return false;
    } else {
      file.url = 'zh';
    }
  } else {
    file.url = 'jp';
  }
}

const customRequest = async ({
  file,
  onFinish,
  onError,
  onProgress,
}: UploadCustomRequestOptions) => {
  if (!whoami.value.isSignedIn) {
    onError();
    return;
  }

  try {
    const type = file.url === 'jp' ? 'jp' : 'zh';
    await WenkuNovelRepo.createVolume(
      props.novelId,
      file.name,
      type,
      file.file as File,
      (percent) => onProgress({ percent }),
    );
    onFinish();
  } catch (e) {
    onError();
    message.error(t('uploadButton.uploadFailed', { error: await formatError(e) }));
  }
};

const noticeStore = useNoticeStore();
const { noticed } = storeToRefs(noticeStore);

const showRuleModal = ref(false);
const haveReadRule = computed(() => {
  const durationSinceLastRead = Date.now() - noticed.value.wenkuUploadRule;
  return durationSinceLastRead < 24 * 3600 * 1000;
});
const uploadRef = useTemplateRef<UploadInst>('upload');
const uploadVolumes = () => {
  showRuleModal.value = true;
  noticed.value.wenkuUploadRule = Date.now();
};
</script>

<template>
  <c-button
    v-if="!haveReadRule"
    :label="t('novel.uploadButton.upload')"
    :icon="PlusOutlined"
    @action="uploadVolumes"
  />
  <n-upload
    ref="upload"
    accept=".txt,.epub"
    multiple
    :custom-request="customRequest"
    :show-trigger="haveReadRule"
    @before-upload="beforeUpload"
  >
    <c-button :label="t('novel.uploadButton.upload')" :icon="PlusOutlined" />
  </n-upload>

  <c-modal
    :title="t('uploadButton.rules.title')"
    v-model:show="showRuleModal"
    @after-leave="uploadRef?.openOpenFileDialog()"
  >
    <n-p>{{ t('uploadButton.rules.intro') }}</n-p>
    <n-ul>
      <n-li>
        {{ t('uploadButton.rules.rule1') }}
      </n-li>
      <n-li>{{ t('uploadButton.rules.rule2') }}</n-li>
      <n-li>{{ t('uploadButton.rules.rule3') }}</n-li>
      <n-li>{{ t('uploadButton.rules.rule4') }}</n-li>
    </n-ul>
    <n-p>{{ t('uploadButton.rules.note') }}</n-p>

    <template #action>
      <c-button :label="t('novel.uploadButton.confirm')" type="primary" @action="showRuleModal = false" />
    </template>
  </c-modal>
</template>
