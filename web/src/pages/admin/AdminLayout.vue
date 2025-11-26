<script lang="ts" setup>
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';

import { useWhoamiStore } from '@/stores';

const { t } = useI18n();

const router = useRouter();
const route = useRoute();

const whoamiStore = useWhoamiStore();
const { whoami } = storeToRefs(whoamiStore);

const path = computed(() => route.path);
const handleUpdateValue = (path: string) => router.push({ path });
</script>

<template>
  <div class="layout-content">
    <template v-if="whoami.isSignedIn">
      <n-h1>{{ t('admin.layout.title') }}</n-h1>
      <n-tabs
        type="line"
        :value="path"
        @update:value="handleUpdateValue"
        style="margin-bottom: 24px"
      >
        <n-tab name="/admin/user">{{ t('admin.layout.user') }}</n-tab>
        <n-tab name="/admin/operation">{{ t('admin.layout.operationHistory') }}</n-tab>
        <n-tab name="/admin/web-toc-merge-history">{{ t('admin.layout.mergeHistory') }}</n-tab>
      </n-tabs>
      <router-view />
    </template>
    <n-result v-else status="error" :title="t('admin.layout.notLoggedIn')" />
  </div>
</template>
