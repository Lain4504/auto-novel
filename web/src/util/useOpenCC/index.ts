type Locale = 'vi' | 'zh';

type Converter = {
  toView: (text: string) => string;
  toData: (text: string) => string;
};

export const defaultConverter: Converter = {
  toView: (text: string) => text,
  toData: (text: string) => text,
};

export async function useOpenCC(locale: Locale) {
  // Both Vietnamese and Chinese (simplified) use default converter
  // No character conversion needed
  return defaultConverter;
}
