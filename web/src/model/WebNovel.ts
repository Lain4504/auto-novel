export interface WebNovelOutlineDto {
  providerId: string;
  novelId: string;
  titleJp: string;
  titleVi?: string;
  type: string;
  attentions: string[];
  keywords: string[];
  extra?: string;
  //
  favored?: string;
  lastReadAt?: number;
  //
  total: number;
  jp: number;
  baidu: number;
  youdao: number;
  gpt: number;
  sakura: number;
  updateAt?: number;
}

export interface WebNovelTocItemDto {
  titleJp: string;
  titleVi?: string;
  chapterId?: string;
  createAt?: number;
}

export interface WebNovelDto {
  wenkuId?: string;
  titleJp: string;
  titleVi?: string;
  authors: { name: string; link: string }[];
  type: string;
  attentions: string[];
  keywords: string[];
  points?: number;
  totalCharacters?: number;
  introductionJp: string;
  introductionVi?: string;
  glossary: { [key: string]: string };
  toc: WebNovelTocItemDto[];
  visited: number;
  syncAt: number;
  favored?: string;
  lastReadChapterId?: string;
  jp: number;
  baidu: number;
  youdao: number;
  gpt: number;
  sakura: number;
}

export interface WebNovelChapterDto {
  titleJp: string;
  titleVi?: string;
  prevId?: string;
  nextId?: string;
  paragraphs: string[];
  baiduParagraphs?: string[];
  youdaoParagraphs?: string[];
  gptParagraphs?: string[];
  sakuraParagraphs?: string[];
}
