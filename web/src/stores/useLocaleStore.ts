import { DEFAULT_LOCALE, availableLocales } from '@/locales';
import type { SupportedLocale } from '@/locales';
import { defineStore } from 'pinia';
import { ref } from 'vue';

const STORAGE_KEY = 'auto-novel-locale';

export const useLocaleStore = defineStore('locale', () => {
  const locale = ref<SupportedLocale>(DEFAULT_LOCALE);

  const persist = (value: SupportedLocale) => {
    if (typeof window === 'undefined') return;
    window.localStorage.setItem(STORAGE_KEY, value);
  };

  const hydrate = () => {
    if (typeof window === 'undefined') return;
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return;

    // Migrate old locale values
    let migratedLocale = raw;
    if (raw === 'zh-cn' || raw === 'zh-tw') {
      migratedLocale = 'zh';
      window.localStorage.setItem(STORAGE_KEY, migratedLocale);
    }

    if (availableLocales.includes(migratedLocale as SupportedLocale)) {
      locale.value = migratedLocale as SupportedLocale;
    }
  };

  hydrate();

  const setLocale = (value: SupportedLocale) => {
    if (!availableLocales.includes(value)) return;
    locale.value = value;
    persist(value);
  };

  if (typeof window !== 'undefined') {
    window.addEventListener('storage', (event) => {
      if (event.key === STORAGE_KEY && event.newValue) {
        const next = event.newValue as SupportedLocale;
        if (availableLocales.includes(next)) {
          locale.value = next;
        }
      }
    });
  }

  return {
    locale,
    setLocale,
    availableLocales,
  };
});
