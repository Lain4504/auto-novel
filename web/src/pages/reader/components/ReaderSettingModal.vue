<script lang="ts" setup>
import { checkIsMobile, useIsWideScreen } from '@/pages/util';
import { ReaderSetting, useReaderSettingStore } from '@/stores';
import { AddOutlined, MinusOutlined } from '@vicons/material';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

const isMobile = checkIsMobile();
const isWideScreen = useIsWideScreen(600);

const readerSettingStore = useReaderSettingStore();
const { readerSetting } = storeToRefs(readerSettingStore);

const setCustomBodyColor = (color: string) =>
  (readerSetting.value.theme.bodyColor = color);
const setCustomFontColor = (color: string) =>
  (readerSetting.value.theme.fontColor = color);
const setIndentSize = (diff: number) => {
  readerSetting.value.indentSize = Math.min(
    Math.max(readerSetting.value.indentSize! + diff, 0),
    9,
  );
};
</script>

<template>
  <c-modal content-style="padding: 0;" :max-height-percentage="80">
    <n-tabs
      type="line"
      size="large"
      :tabs-padding="20"
      pane-style="padding: 0px;"
      animated
      style="width: 100%"
    >
      <n-tab-pane name="signin" :tab="t('reader.settingModal.contentTab')">
        <n-flex vertical size="large" style="width: 100%; padding: 20px">
          <c-action-wrapper :title="t('reader.settingModal.language')">
            <c-radio-group
              v-model:value="readerSetting.mode"
              :options="ReaderSetting.modeOptions"
            />
          </c-action-wrapper>

          <c-action-wrapper :title="t('reader.settingModal.translation')">
            <n-flex size="large">
              <c-radio-group
                v-model:value="readerSetting.translationsMode"
                :options="ReaderSetting.translationModeOptions"
              />
              <translator-check
                v-model:value="readerSetting.translations"
                show-order
                :two-line="!isWideScreen"
              />
            </n-flex>
          </c-action-wrapper>

          <c-action-wrapper v-if="isMobile" :title="t('reader.settingModal.clickArea')">
            <c-radio-group
              v-model:value="readerSetting.clickArea"
              :options="ReaderSetting.clickAreaOptions"
            />
          </c-action-wrapper>

          <c-action-wrapper :title="t('reader.settingModal.speakLanguage')">
            <c-radio-group
              :value="readerSetting.speakLanguages[0]"
              @update-value="(it: any) => (readerSetting.speakLanguages = [it])"
              :options="ReaderSetting.speakLanguagesOptions"
            />
          </c-action-wrapper>
          <c-action-wrapper :title="t('reader.settingModal.pageTurnMode')">
            <c-radio-group
              :value="readerSetting.pageTurnMode"
              @update-value="(it: any) => (readerSetting.pageTurnMode = it)"
              :options="ReaderSetting.pageTurnModeOptions"
            />
          </c-action-wrapper>
          <c-action-wrapper v-if="isMobile" :title="t('reader.settingModal.clickAnimation')" align="center">
            <n-switch
              v-model:value="readerSetting.enableClickAnimition"
              size="small"
            />
          </c-action-wrapper>
          <c-action-wrapper :title="t('reader.settingModal.showTranslationSource')" align="center">
            <n-switch
              v-model:value="readerSetting.enableSourceLabel"
              size="small"
            />
          </c-action-wrapper>
          <c-action-wrapper :title="t('reader.settingModal.indentFix')" align="center">
            <n-flex size="large" align="center">
              <n-switch
                :value="readerSetting.indentSize !== undefined"
                @update:value="
                  (v) => (readerSetting.indentSize = v ? 2 : undefined)
                "
                size="small"
              />
              <c-action-wrapper
                v-if="readerSetting.indentSize !== undefined"
                :title="t('reader.settingModal.indentValue')"
                align="center"
              >
                <n-input-group>
                  <n-button size="small" @click="setIndentSize(-1)">
                    <template #icon>
                      <n-icon><MinusOutlined /></n-icon>
                    </template>
                  </n-button>
                  <n-input
                    :value="readerSetting.indentSize + t('reader.settingModal.indentChars')"
                    size="small"
                    style="text-align: center; width: 70px"
                    readonly
                  />
                  <n-button size="small" @click="setIndentSize(1)">
                    <template #icon>
                      <n-icon><AddOutlined /></n-icon>
                    </template>
                  </n-button>
                </n-input-group>
              </c-action-wrapper>
            </n-flex>
          </c-action-wrapper>

          <n-text depth="3" style="font-size: 12px">
            {{ t('reader.settingModal.keyboardHint') }}
          </n-text>
        </n-flex>
      </n-tab-pane>

      <n-tab-pane name="signup" :tab="t('reader.settingModal.styleTab')">
        <n-flex vertical size="large" style="width: 100%; padding: 20px">
          <c-action-wrapper :title="t('reader.settingModal.fontWeight')">
            <c-radio-group
              v-model:value="readerSetting.fontWeight"
              :options="ReaderSetting.fontWeightOptions"
            />
          </c-action-wrapper>

          <c-action-wrapper :title="t('reader.settingModal.fontSize')" align="center">
            <n-slider
              v-model:value="readerSetting.fontSize"
              :min="14"
              :max="40"
              style="flex: auto"
              :format-tooltip="(value: number) => `${value}px`"
            />
            <n-text style="width: 6em">{{ readerSetting.fontSize }}px</n-text>
          </c-action-wrapper>

          <c-action-wrapper :title="t('reader.settingModal.lineHeight')" align="center">
            <n-slider
              v-model:value="readerSetting.lineSpace"
              :step="0.1"
              :min="0"
              :max="2"
              style="flex: auto"
              :format-tooltip="(value: number) => value.toFixed(1)"
            />
            <n-text style="width: 6em">
              {{ readerSetting.lineSpace.toFixed(1) }}
            </n-text>
          </c-action-wrapper>

          <c-action-wrapper :title="t('reader.settingModal.pageWidth')" align="center">
            <n-slider
              v-model:value="readerSetting.pageWidth"
              :step="50"
              :min="600"
              :max="1200"
              style="flex: auto"
              :format-tooltip="(value: number) => `${value}px`"
            />
            <n-text style="width: 6em">{{ readerSetting.pageWidth }}px</n-text>
          </c-action-wrapper>

          <c-action-wrapper :title="t('reader.settingModal.underline')">
            <c-radio-group
              v-model:value="readerSetting.textUnderline"
              :options="ReaderSetting.textUnderlineOptions"
            />
          </c-action-wrapper>

          <c-action-wrapper :title="t('reader.settingModal.theme')">
            <n-flex size="large" vertical>
              <c-radio-group
                v-model:value="readerSetting.theme.mode"
                :options="ReaderSetting.themeModeOptions"
              />
              <template v-if="readerSetting.theme.mode === 'custom'">
                <n-flex>
                  <n-radio
                    v-for="theme of ReaderSetting.themeOptions"
                    :key="theme.bodyColor"
                    :checked="theme.bodyColor == readerSetting.theme.bodyColor"
                    @update:checked="
                      readerSetting.theme = { mode: 'custom', ...theme }
                    "
                  >
                    <n-tag
                      :color="{
                        color: theme.bodyColor,
                        textColor: theme.fontColor,
                      }"
                      :style="{
                        width: isWideScreen ? '5.5em' : '2em',
                      }"
                    >
                      {{ isWideScreen ? theme.bodyColor : '#' }}
                    </n-tag>
                  </n-radio>
                </n-flex>
                <n-divider style="margin: 0px" />
                <n-flex>
                  <n-color-picker
                    :modes="['hex']"
                    :show-alpha="false"
                    :default-value="readerSetting.theme.bodyColor"
                    :on-complete="setCustomBodyColor"
                    style="width: 8.2em"
                  >
                    <template #label="color">{{ t('reader.settingModal.background', { color }) }}</template>
                  </n-color-picker>
                  <n-color-picker
                    :modes="['hex']"
                    :show-alpha="false"
                    :default-value="readerSetting.theme.fontColor"
                    :on-complete="setCustomFontColor"
                    style="width: 8.2em"
                  >
                    <template #label="color">{{ t('reader.settingModal.text', { color }) }}</template>
                  </n-color-picker>
                </n-flex>
              </template>
            </n-flex>
          </c-action-wrapper>

          <c-action-wrapper :title="t('reader.settingModal.mainOpacity')" align="center">
            <n-slider
              v-model:value="readerSetting.mixZhOpacity"
              :max="1"
              :min="0"
              :step="0.05"
              :format-tooltip="
                (value: number) => `${(value * 100).toFixed(0)}%`
              "
              style="flex: auto"
            />
            <n-text style="width: 6em">
              {{ (readerSetting.mixZhOpacity * 100).toFixed(0) }}%
            </n-text>
          </c-action-wrapper>

          <c-action-wrapper :title="t('reader.settingModal.auxOpacity')" align="center">
            <n-slider
              v-model:value="readerSetting.mixJpOpacity"
              :max="1"
              :min="0"
              :step="0.05"
              :format-tooltip="
                (value: number) => `${(value * 100).toFixed(0)}%`
              "
            />
            <n-text style="width: 6em">
              {{ (readerSetting.mixJpOpacity * 100).toFixed(0) }}%
            </n-text>
          </c-action-wrapper>
        </n-flex>
      </n-tab-pane>
    </n-tabs>
  </c-modal>
</template>
