import type { FormEvent } from 'react';
import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as harmApi from '../api/harmonization';
import * as referenceApi from '../api/reference';
import { ErrorAlert, Loading, SuccessAlert } from '../components/Feedback';
import { Pagination } from '../components/Pagination';
import { useI18n } from '../i18n/useI18n';
import type { Rule, RuleRequest } from '../types';
import { ApiError } from '../types';

export function HarmonizationPage() {
  const { id: projectId = '' } = useParams();
  const queryClient = useQueryClient();
  const { t, natureLabel } = useI18n();
  const [suggestType, setSuggestType] = useState<'applicant' | 'inventor'>('applicant');
  const [query, setQuery] = useState('');
  const [debounced, setDebounced] = useState('');
  const [entitySearch, setEntitySearch] = useState('');
  const [entityPage, setEntityPage] = useState(0);
  const [selected, setSelected] = useState<string[]>([]);
  const [editingRuleId, setEditingRuleId] = useState<string | null>(null);
  const [ruleName, setRuleName] = useState('');
  const [ruleAcronym, setRuleAcronym] = useState('');
  const [countryAcronym, setCountryAcronym] = useState('');
  const [stateAcronym, setStateAcronym] = useState('');
  const [nature, setNature] = useState('');
  const [feedback, setFeedback] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const timer = window.setTimeout(() => setDebounced(query.trim()), 300);
    return () => window.clearTimeout(timer);
  }, [query]);

  const countriesQuery = useQuery({
    queryKey: ['reference-countries'],
    queryFn: referenceApi.listCountries,
  });

  const applicantTypesQuery = useQuery({
    queryKey: ['reference-applicant-types'],
    queryFn: referenceApi.listApplicantTypes,
  });

  const suggestionsQuery = useQuery({
    queryKey: ['suggestions', projectId, suggestType, debounced],
    queryFn: () => harmApi.getSuggestions(projectId, suggestType, debounced),
    enabled: !!projectId && debounced.length >= 2,
  });

  const entitiesQuery = useQuery({
    queryKey: ['harmonization-entities', projectId, suggestType, entityPage, entitySearch],
    queryFn: () =>
      harmApi.listEntities(projectId, {
        type: suggestType,
        page: entityPage,
        size: 10,
        q: entitySearch || undefined,
      }),
    enabled: !!projectId,
  });

  const rulesQuery = useQuery({
    queryKey: ['rules', projectId],
    queryFn: () => harmApi.listRules(projectId),
    enabled: !!projectId,
  });

  function resetRuleForm() {
    setEditingRuleId(null);
    setRuleName('');
    setRuleAcronym('');
    setCountryAcronym('');
    setStateAcronym('');
    setNature('');
    setSelected([]);
  }

  const createMutation = useMutation({
    mutationFn: (data: RuleRequest) => harmApi.createRule(projectId, data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['rules', projectId] });
      setFeedback(t('harm.ruleCreated'));
      resetRuleForm();
      setError(null);
    },
    onError: (err: unknown) => {
      setError(err instanceof ApiError ? err.message : 'Error');
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ ruleId, data }: { ruleId: string; data: RuleRequest }) =>
      harmApi.updateRule(projectId, ruleId, data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['rules', projectId] });
      setFeedback(t('harm.ruleUpdated'));
      resetRuleForm();
      setError(null);
    },
    onError: (err: unknown) => {
      setError(err instanceof ApiError ? err.message : 'Error');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (ruleId: string) => harmApi.deleteRule(projectId, ruleId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['rules', projectId] });
      setFeedback(t('harm.ruleDeleted'));
    },
    onError: (err: unknown) => {
      setError(err instanceof ApiError ? err.message : 'Error');
    },
  });

  const undoMutation = useMutation({
    mutationFn: (ruleId: string) => harmApi.undoRule(projectId, ruleId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['harmonization-entities', projectId] });
      setFeedback(t('harm.ruleUndone'));
    },
    onError: (err: unknown) => {
      setError(err instanceof ApiError ? err.message : 'Error');
    },
  });

  const applyMutation = useMutation({
    mutationFn: () => harmApi.applyAllRules(projectId),
    onSuccess: (data) => {
      void queryClient.invalidateQueries({ queryKey: ['harmonization-entities', projectId] });
      setFeedback(data.message || `${data.applied}`);
    },
    onError: (err: unknown) => {
      setError(err instanceof ApiError ? err.message : 'Error');
    },
  });

  const applyOneMutation = useMutation({
    mutationFn: (ruleId: string) => harmApi.applyRule(projectId, ruleId),
    onSuccess: (data) => {
      void queryClient.invalidateQueries({ queryKey: ['harmonization-entities', projectId] });
      setFeedback(data.message);
    },
    onError: (err: unknown) => {
      setError(err instanceof ApiError ? err.message : 'Error');
    },
  });

  const suggestions = suggestionsQuery.data?.suggestions || [];
  const selectedSet = useMemo(() => new Set(selected), [selected]);
  const selectedCountryObj = useMemo(
    () => (countriesQuery.data || []).find((c) => c.acronym === countryAcronym),
    [countriesQuery.data, countryAcronym],
  );
  const availableStates = selectedCountryObj?.states || [];

  function toggleSuggestion(name: string) {
    setSelected((prev) =>
      prev.includes(name) ? prev.filter((s) => s !== name) : [...prev, name],
    );
  }

  function startEditRule(rule: Rule) {
    const subs = Array.isArray(rule.substitutions)
      ? rule.substitutions
      : rule.substitutions
        ? Array.from(rule.substitutions)
        : [];
    setEditingRuleId(rule.id);
    setSuggestType(rule.type === 'INVENTOR' ? 'inventor' : 'applicant');
    setRuleName(rule.name);
    setRuleAcronym(rule.acronym || '');
    setCountryAcronym(rule.countryAcronym || '');
    setStateAcronym(rule.stateAcronym || '');
    setNature(rule.nature || '');
    setSelected(subs);
    setError(null);
  }

  function onSubmitRule(e: FormEvent) {
    e.preventDefault();
    if (!ruleName.trim()) {
      setError(t('harm.nameRequired'));
      return;
    }
    const payload: RuleRequest = {
      name: ruleName.trim(),
      acronym: ruleAcronym.trim() || '',
      type: suggestType === 'applicant' ? 'APPLICANT' : 'INVENTOR',
      substitutions: selected,
      countryAcronym: countryAcronym.trim() || '',
      stateAcronym: stateAcronym.trim() || '',
      nature: suggestType === 'applicant' ? nature.trim() || '' : '',
    };
    if (editingRuleId) {
      updateMutation.mutate({ ruleId: editingRuleId, data: payload });
    } else {
      createMutation.mutate(payload);
    }
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>{t('harm.title')}</h1>
          <p>{t('harm.subtitle')}</p>
        </div>
        <div className="stack-actions">
          <button
            type="button"
            className="btn"
            disabled={applyMutation.isPending}
            onClick={() => applyMutation.mutate()}
          >
            {t('harm.applyAll')}
          </button>
          <Link className="btn btn-secondary" to={`/projects/${projectId}/patents`}>
            {t('harm.patents')}
          </Link>
        </div>
      </div>

      {feedback && <SuccessAlert message={feedback} />}
      {error && <ErrorAlert message={error} />}

      <div className="card">
        <h2>{t('harm.suggestionsTitle')}</h2>
        <div className="form-grid two">
          <div className="field">
            <label htmlFor="suggestType">{t('harm.type')}</label>
            <select
              id="suggestType"
              value={suggestType}
              onChange={(e) => {
                setSuggestType(e.target.value as 'applicant' | 'inventor');
                setEntityPage(0);
                setSelected([]);
              }}
            >
              <option value="applicant">{t('harm.type.applicant')}</option>
              <option value="inventor">{t('harm.type.inventor')}</option>
            </select>
          </div>
          <div className="field">
            <label htmlFor="query">{t('harm.query')}</label>
            <input
              id="query"
              data-testid="harmonization-query"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder={t('harm.queryPlaceholder')}
            />
          </div>
        </div>

        {debounced.length >= 2 && (
          <div style={{ marginTop: '1rem' }}>
            {suggestionsQuery.isLoading ? (
              <Loading label={t('harm.searching')} />
            ) : suggestionsQuery.isError ? (
              <ErrorAlert
                message={
                  suggestionsQuery.error instanceof Error
                    ? suggestionsQuery.error.message
                    : t('harm.suggestionsError')
                }
              />
            ) : suggestions.length === 0 ? (
              <p className="muted">{t('harm.noSuggestions')}</p>
            ) : (
              <div
                className="suggestions-box"
                role="listbox"
                aria-label={t('harm.suggestionsTitle')}
                data-testid="suggestions-list"
              >
                {suggestions.map((name) => (
                  <button
                    key={name}
                    type="button"
                    data-testid={`suggestion-${name}`}
                    onClick={() => toggleSuggestion(name)}
                    aria-selected={selectedSet.has(name)}
                  >
                    {selectedSet.has(name) ? '✓ ' : ''}
                    {name}
                  </button>
                ))}
              </div>
            )}
          </div>
        )}

        {selected.length > 0 && (
          <div style={{ marginTop: '1rem' }}>
            <p className="muted">{t('harm.selectedSubstitutions')}</p>
            <div className="chips">
              {selected.map((name) => (
                <button
                  key={name}
                  type="button"
                  className="chip"
                  onClick={() => toggleSuggestion(name)}
                >
                  {name} ✕
                </button>
              ))}
            </div>
          </div>
        )}

        <form className="form-grid" style={{ marginTop: '1.25rem' }} onSubmit={onSubmitRule} data-testid="rule-form">
          <h3>{editingRuleId ? t('harm.ruleFormEdit') : t('harm.ruleFormCreate')}</h3>
          <div className="form-grid two">
            <div className="field">
              <label htmlFor="ruleName">{t('harm.canonicalName')}</label>
              <input
                id="ruleName"
                data-testid="rule-name"
                value={ruleName}
                onChange={(e) => setRuleName(e.target.value)}
                required
              />
            </div>
            <div className="field">
              <label htmlFor="ruleAcronym">{t('harm.acronym')}</label>
              <input
                id="ruleAcronym"
                data-testid="rule-acronym"
                value={ruleAcronym}
                onChange={(e) => setRuleAcronym(e.target.value)}
              />
            </div>
            <div className="field">
              <label htmlFor="ruleCountry">{t('harm.country')}</label>
              <select
                id="ruleCountry"
                data-testid="rule-country"
                value={countryAcronym}
                onChange={(e) => {
                  setCountryAcronym(e.target.value);
                  setStateAcronym('');
                }}
              >
                <option value="">{t('harm.selectCountry')}</option>
                {(countriesQuery.data || []).map((c) => (
                  <option key={c.acronym} value={c.acronym}>
                    {c.acronym} — {c.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label htmlFor="ruleState">{t('harm.state')}</label>
              {availableStates.length > 0 ? (
                <select
                  id="ruleState"
                  data-testid="rule-state"
                  value={stateAcronym}
                  onChange={(e) => setStateAcronym(e.target.value)}
                >
                  <option value="">{t('harm.selectState')}</option>
                  {availableStates.map((s) => (
                    <option key={s.acronym} value={s.acronym}>
                      {s.acronym} — {s.name}
                    </option>
                  ))}
                </select>
              ) : (
                <input
                  id="ruleState"
                  data-testid="rule-state"
                  value={stateAcronym}
                  onChange={(e) => setStateAcronym(e.target.value)}
                  placeholder="SP, MT…"
                />
              )}
            </div>
            {suggestType === 'applicant' && (
              <div className="field">
                <label htmlFor="ruleNature">{t('harm.nature')}</label>
                <select
                  id="ruleNature"
                  data-testid="rule-nature"
                  value={nature}
                  onChange={(e) => setNature(e.target.value)}
                >
                  <option value="">{t('harm.selectNature')}</option>
                  {(applicantTypesQuery.data || []).map((key) => (
                    <option key={key} value={key}>
                      {natureLabel(key)}
                    </option>
                  ))}
                </select>
              </div>
            )}
          </div>
          <div className="stack-actions">
            <button
              type="submit"
              className="btn"
              data-testid="rule-submit"
              disabled={createMutation.isPending || updateMutation.isPending}
            >
              {editingRuleId
                ? updateMutation.isPending
                  ? t('harm.savingRule')
                  : t('harm.saveRule')
                : createMutation.isPending
                  ? t('harm.creatingRule')
                  : t('harm.createRule')}
            </button>
            {editingRuleId && (
              <button type="button" className="btn btn-secondary" onClick={resetRuleForm}>
                {t('harm.cancelEdit')}
              </button>
            )}
          </div>
        </form>
      </div>

      <div className="card">
        <div className="page-header" style={{ marginBottom: '0.75rem' }}>
          <h2>{t('harm.entitiesTitle')}</h2>
          <div className="field" style={{ minWidth: 220 }}>
            <input
              aria-label={t('harm.entitiesFilter')}
              placeholder={t('harm.entitiesFilter')}
              value={entitySearch}
              onChange={(e) => {
                setEntityPage(0);
                setEntitySearch(e.target.value);
              }}
            />
          </div>
        </div>
        {entitiesQuery.isLoading ? (
          <Loading />
        ) : !entitiesQuery.data?.content.length ? (
          <div className="empty-block">{t('harm.entitiesEmpty')}</div>
        ) : (
          <>
            <div className="table-wrap">
              <table className="data" data-testid="entities-table">
                <thead>
                  <tr>
                    <th>{t('harm.col.entityName')}</th>
                    <th>{t('harm.col.ruleGeo')}</th>
                    <th>{t('harm.col.docs')}</th>
                    <th>{t('harm.col.status')}</th>
                    <th>{t('common.actions')}</th>
                  </tr>
                </thead>
                <tbody>
                  {entitiesQuery.data.content.map((ent, idx) => (
                    <tr key={`${ent.name}-${idx}`}>
                      <td>{ent.name}</td>
                      <td className="muted">
                        {[ent.countryAcronym, ent.stateAcronym].filter(Boolean).join(' / ') || '—'}
                      </td>
                      <td>{ent.documentCount}</td>
                      <td>
                        <span className={`badge ${ent.harmonized ? 'badge-success' : 'badge-muted'}`}>
                          {ent.harmonized ? t('harm.harmonized') : t('harm.notHarmonized')}
                        </span>
                      </td>
                      <td>
                        <div className="stack-actions">
                          <button
                            type="button"
                            className="btn btn-secondary btn-sm"
                            onClick={() => toggleSuggestion(ent.name)}
                          >
                            {selectedSet.has(ent.name) ? '✓ ' : ''}
                            {t('harm.addSubstitution')}
                          </button>
                          <button
                            type="button"
                            className="btn btn-secondary btn-sm"
                            onClick={() => setQuery(ent.name)}
                          >
                            {t('harm.findSimilar')}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination
              page={entitiesQuery.data.page}
              totalPages={entitiesQuery.data.totalPages}
              totalElements={entitiesQuery.data.totalElements}
              onChange={setEntityPage}
            />
          </>
        )}
      </div>

      <div className="card">
        <h2>{t('harm.rulesTitle')}</h2>
        {rulesQuery.isLoading ? (
          <Loading />
        ) : rulesQuery.isError ? (
          <ErrorAlert
            message={
              rulesQuery.error instanceof Error ? rulesQuery.error.message : t('harm.rulesError')
            }
          />
        ) : !rulesQuery.data?.length ? (
          <div className="empty-block">{t('harm.rulesEmpty')}</div>
        ) : (
          <div className="table-wrap">
            <table className="data" data-testid="rules-table">
              <thead>
                <tr>
                  <th>{t('harm.col.ruleName')}</th>
                  <th>{t('harm.col.ruleType')}</th>
                  <th>{t('harm.col.ruleAcronym')}</th>
                  <th>{t('harm.col.ruleGeo')}</th>
                  <th>{t('harm.col.ruleNature')}</th>
                  <th>{t('harm.col.ruleSubs')}</th>
                  <th>{t('common.actions')}</th>
                </tr>
              </thead>
              <tbody>
                {rulesQuery.data.map((rule) => {
                  const subs = Array.isArray(rule.substitutions)
                    ? rule.substitutions
                    : rule.substitutions
                      ? Array.from(rule.substitutions)
                      : [];
                  return (
                    <tr key={rule.id} data-testid={`rule-row-${rule.id}`}>
                      <td>{rule.name}</td>
                      <td>
                        <span className="badge">{rule.type}</span>
                      </td>
                      <td>{rule.acronym || '—'}</td>
                      <td className="muted">
                        {[rule.countryAcronym, rule.stateAcronym].filter(Boolean).join(' / ') || '—'}
                      </td>
                      <td className="muted">{natureLabel(rule.nature)}</td>
                      <td className="muted">{subs.join(', ') || '—'}</td>
                      <td>
                        <div className="stack-actions">
                          <button
                            type="button"
                            className="btn btn-secondary btn-sm"
                            onClick={() => startEditRule(rule)}
                          >
                            {t('common.edit')}
                          </button>
                          <button
                            type="button"
                            className="btn btn-secondary btn-sm"
                            onClick={() => applyOneMutation.mutate(rule.id)}
                          >
                            {t('harm.apply')}
                          </button>
                          <button
                            type="button"
                            className="btn btn-secondary btn-sm"
                            onClick={() => undoMutation.mutate(rule.id)}
                          >
                            {t('harm.undo')}
                          </button>
                          <button
                            type="button"
                            className="btn btn-danger btn-sm"
                            onClick={() => {
                              if (confirm(t('harm.confirmDeleteRule'))) {
                                deleteMutation.mutate(rule.id);
                              }
                            }}
                          >
                            {t('common.delete')}
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
