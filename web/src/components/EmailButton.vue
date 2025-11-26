<script lang="ts" setup>
import { useI18n } from 'vue-i18n';

import { formatError } from '@/api';

const message = useMessage();

const props = defineProps<{
  label: string;
  allowSendEmail: () => boolean;
  sendEmail: () => Promise<unknown>;
}>();

type VerifyState =
  | { state: 'sending' }
  | { state: 'cooldown'; seconds: number }
  | undefined;

const verifyState = ref<VerifyState>(undefined);

const { t } = useI18n();

const verifyButtonLabel = computed(() => {
  if (verifyState.value === undefined) {
    return props.label;
  } else if (verifyState.value.state === 'sending') {
    return t('components.emailButton.sending');
  } else {
    return t('components.emailButton.cooldown', {
      seconds: verifyState.value.seconds,
    });
  }
});

async function realSendEmail() {
  if (verifyState.value !== undefined) return;
  if (!props.allowSendEmail) return;

  verifyState.value = { state: 'sending' };
  await props
    .sendEmail()
    .then(() => {
      verifyState.value = { state: 'cooldown', seconds: 60 };
      message.info(t('components.emailButton.sent'));

      const timer = window.setInterval(() => {
        if (
          verifyState.value?.state === 'cooldown' &&
          verifyState.value.seconds > 0
        ) {
          verifyState.value.seconds--;
        } else {
          verifyState.value = undefined;
          window.clearInterval(timer);
        }
      }, 1000);
    })
    .catch(async (e) => {
      verifyState.value = undefined;
      const reason = await formatError(e);
      message.error(t('components.emailButton.failed', { reason }));
    });
}
</script>

<template>
  <n-button
    type="primary"
    :disabled="verifyState !== undefined"
    @click="realSendEmail()"
    style="width: 100px"
  >
    {{ verifyButtonLabel }}
  </n-button>
</template>
