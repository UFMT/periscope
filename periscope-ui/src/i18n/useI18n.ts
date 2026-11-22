import { useContext } from 'react';
import { I18nContext, type I18nContextValue } from './i18nContext';
import { translate, translateNature, type TranslationKey } from './translations';

const FALLBACK: I18nContextValue = {
  locale: 'pt-BR',
  setLocale: () => {},
  t: (key: TranslationKey, params?: Record<string, string | number>) =>
    translate('pt-BR', key, params),
  natureLabel: (key: string | null | undefined) => translateNature('pt-BR', key),
};

export function useI18n(): I18nContextValue {
  return useContext(I18nContext) ?? FALLBACK;
}
