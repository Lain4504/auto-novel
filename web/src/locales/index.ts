import { createI18n } from 'vue-i18n';

import type { AppMessages } from './vi';
import vi from './vi';
import zh from './zh';

export type SupportedLocale = 'vi' | 'zh';

export const DEFAULT_LOCALE: SupportedLocale = 'vi';
export const FALLBACK_LOCALE: SupportedLocale = 'zh';

const messages: Record<SupportedLocale, AppMessages> = {
  vi,
  zh,
};

const appI18n = createI18n({
  legacy: false,
  locale: DEFAULT_LOCALE,
  fallbackLocale: FALLBACK_LOCALE,
  messages,
});

export const createAppI18n = () => appI18n;
export const i18nGlobal = appI18n.global;

export const availableLocales: SupportedLocale[] = Object.keys(
  messages,
) as SupportedLocale[];
