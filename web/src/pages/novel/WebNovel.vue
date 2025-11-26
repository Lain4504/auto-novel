<script lang="ts" setup>
import { formatError } from '@/api';
import { WebNovelRepo } from '@/repos';
import { useIsWideScreen } from '@/pages/util';
import { computedAsync } from '@vueuse/core';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

const { providerId, novelId } = defineProps<{
  providerId: string;
  novelId: string;
}>();

const isWideScreen = useIsWideScreen();
const router = useRouter();

const { data: novel, error } = WebNovelRepo.useWebNovel(providerId, novelId);

watch(novel, (novel) => {
  if (novel) {
    document.title = novel.titleJp;
  }
});

const formatedError = computedAsync(async () => {
  if (!error.value) return '';
  const message = await formatError(error.value);
  return message;
});

watch(formatedError, async (error) => {
  const invalidIdMessage = t('novel.webNovel.invalidIdMessage');
  if (error.includes(invalidIdMessage)) {
    const targetNovelPath = error.split(invalidIdMessage)[1];
    router.push({ path: `/novel${targetNovelPath}` });
  }
});
</script>

<template>
  <div class="layout-content">
    <template v-if="novel">
      <web-novel-wide
        v-if="isWideScreen"
        :provider-id="providerId"
        :novel-id="novelId"
        :novel="novel"
      />
      <web-novel-narrow
        v-else
        :provider-id="providerId"
        :novel-id="novelId"
        :novel="novel"
      />
    </template>

    <n-result
      v-else-if="error"
      status="error"
      :title="t('novel.webNovel.loadError')"
      :description="formatedError"
    />
  </div>
</template>
