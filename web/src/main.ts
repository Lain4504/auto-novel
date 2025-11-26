import { PiniaColada } from '@pinia/colada';
import { createPinia } from 'pinia';
import { createApp, watch } from 'vue';

import App from './App.vue';
import router from './router';
import { createAppI18n } from './locales';
import { useLocaleStore } from './stores';

const app = createApp(App);

const pinia = createPinia();
app.use(pinia);
app.use(PiniaColada, {
  queryOptions: {
    gcTime: 3600_000,
    refetchOnMount: true,
    refetchOnWindowFocus: false,
    refetchOnReconnect: true,
  },
});

const localeStore = useLocaleStore(pinia);
const { locale: storeLocale } = storeToRefs(localeStore);
const i18n = createAppI18n();

// Immediately sync locale to prevent initialization issues
i18n.global.locale.value = storeLocale.value;

watch(
  storeLocale,
  (value) => {
    i18n.global.locale.value = value;
  },
);

app.use(i18n);
app.use(router);
app.mount('#app');
