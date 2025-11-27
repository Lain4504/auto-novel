import { MD5 } from 'crypto-es/lib/md5';
import { customAlphabet } from 'nanoid';

import { TranslationCacheRepo } from '@/repos';
import type { Glossary } from '@/model/Glossary';
import type { TranslatorId } from '@/model/Translator';

export type Segmentor = (
  textJp: string[],
  textVi?: string[],
) => [string[], string[]?][];

export type Logger = (message: string, detail?: string[]) => void;

export type SegmentContext = {
  glossary: Glossary;
  prevSegs: string[][];
  signal?: AbortSignal;
};

export interface SegmentTranslator {
  id: TranslatorId;
  segmentor: Segmentor;
  translate: (seg: string[], context: SegmentContext) => Promise<string[]>;
  log: (message: string, detail?: string[]) => void;
}

export const createGlossaryWrapper = (glossary: Glossary) => {
  for (const key in glossary) {
    const parts = glossary[key].split('#');
    if (parts.length > 1) {
      glossary[key] = parts[0].trim();
    }
  }

  const presetTokens = [
    'kie',
    'rgx',
    'wfv',
    'oyg',
    'yhs',
    'rvy',
    'dpt',
    'wkj',
    'gzg',
    'xef',
    'efx',
    'ugx',
    'woz',
    'peh',
    'rjp',
    'eon',
    'ayj',
    'gkp',
    'wie',
    'yla',
  ];
  const usedToken: string[] = [];
  const nanoid = customAlphabet('abcdefghijklmnopqrstuvwxyz', 4);
  const generateToken = () => {
    let token = presetTokens.shift();
    if (token === undefined) {
      while (true) {
        token = nanoid();
        if (
          !/(.)\1/.test(token) &&
          !usedToken.some((used) => token!.includes(used))
        ) {
          break;
        }
      }
    }
    usedToken.push(token);
    return token;
  };

  const sortedKeys = (glossary: Glossary) =>
    Object.keys(glossary).sort((a, b) => b.length - a.length);

  const wordJpToToken: Glossary = {};
  const tokenToWordVi: Glossary = {};
  for (const wordJp of sortedKeys(glossary)) {
    const wordVi = glossary[wordJp];
    const token = generateToken();
    wordJpToToken[wordJp] = token;
    tokenToWordVi[token] = wordVi;
  }

  const encode = (text: string[]): string[] => {
    return text.map((line) => {
      for (const wordJp of sortedKeys(wordJpToToken)) {
        const token = wordJpToToken[wordJp];
        line = line.replaceAll(wordJp, '$' + token);
      }
      return line;
    });
  };

  const decode = (text: string[]): string[] => {
    return text.map((line) => {
      for (const token of sortedKeys(tokenToWordVi)) {
        const wordVi = tokenToWordVi[token];
        line = line
          .replaceAll('$' + token, wordVi)
          .replaceAll('$ ' + token, wordVi)
          .replaceAll(token, wordVi);
      }
      return line;
    });
  };

  return async (
    textJp: string[],
    callback: (input: string[]) => Promise<string[]>,
  ) => {
    const textJpEncoded = encode(textJp);
    const textVi = await callback(textJpEncoded);
    const textViDecoded = decode(textVi);
    return textViDecoded;
  };
};

export const createLengthSegmentor = (
  maxLength: number,
  maxLine?: number,
): Segmentor => {
  maxLine = maxLine ?? 65536;

  return (textJp: string[], textVi?: string[]) => {
    type Seg = [string[], string[]?];
    const segs: Seg[] = [];
    let segJp: string[] = [];
    let segVi: string[] = [];
    let segSize = 0;

    for (let i = 0; i < textJp.length; i++) {
      const lineJp = textJp[i];
      const lineJpSize = lineJp.length;

      if (segSize + lineJpSize > maxLength || segJp.length >= maxLine) {
        if (segJp.length > 0) {
          if (textVi === undefined) {
            segs.push([segJp]);
          } else {
            segs.push([segJp, segVi]);
            segVi = [];
          }
          segJp = [];
          segSize = 0;
        }
      }

      if (textVi !== undefined) {
        const lineVi = textVi[i];
        segVi.push(lineVi);
      }

      segJp.push(lineJp);
      segSize += lineJpSize;
    }

    if (segJp.length > 0) {
      if (textVi === undefined) {
        segs.push([segJp]);
      } else {
        segs.push([segJp, segVi]);
      }
    }
    return segs;
  };
};

export interface SegmentCache {
  cacheKey(seg: string[], extra?: unknown): string;
  get(cacheKey: string): Promise<string[] | undefined>;
  save(cacheKey: string, output: string[]): Promise<void>;
}

export const createSegIndexedDbCache = async (
  storeName: 'gpt-seg-cache' | 'sakura-seg-cache',
) => {
  return <SegmentCache>{
    cacheKey: (seg: string[], extra?: unknown): string =>
      MD5(JSON.stringify({ seg, extra })).toString(),

    get: (hash: string): Promise<string[] | undefined> =>
      TranslationCacheRepo.get(storeName, hash),

    save: (hash: string, text: string[]): Promise<void> =>
      TranslationCacheRepo.create(storeName, hash, text).then(() => {}),
  };
};
