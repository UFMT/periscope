import { useCallback, useMemo, useState, type ReactNode } from 'react';
import { I18nContext, type I18nContextValue } from './i18nContext';
import { translate, translateNature, type Locale, type TranslationKey } from './translations';

const LANG_KEY = 'periscope.lang';

function readInitialLocale(): Locale {
  try {
    const saved = localStorage.getItem(LANG_KEY);
    if (saved === 'en-US' || saved === 'pt-BR') {
      return saved;
    }
  } catch {
    // ignore
  }
  return 'pt-BR';
}

export function I18nProvider({
  children,
  initialLocale,
}: {
  children: ReactNode;
  initialLocale?: Locale;
}) {
  const [locale, setLocaleState] = useState<Locale>(() => initialLocale ?? readInitialLocale());

  const setLocale = useCallback((next: Locale) => {
    setLocaleState(next);
    try {
      localStorage.setItem(LANG_KEY, next);
    } catch {
      // ignore
    }
  }, []);

  const t = useCallback(
    (key: TranslationKey, params?: Record<string, string | number>) =>
      translate(locale, key, params),
    [locale],
  );

  const natureLabel = useCallback(
    (key: string | null | undefined) => translateNature(locale, key),
    [locale],
  );

  const value = useMemo<I18nContextValue>(
    () => ({ locale, setLocale, t, natureLabel }),
    [locale, setLocale, t, natureLabel],
  );

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}
