import type { TranslatorId } from '@/model/Translator';
import { i18nGlobal } from '@/locales';
import { defaultConverter, useLocalStorage, useOpenCC } from '@/util';
import { LSKey } from './key';

export interface Setting {
  theme: 'light' | 'dark' | 'system';
  enabledTranslator: TranslatorId[];
  tocSortReverse: boolean;
  //
  tocCollapseInNarrowScreen: boolean;
  tocExpandAll: boolean;
  hideCommmentWebNovel: boolean;
  hideCommmentWenkuNovel: boolean;
  showTagInWebFavored: boolean;
  favoriteCreateTimeFirst: boolean;
  //
  autoTopJobWhenAddTask: boolean;
  //
  menuCollapsed: boolean;
  //
  downloadFilenameType: 'jp' | 'vi';
  downloadFormat: {
    mode: 'vi' | 'vi-jp' | 'jp-vi';
    translationsMode: 'parallel' | 'priority';
    translations: TranslatorId[];
    type: 'epub' | 'txt';
  };
  workspaceSound: boolean;
  paginationMode: 'pagination' | 'scroll';
  localVolumeOrder: {
    value: 'byCreateAt' | 'byReadAt' | 'byId';
    desc: boolean;
  };
  //
  locale: 'vi' | 'zh';
  searchLocaleAware: boolean;
}

export namespace Setting {
  export const defaultValue: Setting = {
    theme: 'system',
    enabledTranslator: ['baidu', 'youdao', 'gpt', 'sakura'],
    tocSortReverse: false,
    //
    tocCollapseInNarrowScreen: true,
    tocExpandAll: true,
    hideCommmentWebNovel: false,
    hideCommmentWenkuNovel: false,
    showTagInWebFavored: false,
    favoriteCreateTimeFirst: false,
    //
    autoTopJobWhenAddTask: false,
    //
    menuCollapsed: false,
    //
    downloadFilenameType: 'vi',
    downloadFormat: {
      mode: 'vi-jp',
      translationsMode: 'priority',
      translations: ['sakura', 'gpt', 'youdao', 'baidu'],
      type: 'epub',
    },
    workspaceSound: false,
    paginationMode: 'pagination',
    localVolumeOrder: {
      value: 'byCreateAt',
      desc: true,
    },
    //
    locale: 'vi',
    searchLocaleAware: false,
  };

  export const migrate = (setting: Setting) => {
    if ('isDark' in setting && typeof setting.isDark !== undefined) {
      if (setting.isDark === true) {
        setting.theme = 'dark';
      }
      delete setting.isDark;
    }
    if (setting.enabledTranslator === undefined) {
      setting.enabledTranslator = ['baidu', 'youdao', 'gpt', 'sakura'];
    }
    if ((setting.downloadFormat.mode as string) === 'mix') {
      setting.downloadFormat.mode = 'vi-jp';
    } else if ((setting.downloadFormat.mode as string) === 'mix-reverse') {
      setting.downloadFormat.mode = 'jp-vi';
    } else if ((setting.downloadFormat.mode as string) === 'jp') {
      setting.downloadFormat.mode = 'vi';
    }
    // Migrate old zh modes to vi
    if ((setting.downloadFormat.mode as string) === 'zh') {
      setting.downloadFormat.mode = 'vi';
    } else if ((setting.downloadFormat.mode as string) === 'zh-jp') {
      setting.downloadFormat.mode = 'vi-jp';
    } else if ((setting.downloadFormat.mode as string) === 'jp-zh') {
      setting.downloadFormat.mode = 'jp-vi';
    }
    // 2024-03-05
    if (setting.workspaceSound === undefined) {
      setting.workspaceSound = false;
    }
    // 2024-05-28
    if ((setting.paginationMode as unknown) === 'auto') {
      setting.paginationMode = 'pagination';
    }
    // 2025-11-27: Migrate old locale values to new supported locales
    if ((setting.locale as unknown) === 'zh-cn' || (setting.locale as unknown) === 'zh-tw') {
      setting.locale = 'zh';
    }
  };

  export const downloadModeOptions = [
    { label: '', value: 'vi' },
    { label: '', value: 'vi-jp' },
    { label: '', value: 'jp-vi' },
  ];
  export const downloadTranslationModeOptions = [
    { label: '', value: 'priority' },
    { label: '', value: 'parallel' },
  ];
  export const downloadTypeOptions = [
    { label: 'EPUB', value: 'epub' },
    { label: 'TXT', value: 'txt' },
  ];

  export const themeOptions = [
    { label: '', value: 'light' },
    { label: '', value: 'dark' },
    { label: '', value: 'system' },
  ];
  export const paginationModeOptions = [
    { label: '', value: 'pagination' },
    { label: '', value: 'scroll' },
  ];
  export const localVolumeOrderOptions = [
    { value: 'byCreateAt', label: '' },
    { value: 'byReadAt', label: '' },
    { value: 'byId', label: '' },
  ];
  export const localeOptions = [
    { label: '', value: 'vi' },
    { label: '', value: 'zh' },
  ];
}

export interface ReaderSetting {
  mode: 'jp' | 'vi' | 'vi-jp' | 'jp-vi';
  translationsMode: 'parallel' | 'priority';
  translations: TranslatorId[];
  clickArea: 'default' | 'left-right' | 'up-down' | 'none';
  speakLanguages: string[];
  pageTurnMode: 'page' | 'scroll';
  enableClickAnimition: boolean;
  indentSize?: number;
  enableSourceLabel: boolean;
  //
  fontWeight: number;
  fontSize: number;
  lineSpace: number;
  pageWidth: number;
  theme: {
    mode: 'light' | 'dark' | 'system' | 'custom';
    bodyColor: string;
    fontColor: string;
  };
  mixJpOpacity: number;
  mixViOpacity: number;
  textUnderline: 'none' | 'solid' | 'dashed' | 'dotted';
}

export namespace ReaderSetting {
  export const defaultValue: ReaderSetting = {
    mode: 'vi-jp',
    translationsMode: 'priority',
    translations: ['sakura', 'gpt', 'youdao', 'baidu'],
    clickArea: 'default',
    speakLanguages: ['jp'],
    pageTurnMode: 'page',
    enableClickAnimition: true,
    enableSourceLabel: false,
    //
    fontWeight: 400,
    fontSize: 14,
    lineSpace: 1.0,
    pageWidth: 800,
    theme: {
      mode: 'system',
      bodyColor: '#FFFFFF',
      fontColor: '#000000',
    },
    mixJpOpacity: 0.4,
    mixZhOpacity: 0.75,
    textUnderline: 'none',
  };

  export const migrate = (setting: ReaderSetting) => {
    if (typeof setting.fontSize === 'string') {
      setting.fontSize = Number(
        (setting.fontSize as string).replace(/[^0-9]/g, ''),
      );
    }
    if ((setting.mode as unknown) === 'mix') {
      setting.mode = 'vi-jp';
    } else if ((setting.mode as unknown) === 'mix-reverse') {
      setting.mode = 'jp-vi';
    }
    // Migrate old zh modes to vi
    if ((setting.mode as unknown) === 'zh') {
      setting.mode = 'vi';
    } else if ((setting.mode as unknown) === 'zh-jp') {
      setting.mode = 'vi-jp';
    } else if ((setting.mode as unknown) === 'jp-zh') {
      setting.mode = 'jp-vi';
    }
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const theme = setting.theme as any;
    if (theme.isDark !== undefined) {
      if (theme.bodyColor === '#272727' && theme.fontColor === undefined) {
        setting.theme = {
          mode: 'dark',
          bodyColor: '#FFFFFF',
          fontColor: '#000000',
        };
      } else {
        setting.theme = {
          mode: 'light',
          bodyColor: '#FFFFFF',
          fontColor: '#000000',
        };
      }
    }
    if ('trimLeadingSpaces' in setting) {
      if (setting.trimLeadingSpaces) {
        setting.indentSize = 0;
      }
      delete setting.trimLeadingSpaces;
    }
  };

  export const modeOptions = [
    { label: '', value: 'jp' },
    { label: '', value: 'vi' },
    { label: '', value: 'vi-jp' },
    { label: '', value: 'jp-vi' },
  ];
  export const translationModeOptions = [
    { label: '', value: 'priority' },
    { label: '', value: 'parallel' },
  ];

  export const clickAreaOptions = [
    { label: '', value: 'default' },
    { label: '', value: 'left-right' },
    { label: '', value: 'up-down' },
    { label: '', value: 'none' },
  ];

  export const speakLanguagesOptions = [
    { label: '', value: 'vi' },
    { label: '', value: 'jp' },
  ];

  export const pageTurnModeOptions = [
    { label: '', value: 'page' },
    { label: '', value: 'scroll' },
  ];

  export const fontWeightOptions = [
    { label: '', value: 400 },
    { label: '', value: 600 },
  ];

  export const textUnderlineOptions = [
    { label: '', value: 'none' },
    { label: '', value: 'solid' },
    { label: '', value: 'dashed' },
    { label: '', value: 'dotted' },
  ];

  export const themeModeOptions = [
    { label: '', value: 'light' },
    { label: '', value: 'dark' },
    { label: '', value: 'system' },
    { label: '', value: 'custom' },
  ];
  export const themeOptions = [
    { bodyColor: '#FFFFFF', fontColor: '#000000' },
    { bodyColor: '#FFF2E2', fontColor: '#000000' },
    { bodyColor: '#E3EDCD', fontColor: '#000000' },
    { bodyColor: '#E9EBFE', fontColor: '#000000' },
    { bodyColor: '#EAEAEF', fontColor: '#000000' },

    { bodyColor: '#000000', fontColor: '#FFFFFF' },
    { bodyColor: '#272727', fontColor: '#FFFFFF' },
  ];
}

const syncSettingOptionLabels = () => {
  Setting.downloadModeOptions[0].label = i18nGlobal.t(
    'stores.setting.downloadModes.vi',
  );
  Setting.downloadModeOptions[1].label = i18nGlobal.t(
    'stores.setting.downloadModes.zhJp',
  );
  Setting.downloadModeOptions[2].label = i18nGlobal.t(
    'stores.setting.downloadModes.jpZh',
  );
  Setting.downloadTranslationModeOptions[0].label = i18nGlobal.t(
    'stores.setting.translationModes.priority',
  );
  Setting.downloadTranslationModeOptions[1].label = i18nGlobal.t(
    'stores.setting.translationModes.parallel',
  );
  Setting.themeOptions[0].label = i18nGlobal.t('stores.setting.theme.light');
  Setting.themeOptions[1].label = i18nGlobal.t('stores.setting.theme.dark');
  Setting.themeOptions[2].label = i18nGlobal.t('stores.setting.theme.system');
  Setting.paginationModeOptions[0].label = i18nGlobal.t(
    'stores.setting.pagination.pagination',
  );
  Setting.paginationModeOptions[1].label = i18nGlobal.t(
    'stores.setting.pagination.scroll',
  );
  Setting.localVolumeOrderOptions[0].label = i18nGlobal.t(
    'stores.setting.localOrder.created',
  );
  Setting.localVolumeOrderOptions[1].label = i18nGlobal.t(
    'stores.setting.localOrder.read',
  );
  Setting.localVolumeOrderOptions[2].label = i18nGlobal.t(
    'stores.setting.localOrder.title',
  );
  Setting.localeOptions[0].label = i18nGlobal.t('stores.setting.locale.vi');
  Setting.localeOptions[1].label = i18nGlobal.t('stores.setting.locale.zh');

  ReaderSetting.modeOptions[0].label = i18nGlobal.t('stores.reader.mode.jp');
  ReaderSetting.modeOptions[1].label = i18nGlobal.t('stores.reader.mode.vi');
  ReaderSetting.modeOptions[2].label = i18nGlobal.t('stores.reader.mode.viJp');
  ReaderSetting.modeOptions[3].label = i18nGlobal.t('stores.reader.mode.jpVi');
  ReaderSetting.translationModeOptions[0].label = i18nGlobal.t(
    'stores.setting.translationModes.priority',
  );
  ReaderSetting.translationModeOptions[1].label = i18nGlobal.t(
    'stores.setting.translationModes.parallel',
  );
  ReaderSetting.clickAreaOptions[0].label = i18nGlobal.t(
    'stores.reader.clickArea.default',
  );
  ReaderSetting.clickAreaOptions[1].label = i18nGlobal.t(
    'stores.reader.clickArea.leftRight',
  );
  ReaderSetting.clickAreaOptions[2].label = i18nGlobal.t(
    'stores.reader.clickArea.upDown',
  );
  ReaderSetting.clickAreaOptions[3].label = i18nGlobal.t(
    'stores.reader.clickArea.none',
  );
  ReaderSetting.speakLanguagesOptions[0].label = i18nGlobal.t(
    'stores.reader.speak.vi',
  );
  ReaderSetting.speakLanguagesOptions[1].label = i18nGlobal.t(
    'stores.reader.speak.jp',
  );
  ReaderSetting.pageTurnModeOptions[0].label = i18nGlobal.t(
    'stores.reader.pageTurn.page',
  );
  ReaderSetting.pageTurnModeOptions[1].label = i18nGlobal.t(
    'stores.reader.pageTurn.scroll',
  );
  ReaderSetting.fontWeightOptions[0].label = i18nGlobal.t(
    'stores.reader.fontWeight.normal',
  );
  ReaderSetting.fontWeightOptions[1].label = i18nGlobal.t(
    'stores.reader.fontWeight.bold',
  );
  ReaderSetting.textUnderlineOptions[0].label = i18nGlobal.t(
    'stores.reader.underline.none',
  );
  ReaderSetting.textUnderlineOptions[1].label = i18nGlobal.t(
    'stores.reader.underline.solid',
  );
  ReaderSetting.textUnderlineOptions[2].label = i18nGlobal.t(
    'stores.reader.underline.dashed',
  );
  ReaderSetting.textUnderlineOptions[3].label = i18nGlobal.t(
    'stores.reader.underline.dotted',
  );
  ReaderSetting.themeModeOptions[0].label = i18nGlobal.t(
    'stores.reader.themeMode.light',
  );
  ReaderSetting.themeModeOptions[1].label = i18nGlobal.t(
    'stores.reader.themeMode.dark',
  );
  ReaderSetting.themeModeOptions[2].label = i18nGlobal.t(
    'stores.reader.themeMode.system',
  );
  ReaderSetting.themeModeOptions[3].label = i18nGlobal.t(
    'stores.reader.themeMode.custom',
  );
};

export const useSettingStore = defineStore(LSKey.Setting, () => {
  const setting = useLocalStorage<Setting>(LSKey.Setting, Setting.defaultValue);
  Setting.migrate(setting.value);

  const cc = ref(defaultConverter);

  watch(
    () => i18nGlobal.locale.value,
    (locale) => {
      // Only sync if locale is valid
      if (locale === 'vi' || locale === 'zh') {
        try {
          syncSettingOptionLabels();
        } catch (error) {
          console.warn('Failed to sync setting option labels:', error);
        }
      }
    },
    { immediate: true },
  );

  watch(
    () => setting.value.locale,
    async (locale) => {
      cc.value = await useOpenCC(locale);
    },
    { immediate: true },
  );

  return { setting, cc };
});

export const useReaderSettingStore = defineStore(LSKey.SettingReader, () => {
  const readerSetting = useLocalStorage<ReaderSetting>(
    LSKey.SettingReader,
    ReaderSetting.defaultValue,
  );
  ReaderSetting.migrate(readerSetting.value);
  return { readerSetting };
});
