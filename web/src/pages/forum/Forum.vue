<script lang="ts" setup>
import { LockOutlined, PlusOutlined, PushPinOutlined } from '@vicons/material';
import { useI18n } from 'vue-i18n';

import { ArticleRepo } from '@/repos';
import type { ArticleCategory, ArticleSimplified } from '@/model/Article';
import { doAction } from '@/pages/util';
import { useBlacklistStore, useWhoamiStore } from '@/stores';

const { t } = useI18n();

const props = defineProps<{
  page: number;
  category: ArticleCategory;
}>();

const route = useRoute();
const router = useRouter();
const message = useMessage();

const whoamiStore = useWhoamiStore();
const { whoami } = storeToRefs(whoamiStore);

const blacklistStore = useBlacklistStore();

const articleCategoryOptions = computed(() => [
  { value: 'General', label: t('forum.categories.general') },
  { value: 'Guide', label: t('forum.categories.guide') },
  { value: 'Support', label: t('forum.categories.support') },
]);

const onUpdatePage = (page: number) => {
  const query = { ...route.query, page };
  router.push({ path: route.path, query });
};

const onUpdateCategory = (category: ArticleCategory) => {
  const query = { ...route.query, category, page: 1 };
  router.push({ path: route.path, query });
};

const { data: articlePage, error } = ArticleRepo.useArticleList(
  () => props.page,
  () => props.category,
);

const lockArticle = (article: ArticleSimplified) =>
  doAction(
    ArticleRepo.lockArticle(article.id).then(() => (article.locked = true)),
    t('forum.actions.lockAction'),
    message,
  );

const unlockArticle = (article: ArticleSimplified) =>
  doAction(
    ArticleRepo.unlockArticle(article.id).then(() => (article.locked = false)),
    t('forum.actions.unlockAction'),
    message,
  );

const pinArticle = (article: ArticleSimplified) =>
  doAction(
    ArticleRepo.pinArticle(article.id).then(() => (article.pinned = true)),
    t('forum.actions.pinAction'),
    message,
  );

const unpinArticle = (article: ArticleSimplified) =>
  doAction(
    ArticleRepo.unpinArticle(article.id).then(() => (article.pinned = false)),
    t('forum.actions.unpinAction'),
    message,
  );

const hideArticle = (article: ArticleSimplified) =>
  doAction(
    ArticleRepo.hideArticle(article.id).then(() => (article.hidden = true)),
    t('forum.actions.hideAction'),
    message,
  );

const unhideArticle = (article: ArticleSimplified) =>
  doAction(
    ArticleRepo.unhideArticle(article.id).then(() => (article.hidden = false)),
    t('forum.actions.unhideAction'),
    message,
  );

const deleteArticle = (article: ArticleSimplified) =>
  doAction(ArticleRepo.deleteArticle(article.id), t('forum.actions.deleteAction'), message);
</script>

<template>
  <div class="layout-content">
    <n-h1>{{ t('forum.title') }}</n-h1>

    <router-link to="/forum-edit">
      <c-button
        :label="t('forum.publishArticle')"
        :icon="PlusOutlined"
        style="margin-bottom: 16px"
      />
    </router-link>

    <c-action-wrapper :title="t('forum.section')" style="margin-bottom: 20px">
      <c-radio-group
        :value="category"
        @update-value="onUpdateCategory"
        :options="articleCategoryOptions"
      />
    </c-action-wrapper>

    <CPage
      :page="page"
      :page-number="articlePage?.pageNumber"
      @update:page="onUpdatePage"
    >
      <n-table
        v-if="articlePage"
        :bordered="false"
        style="margin-top: 24px; margin-bottom: 24px"
      >
        <thead>
          <tr>
            <th><b>{{ t('forum.table.title') }}</b></th>
            <th class="article-number"><b>{{ t('forum.table.viewsReplies') }}</b></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="article of articlePage.items" :key="article.id">
            <td>
              <n-flex :size="2" align="center" :wrap="false">
                <n-icon
                  v-if="article.pinned"
                  size="15"
                  :component="PushPinOutlined"
                />
                <n-icon
                  v-if="article.locked"
                  size="15"
                  :component="LockOutlined"
                />
                <c-a :to="`/forum/${article.id}`">
                  <n-text v-if="article.hidden" depth="3">{{ t('forum.status.hidden') }}</n-text>
                  <n-text
                    v-else-if="blacklistStore.isBlocked(article.user.username)"
                    depth="3"
                  >
                    {{ t('forum.status.blocked') }}
                  </n-text>
                  <b v-else>{{ article.title }}</b>
                </c-a>
              </n-flex>
              <n-text style="font-size: 12px">
                {{ article.updateAt === article.createAt ? t('forum.status.published') : t('forum.status.updated') }} {{ t('forum.status.at') }}
                <n-time :time="article.updateAt * 1000" type="relative" />
                {{ t('forum.status.by') }} {{ article.user.username }}
              </n-text>

              <n-flex v-if="whoami.asAdmin" style="margin-top: 4px">
                <c-button
                  v-if="article.locked"
                  size="tiny"
                  secondary
                  :label="t('forum.actions.unlock')"
                  @action="unlockArticle(article)"
                />
                <c-button
                  v-else
                  :label="t('forum.actions.lock')"
                  size="tiny"
                  secondary
                  @action="lockArticle(article)"
                />

                <c-button
                  v-if="article.pinned"
                  :label="t('forum.actions.unpin')"
                  size="tiny"
                  secondary
                  @action="unpinArticle(article)"
                />
                <c-button
                  v-else
                  :label="t('forum.actions.pin')"
                  size="tiny"
                  secondary
                  @action="pinArticle(article)"
                />

                <c-button
                  v-if="article.hidden"
                  :label="t('forum.actions.unhide')"
                  secondary
                  size="tiny"
                  @action="unhideArticle(article)"
                />
                <c-button
                  v-else
                  :label="t('forum.actions.hide')"
                  secondary
                  size="tiny"
                  @action="hideArticle(article)"
                />

                <c-button
                  size="tiny"
                  secondary
                  :label="t('forum.actions.delete')"
                  type="error"
                  @action="deleteArticle(article)"
                />
              </n-flex>
            </td>
            <td class="article-number">
              {{ article.numViews }}/{{ article.numComments }}
            </td>
          </tr>
        </tbody>
      </n-table>

      <CResultX v-else :error="error" :title="t('forum.loadError')" />
    </CPage>
  </div>
</template>

<style scoped>
.article-number {
  width: 50px;
  text-align: center;
}
</style>
