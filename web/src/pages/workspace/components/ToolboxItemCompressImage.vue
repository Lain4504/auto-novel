<script lang="ts" setup>
import { ImgComparisonSlider } from '@img-comparison-slider/vue';
import { useI18n } from 'vue-i18n';

import { Humanize } from '@/util';
import type { Epub, ParsedFile } from '@/util/file';

import { Toolbox } from './Toolbox';

const props = defineProps<{
  files: ParsedFile[];
}>();

const message = useMessage();
const { t } = useI18n();

const quality = ref(0.8);
const scaleRatio = ref(1.0);

const imageFormat = ref('image/webp');
const imageFormatOptions = computed(() => [
  { label: t('workspace.compress.formatKeep'), value: '' },
  { label: t('workspace.compress.formatPng'), value: 'image/png' },
  { label: t('workspace.compress.formatJpeg'), value: 'image/jpeg' },
  { label: t('workspace.compress.formatWebp'), value: 'image/webp' },
]);

const compressImage = async (blob: Blob) => {
  const canvas = document.createElement('canvas');
  const ctx = canvas.getContext('2d')!;
  // eslint-disable-next-line compat/compat
  const img = await createImageBitmap(blob);

  const scaleRatioValue = Math.min(1, scaleRatio.value);
  canvas.width = img.width * scaleRatioValue;
  canvas.height = img.height * scaleRatioValue;
  ctx.drawImage(img, 0, 0, canvas.width, canvas.height);

  const imageFormatValue = imageFormat.value;
  const qualityValue = quality.value;

  return await new Promise<Blob | undefined>((resolve, _reject) => {
    canvas.toBlob(
      (newBlob) => {
        resolve(newBlob ?? undefined);
      },
      imageFormatValue,
      qualityValue,
    );
  });
};

const compressImagesForEpub = async (epub: Epub) => {
  for await (const item of epub.iterImage()) {
    const newBlob = await compressImage(item.blob);
    if (!newBlob)
      throw new Error(
        t('workspace.compress.compressFailed', {
          file: epub.name,
          image: item.href,
        }),
      );
    epub.updateImage(item.id, newBlob);
  }
};

const compressImages = () =>
  Toolbox.modifyFiles(
    props.files.filter((file) => file.type === 'epub'),
    compressImagesForEpub,
    (e) => message.error(t('workspace.compress.error', { error: String(e) })),
  );

interface EpubImage {
  id: string;
  href: string;
  blob: Blob;
  uri: string;
  blobCompressed: Blob | undefined;
  uriCompressed: string;
}
interface EpubDetail {
  name: string;
  images: EpubImage[];
  size: number;
  sizeCompressed: number;
  failed: number;
}

const getEpubDetailList = async () => {
  const detailList: EpubDetail[] = [];
  for (const file of props.files) {
    if (file.type === 'epub') {
      const detail: EpubDetail = {
        name: file.name,
        images: [],
        size: 0,
        sizeCompressed: 0,
        failed: 0,
      };
      for await (const item of file.iterImage()) {
        const blobCompressed = await compressImage(item.blob);
        detail.images.push({
          id: item.id,
          href: item.href,
          blob: item.blob,
          uri: URL.createObjectURL(item.blob),
          blobCompressed,
          uriCompressed: URL.createObjectURL(blobCompressed ?? item.blob),
        });
        detail.size += item.blob.size;
        detail.sizeCompressed += (blobCompressed ?? item.blob).size;
        if (!blobCompressed) detail.failed += 1;
      }
      detailList.push(detail);
    }
  }
  return detailList;
};

const showDetail = ref(false);
const detailList = ref<EpubDetail[]>([]);
const toggleShowDetail = async () => {
  if (showDetail.value) {
    showDetail.value = false;
    detailList.value = [];
  } else {
    showDetail.value = true;
    detailList.value = await getEpubDetailList();
  }
};

const showCompare = ref(false);
const compareImages = ref({ old: '', new: '' });
const showPreview = (image: EpubImage) => {
  showCompare.value = true;
  compareImages.value.old = image.uri;
  compareImages.value.new = image.uriCompressed;
};
</script>

<template>
  <n-flex vertical>
    <c-action-wrapper :title="t('workspace.compress.formatTitle')">
      <c-radio-group
        v-model:value="imageFormat"
        :options="imageFormatOptions"
        size="small"
      />
    </c-action-wrapper>

    <c-action-wrapper
      :title="t('workspace.compress.qualityTitle')"
      align="center"
    >
      <n-slider
        v-model:value="quality"
        :max="1"
        :min="0.1"
        :step="0.05"
        :format-tooltip="(value: number) => `${(value * 100).toFixed(0)}%`"
        style="max-width: 400px"
      />
      <n-text style="width: 6em">{{ (quality * 100).toFixed(0) }}%</n-text>
    </c-action-wrapper>

    <c-action-wrapper :title="t('workspace.compress.sizeTitle')" align="center">
      <n-slider
        v-model:value="scaleRatio"
        :max="1"
        :min="0.1"
        :step="0.05"
        :format-tooltip="(value: number) => `${(value * 100).toFixed(0)}%`"
        style="max-width: 400px"
      />
      <n-text style="width: 6em">{{ (scaleRatio * 100).toFixed(0) }}%</n-text>
    </c-action-wrapper>

    <n-button-group>
      <c-button
        :label="t('workspace.compress.compress')"
        @action="compressImages"
      />
      <c-button
        :label="t('workspace.compress.preview')"
        @action="toggleShowDetail"
      />
    </n-button-group>

    <template v-if="showDetail">
      <n-text>{{ t('workspace.compress.previewHint') }}</n-text>
      <n-empty
        v-if="detailList.length === 0"
        :description="t('workspace.compress.empty')"
      />
      <template v-for="detail of detailList" :key="detail.name">
        <n-text>
          [{{ Humanize.bytes(detail.size) }}
          =>
          {{ Humanize.bytes(detail.sizeCompressed) }}]
          {{ detail.name }}
        </n-text>
        <c-x-scrollbar style="margin-top: 16px">
          <n-image-group show-toolbar-tooltip>
            <n-flex :size="4" :wrap="false" style="margin-bottom: 16px">
              <n-image
                v-for="image of detail.images"
                :key="image.id"
                height="150"
                :src="image.uri"
                preview-disabled
                :alt="image.id"
                style="border-radius: 2px"
                @click="showPreview(image)"
              />
            </n-flex>
          </n-image-group>
        </c-x-scrollbar>
      </template>
    </template>

    <c-modal
      v-model:show="showCompare"
      style="width: auto; max-width: 95%"
      :max-height-percentage="100"
    >
      <img-comparison-slider style="outline: none">
        <!-- eslint-disable -->
        <img
          slot="first"
          style="width: 100%; max-height: 85vh"
          :src="compareImages.old"
        />
        <img
          slot="second"
          style="width: 100%; max-height: 85vh"
          :src="compareImages.new"
        />
        <!-- eslint-enable -->
      </img-comparison-slider>
    </c-modal>
  </n-flex>
</template>
