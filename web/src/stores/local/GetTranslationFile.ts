import { i18nGlobal } from '@/locales';
import { parseFile } from '@/util/file';

import { EpubParserV1 } from './EpubParser';
import type { LocalVolumeDao } from './LocalVolumeDao';

export const getTranslationFile = async (
  dao: LocalVolumeDao,
  {
    id,
    mode,
    translationsMode,
    translations,
  }: {
    id: string;
    mode: 'vi' | 'vi-jp' | 'jp-vi';
    translationsMode: 'parallel' | 'priority';
    translations: ('sakura' | 'baidu' | 'youdao' | 'gpt')[];
  },
) => {
  const filename = [
    mode,
    (translationsMode === 'parallel' ? 'B' : 'Y') +
      translations.map((it) => it[0]).join(''),
    id,
  ].join('.');

  const metadata = await dao.getMetadata(id);
  if (metadata === undefined)
    throw Error(i18nGlobal.t('stores.translationFile.novelMissing'));

  const getViLinesList = async (chapterId: string) => {
    const chapter = await dao.getChapter(id, chapterId);
    if (chapter === undefined)
      throw Error(i18nGlobal.t('stores.translationFile.chapterMissing'));

    const jpLines = chapter.paragraphs;
    const viLinesList: Array<Array<string>> = [];

    for (const id of translations) {
      const viLine = chapter[id]?.paragraphs;
      if (viLine !== undefined) viLinesList.push(viLine);
    }

    if (translationsMode === 'priority' && viLinesList.length > 1) {
      viLinesList.length = 1;
    }

    return { jpLines, viLinesList };
  };

  const file = await dao.getFile(id);
  if (file === undefined)
    throw Error(i18nGlobal.t('stores.translationFile.originalMissing'));

  const myFile = await parseFile(file.file);

  if (myFile.type === 'txt') {
    const buffer = [];
    for (const { chapterId } of metadata.toc) {
      const { jpLines, viLinesList } = await getViLinesList(chapterId);

      if (viLinesList.length === 0) {
        buffer.push(i18nGlobal.t('stores.translationFile.missingSegment'));
      } else {
        const combinedLinesList = viLinesList;
        if (mode === 'jp-vi') {
          combinedLinesList.unshift(jpLines);
        } else if (mode === 'vi-jp') {
          combinedLinesList.push(jpLines);
        }
        for (let i = 0; i < combinedLinesList[0].length; i++) {
          combinedLinesList.forEach((lines) => buffer.push(lines[i]));
        }
      }
    }
    myFile.text = buffer.join('\n');
  } else if (myFile.type === 'epub') {
    // 防止部分阅读器使用竖排
    myFile.packageDoc
      .getElementsByTagName('spine')
      .item(0)
      ?.removeAttribute('page-progression-direction');

    for await (const item of myFile.iterDoc()) {
      if (metadata.toc.some((it) => it.chapterId === item.href)) {
        const { viLinesList } = await getViLinesList(item.href);
        if (viLinesList.length > 0) {
          await EpubParserV1.injectTranslation(item.doc, mode, viLinesList);
        }
      }
    }

    // 清除css格式
    myFile.cleanStyle();
  } else if (myFile.type === 'srt') {
    const { viLinesList } = await getViLinesList('0');
    const newSubtitles: typeof myFile.subtitles = [];
    for (const s of myFile.subtitles) {
      const texts: string[][] = [];
      for (const viLines of viLinesList) {
        texts.push(viLines.slice(0, s.text.length));
        viLines.splice(0, s.text.length);
      }
      if (mode === 'jp-vi') {
        texts.unshift(s.text);
      } else if (mode === 'vi-jp') {
        texts.push(s.text);
      }

      for (const text of texts) {
        newSubtitles.push({
          id: (newSubtitles.length + 1).toString(),
          time: s.time,
          text,
        });
      }
    }
    myFile.subtitles = newSubtitles;
  }

  return {
    filename,
    blob: await myFile.toBlob(),
  };
};
