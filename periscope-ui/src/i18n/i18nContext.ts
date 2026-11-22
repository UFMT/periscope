import { createContext } from 'react';
import type { Locale, TranslationKey } from './translations';

export interface I18nContextValue {
  locale: Locale;
  setLocale: (locale: Locale) => void;
  t: (key: TranslationKey, params?: Record<string, string | number>) => string;
  natureLabel: (key: string | null | undefined) => string;
}

export const I18nContext = createContext<I18nContextValue | null>(null);
