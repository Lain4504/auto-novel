<script lang="ts" setup>
import { useI18n } from 'vue-i18n';
import SoundAllTaskCompleted from '@/sound/all_task_completed.mp3';
import {
  Setting,
  useSettingStore,
  useWebSearchHistoryStore,
  useWenkuSearchHistoryStore,
  useLocaleStore,
} from '@/stores';
import { InfoOutlined } from '@vicons/material';

const message = useMessage();
const { t } = useI18n();

const settingStore = useSettingStore();
const { setting } = storeToRefs(settingStore);

const localeStore = useLocaleStore();
const { locale: uiLocale } = storeToRefs(localeStore);

const clearWebSearchHistory = () => {
  useWebSearchHistoryStore().clear();
  message.success(t('setting.clearSuccess'));
};

const clearWenkuSearchHistory = () => {
  useWenkuSearchHistoryStore().clear();
  message.success(t('setting.clearSuccess'));
};

const playSound = (source: string) => {
  return new Audio(source).play();
};
</script>

<template>
  <div class="layout-content">
    <n-h1>{{ t('setting.title') }}</n-h1>

    <n-list bordered>
      <n-list-item>
        <n-flex vertical>
          <b>{{ t('setting.theme') }}</b>
          <c-radio-group
            v-model:value="setting.theme"
            :options="Setting.themeOptions"
            size="small"
          />
        </n-flex>
      </n-list-item>

      <n-list-item>
        <n-flex vertical>
          <b>{{ t('setting.shortcuts') }}</b>
          <n-ul>
            <n-li>{{ t('setting.shortcutListPage') }}</n-li>
            <n-li>{{ t('setting.shortcutQueueButton') }}</n-li>
            <n-li>{{ t('setting.shortcutReaderNav') }}</n-li>
            <n-li>{{ t('setting.shortcutReaderTranslate') }}</n-li>
          </n-ul>
        </n-flex>
      </n-list-item>

      <n-list-item>
        <n-flex vertical>
          <b>{{ t('setting.webNovelToc') }}</b>
          <n-checkbox v-model:checked="setting.tocCollapseInNarrowScreen">
            {{ t('setting.tocCollapseInNarrowScreen') }}
          </n-checkbox>
          <n-checkbox v-model:checked="setting.tocExpandAll">
            {{ t('setting.tocExpandAll') }}
            <n-tooltip trigger="hover" placement="top" style="max-width: 400px">
              <template #trigger>
                <n-button text @click.stop>
                  <n-icon depth="4" :component="InfoOutlined" size="12" />
                </n-button>
              </template>
              {{ t('setting.tocExpandAllTooltip') }}
            </n-tooltip>
          </n-checkbox>
          <b>{{ t('setting.comments') }}</b>
          <n-checkbox v-model:checked="setting.hideCommmentWebNovel">
            {{ t('setting.hideCommentWebNovel') }}
          </n-checkbox>
          <n-checkbox v-model:checked="setting.hideCommmentWenkuNovel">
            {{ t('setting.hideCommentWenkuNovel') }}
          </n-checkbox>
          <b>{{ t('setting.favorites') }}</b>
          <n-checkbox v-model:checked="setting.showTagInWebFavored">
            {{ t('setting.showTagInWebFavored') }}
          </n-checkbox>
          <n-checkbox v-model:checked="setting.favoriteCreateTimeFirst">
            {{ t('setting.favoriteCreateTimeFirst') }}
          </n-checkbox>
        </n-flex>
      </n-list-item>

      <n-list-item>
        <n-flex vertical>
          <b>{{ t('setting.workspace') }}</b>
          <n-checkbox v-model:checked="setting.autoTopJobWhenAddTask">
            {{ t('setting.autoTopJobWhenAddTask') }}
          </n-checkbox>
        </n-flex>
      </n-list-item>

      <n-list-item>
        <n-flex vertical>
          <b>{{ t('setting.paginationMode') }}</b>
          <c-radio-group
            v-model:value="setting.paginationMode"
            :options="Setting.paginationModeOptions"
            size="small"
          />
        </n-flex>
      </n-list-item>

      <n-list-item>
        <n-flex vertical>
          <b>{{ t('setting.enabledTranslator') }}</b>
          <translator-check
            v-model:value="setting.enabledTranslator"
            size="small"
          />
        </n-flex>
      </n-list-item>

      <n-list-item>
        <n-flex vertical>
          <b>{{ t('setting.workspaceSound') }}</b>
          <n-flex :wrap="false" :size="0">
            <n-checkbox v-model:checked="setting.workspaceSound">
              {{ t('setting.workspaceSoundAllCompleted') }}
            </n-checkbox>

            [
            <c-button
              :label="t('setting.clickPlay')"
              text
              type="primary"
              @action="playSound(SoundAllTaskCompleted)"
            />
            ]
          </n-flex>
        </n-flex>
      </n-list-item>

      <n-list-item>
        <n-flex vertical align="start">
          <b>{{ t('setting.clearSearchHistory') }}</b>
          <n-flex>
            <c-button
              :label="t('setting.clearWebSearchHistory')"
              size="small"
              @action="clearWebSearchHistory"
            />
            <c-button
              :label="t('setting.clearWenkuSearchHistory')"
              size="small"
              @action="clearWenkuSearchHistory"
            />
          </n-flex>
        </n-flex>
      </n-list-item>

      <n-list-item>
        <n-flex vertical>
          <b>{{ t('setting.blacklist') }}</b>
          <n-flex>
            <user-block-button />
          </n-flex>
        </n-flex>
      </n-list-item>


      <n-list-item>
        <n-flex vertical>
          <b>{{ t('stores.setting.uiLanguage.title') }}</b>
          <c-radio-group
            v-model:value="uiLocale"
            :options="[
              { label: t('stores.setting.uiLanguage.vi'), value: 'vi' },
              { label: t('stores.setting.uiLanguage.zh'), value: 'zh' }
            ]"
            size="small"
          />
        </n-flex>
      </n-list-item>
    </n-list>
  </div>
</template>
