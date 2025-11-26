<script lang="ts" setup>
import { useI18n } from 'vue-i18n';
import { useWhoamiStore } from '@/stores';

const props = defineProps<{
  label?: string;
  icon?: Component;
  requireLogin?: boolean;
  onAction?: (e: MouseEvent) => unknown;
}>();

const message = useMessage();
const { t } = useI18n();

const whoamiStore = useWhoamiStore();
const { whoami } = storeToRefs(whoamiStore);

const running = ref(false);

const onClick = async (e: MouseEvent) => {
  if (!props.onAction) return;

  if (props.requireLogin === true && !whoami.value.isSignedIn) {
    message.info(t('components.cButton.loginRequired'));
    return;
  }
  if (running.value) {
    message.warning(t('components.cButton.processing'));
    return;
  }
  const ret = props.onAction(e);
  if (ret instanceof Promise) {
    try {
      running.value = true;
      await ret;
    } finally {
      running.value = false;
    }
  }
};
</script>

<template>
  <n-button round :loading="running" @click="onClick">
    <template v-if="icon && label" #icon>
      <n-icon :component="icon" />
    </template>
    {{ label }}
    <template v-if="icon && !label">
      <n-icon :component="icon" />
    </template>
  </n-button>
</template>
