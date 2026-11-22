import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { I18nProvider } from '../i18n/I18nProvider';
import { translate, translateNature } from '../i18n/translations';
import { useI18n } from '../i18n/useI18n';

function Probe() {
  const { locale, setLocale, t, natureLabel } = useI18n();
  return (
    <div>
      <span data-testid="locale">{locale}</span>
      <span data-testid="title">{t('projects.title')}</span>
      <span data-testid="nature">{natureLabel('private.company')}</span>
      <button type="button" onClick={() => setLocale(locale === 'pt-BR' ? 'en-US' : 'pt-BR')}>
        switch
      </button>
    </div>
  );
}

describe('i18n', () => {
  it('switches between pt-BR and en-US and translates natures', async () => {
    localStorage.clear();
    const user = userEvent.setup();
    render(
      <I18nProvider>
        <Probe />
      </I18nProvider>,
    );

    expect(screen.getByTestId('locale')).toHaveTextContent('pt-BR');
    expect(screen.getByTestId('title')).toHaveTextContent('Projetos');
    expect(screen.getByTestId('nature')).toHaveTextContent('Empresa Privada');

    await user.click(screen.getByRole('button', { name: 'switch' }));

    expect(screen.getByTestId('locale')).toHaveTextContent('en-US');
    expect(screen.getByTestId('title')).toHaveTextContent('Projects');
    expect(screen.getByTestId('nature')).toHaveTextContent('Private Company');
    expect(localStorage.getItem('periscope.lang')).toBe('en-US');
  });

  it('falls back cleanly for unknown nature keys and interpolates params', () => {
    expect(translateNature('en-US', 'custom.nature')).toBe('custom.nature');
    expect(translate('pt-BR', 'patents.countSuffix', { count: 7 })).toBe('7 patente(s)');
  });
});
