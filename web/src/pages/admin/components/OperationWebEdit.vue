<script lang="ts" setup>
import type { OperationWebEdit } from '@/model/Operation';

defineProps<{ op: OperationWebEdit }>();
</script>

<template>
  <c-a :to="`/novel/${op.providerId}/${op.novelId}`">
    {{ `${op.providerId}/${op.novelId}` }}
  </c-a>
  <TextDiff
    v-if="op.new.titleVi"
    :vi-old="op.old.titleVi"
    :vi-new="op.new.titleVi"
  />
  <TextDiff
    v-if="op.new.introductionVi"
    :vi-old="op.old.introductionVi"
    :vi-new="op.new.introductionVi"
  />

  <TextDiff
    v-for="toc of op.toc"
    :key="toc.jp + toc.old + toc.new"
    :zh-old="toc.old"
    :zh-new="toc.new"
  />
</template>
