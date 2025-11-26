<script lang="ts" setup>
import {
  BookOutlined,
  ForumOutlined,
  LanguageOutlined,
  ReadMoreOutlined,
  StarBorderOutlined,
} from '@vicons/material';
import { useI18n } from 'vue-i18n';

import { FavoredApi } from '@/api';
import { WebNovelRepo, WenkuNovelRepo } from '@/repos';
import bannerUrl from '@/image/banner.webp';
import type { WebNovelOutlineDto } from '@/model/WebNovel';
import { useBreakPoints } from '@/pages/util';
import { useWhoamiStore } from '@/stores';
import { WebUtil } from '@/util/web';


const bp = useBreakPoints();
const showShortcut = bp.smaller('tablet');

const router = useRouter();
const vars = useThemeVars();

const whoamiStore = useWhoamiStore();
const { whoami } = storeToRefs(whoamiStore);
const { t, locale } = useI18n();

// Check if locale is ready before rendering
const isLocaleReady = computed(() => {
  const currentLocale = locale.value;
  return currentLocale === 'vi' || currentLocale === 'zh';
});

const url = ref('');
const query = (url: string) => {
  if (url.length === 0) return;
  const parseResult = WebUtil.parseUrl(url);
  if (parseResult !== undefined) {
    const { providerId, novelId } = parseResult;
    router.push({ path: `/novel/${providerId}/${novelId}` });
  } else {
    router.push({ path: '/novel', query: { query: url } });
  }
};

const favoriteList = ref<{
  data?: WebNovelOutlineDto[];
  error: Error | null;
}>({ error: null });
const loadFavorite = async () => {
  try {
    const data = await FavoredApi.listFavoredWebNovel('default', {
      page: 0,
      pageSize: 8,
      query: '',
      provider: 'kakuyomu,syosetu,novelup,hameln,pixiv,alphapolis',
      type: 0,
      level: 0,
      translate: 0,
      sort: 'update',
    }).then((it) => it.items);
    favoriteList.value = { data, error: null };
  } catch (e) {
    favoriteList.value = { error: e as Error };
  }
};
watch(
  () => whoami.value.isSignedIn,
  (isSignedIn) => {
    if (isSignedIn) {
      loadFavorite();
    }
  },
  { immediate: true },
);

const { data: mostVisitedWeb, error: mostVisitedWebError } =
  WebNovelRepo.useWebNovelList(1, {
    provider: 'kakuyomu,syosetu,novelup,hameln,pixiv,alphapolis',
    sort: 1,
    level: 1,
  });

const { data: latestUpdateWenku, error: latestUpdateWenkuError } =
  WenkuNovelRepo.useWenkuNovelList(1, { level: 1 });

const showHowToUseModal = ref(false);
const linkExample = [
  ['Kakuyomu', 'https://kakuyomu.jp/works/16817139555217983105'],
  [
    '成为小说家吧',
    'https://ncode.syosetu.com/n0833hi <br /> https://novel18.syosetu.com/n3192gh',
  ],
  ['Novelup', 'https://novelup.plus/story/206612087'],
  ['Hameln', 'https://syosetu.org/novel/297874/'],
  [
    'Pixiv系列/短篇',
    'https://www.pixiv.net/novel/series/9406879 <br/> https://www.pixiv.net/novel/show.php?id=18304868',
  ],
  ['Alphapolis', 'https://www.alphapolis.co.jp/novel/638978238/525733370'],
];

const showQQModal = ref(false);
const qqLink =
  'http://qm.qq.com/cgi-bin/qm/qr?_wv=1027&k=Qa0SOMBYZoJZ4vuykz3MbPS0zbpeN0pW&authKey=q75E7fr5CIBSDhqX%2F4kuC%2B0mcPiDvj%2FSDfP%2FGZ8Rl8kDn6Z3M6XPSZ91yt4ZWonq&noverify=0&group_code=819513328';

const telegramLink = 'https://t.me/+Mphy0wV4LYZkNTI1';
const githubLink = 'https://github.com/auto-novel/auto-novel';
</script>

<template>
  <div v-if="isLocaleReady">
  <div
    :style="{ background: `rgba(0, 0, 0, .25) url(${bannerUrl})` }"
    style="background-blend-mode: darken"
  >
    <div id="banner" class="layout-content">
      <n-h1
        style="
          text-align: center;
          font-size: 3em;
          color: white;
          filter: drop-shadow(0.05em 0.05em black);
        "
      >
        {{ t('home.bannerTitle') }}
      </n-h1>
      <n-input-group>
        <n-input
          v-model:value="url"
          size="large"
          :placeholder="t('home.urlPlaceholder')"
          :input-props="{ spellcheck: false }"
          @keyup.enter="query(url)"
          :style="{ 'background-color': vars.bodyColor }"
        />
        <n-button size="large" type="primary" @click="query(url)">
          {{ t('common.search') }}
        </n-button>
      </n-input-group>
    </div>
  </div>

  <div class="layout-content">
    <n-flex
      v-if="showShortcut"
      :size="0"
      justify="space-around"
      :wrap="false"
      style="margin: 8px 0px"
    >
      <router-link
        :to="whoami.isSignedIn ? '/favorite/web' : '/favorite/local'"
        style="flex: 1"
      >
        <n-button quaternary style="width: 100%; height: 64px">
          <n-flex align="center" vertical style="font-size: 12px">
            <n-icon size="24" :component="StarBorderOutlined" />
            {{ t('home.favoriteShortcut') }}
          </n-flex>
        </n-button>
      </router-link>

      <router-link to="/novel" style="flex: 1">
        <n-button quaternary style="width: 100%; height: 64px">
          <n-flex align="center" vertical style="font-size: 12px">
            <n-icon size="24" :component="LanguageOutlined" />
            {{ t('home.webShortcut') }}
          </n-flex>
        </n-button>
      </router-link>

      <router-link to="/wenku" style="flex: 1">
        <n-button quaternary style="width: 100%; height: 64px">
          <n-flex align="center" vertical style="font-size: 12px">
            <n-icon size="24" :component="BookOutlined" />
            {{ t('home.wenkuShortcut') }}
          </n-flex>
        </n-button>
      </router-link>

      <router-link to="/forum" style="flex: 1">
        <n-button quaternary style="width: 100%; height: 64px">
          <n-flex align="center" vertical style="font-size: 12px">
            <n-icon size="24" :component="ForumOutlined" />
            {{ t('home.forumShortcut') }}
          </n-flex>
        </n-button>
      </router-link>
    </n-flex>
    <div v-else style="height: 16px" />

    <bulletin>
      <n-flex>
        <n-button text type="primary" @click="showHowToUseModal = true">
          {{ t('common.howToUse') }}
        </n-button>
        /
        <n-button text type="primary" @click="showQQModal = true">
          {{ t('common.qqGroup') }}
        </n-button>
        /
        <n-a :href="telegramLink" target="_blank">
          {{ t('common.telegram') }}
        </n-a>
        /
        <n-a :href="githubLink" target="_blank">
          {{ t('common.github') }}
        </n-a>
      </n-flex>
      <n-p>
        {{ t('home.bulletinWarning') }}
      </n-p>
      <n-p>
        {{ t('home.bulletinNotice') }}
      </n-p>
    </bulletin>

    <template v-if="whoami.isSignedIn">
      <section-header :title="t('home.favoriteSection')">
        <router-link to="/favorite/web">
          <c-button :label="t('common.more')" :icon="ReadMoreOutlined" />
        </router-link>
      </section-header>
      <PanelWebNovel
        :novels="favoriteList?.data?.slice(0, 8)"
        :error="favoriteList?.error"
      />
      <n-divider />
    </template>

    <section-header :title="t('home.webMostVisited')">
      <router-link to="/novel">
        <c-button :label="t('common.more')" :icon="ReadMoreOutlined" />
      </router-link>
    </section-header>
    <PanelWebNovel
      :novels="mostVisitedWeb?.items?.slice(0, 8)"
      :error="mostVisitedWebError"
    />
    <n-divider />

    <section-header :title="t('home.wenkuLatest')">
      <router-link to="/wenku">
        <c-button :label="t('common.more')" :icon="ReadMoreOutlined" />
      </router-link>
    </section-header>
    <PanelWenkuNovel
      :novels="latestUpdateWenku?.items?.slice(0, 12)"
      :error="latestUpdateWenkuError"
    />
    <n-divider />
  </div>

  <c-modal :title="t('home.modalHowToTitle')" v-model:show="showHowToUseModal">
    <n-p>
      {{ t('home.modalHowToIntro') }}
    </n-p>
    <n-p>
      {{ t('home.modalHowToAdvanced') }}
      <c-a to="/forum/64f3d63f794cbb1321145c07">
        {{ t('home.tutorialLinkText') }}
      </c-a>
      . {{ t('home.modalHowToForum') }}
      <c-a to="/forum">{{ t('home.forumLinkText') }}</c-a>
      .
    </n-p>
    <n-p>{{ t('home.modalSupportedTitle') }}</n-p>
    <n-p v-for="[name, link] of linkExample" :key="name">
      <b>{{ name }}</b>
      <br />
      <!-- eslint-disable-next-line vue/no-v-html -->
      <span v-html="link" />
    </n-p>
  </c-modal>

  <c-modal :title="t('home.modalQQTitle')" v-model:show="showQQModal">
    <n-p>
      {{ t('home.qqGroupLabel') }}
      <n-a :href="qqLink" target="_blank">819513328</n-a>
      ，{{ t('home.qqGroupVerify') }}
      <br />
      <n-qr-code :size="150" :value="qqLink" />
    </n-p>
  </c-modal>
  </div>
</template>

<style scoped>
#banner {
  max-width: 800px;
  padding-top: 20px;
  padding-bottom: 50px;
}
@media only screen and (max-width: 600px) {
  #banner {
    padding-top: 10px;
    padding-bottom: 35px;
  }
}
</style>
