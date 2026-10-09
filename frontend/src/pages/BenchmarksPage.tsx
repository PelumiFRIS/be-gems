import { useEffect, useRef, useState, type FormEvent } from "react";
import { getBenchmarks, resetBenchmark, setBenchmark } from "../api/benchmarks";
import { extractErrorMessage } from "../api/client";
import type { BenchmarkSettings, BenchmarkTarget } from "../api/types";
import { Sidebar } from "../components/Sidebar";
import { TopBar } from "../components/TopBar";
import { canManageBoard } from "../constants/directors";
import { useAuth } from "../context/AuthContext";

/** The BGEI row has no dimension and is a percentage; every other row is a score out of 5. */
function formatTarget(target: BenchmarkTarget): string {
  return target.dimensionId === null ? `${target.target.toFixed(2)}%` : target.target.toFixed(2);
}

function rowKey(target: BenchmarkTarget): string {
  return target.dimensionId ?? "bgei";
}

export function BenchmarksPage() {
  const { user } = useAuth();
  const canEdit = canManageBoard(user?.role);

  const [settings, setSettings] = useState<BenchmarkSettings | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editingKey, setEditingKey] = useState<string | null>(null);
  const [draftTarget, setDraftTarget] = useState("");
  const [draftSource, setDraftSource] = useState("");
  const [saving, setSaving] = useState(false);
  const savingRef = useRef(false);

  useEffect(() => {
    getBenchmarks()
      .then(setSettings)
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  async function guardedSave(action: () => Promise<BenchmarkSettings>) {
    if (savingRef.current) return;
    savingRef.current = true;
    setSaving(true);
    setError(null);
    try {
      setSettings(await action());
      setEditingKey(null);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      savingRef.current = false;
      setSaving(false);
    }
  }

  function startEdit(target: BenchmarkTarget) {
    setError(null);
    setEditingKey(rowKey(target));
    setDraftTarget(target.target.toFixed(2));
    setDraftSource(target.isDefault ? "" : (target.source ?? ""));
  }

  function handleSave(event: FormEvent, target: BenchmarkTarget) {
    event.preventDefault();
    guardedSave(() =>
      setBenchmark(target.dimensionId, { target: Number(draftTarget), source: draftSource.trim() || null }),
    );
  }

  function renderRow(target: BenchmarkTarget) {
    const isBgei = target.dimensionId === null;
    if (editingKey === rowKey(target)) {
      return (
        <tr key={rowKey(target)}>
          <td colSpan={canEdit ? 4 : 3}>
            <form className="inline-edit-form" onSubmit={(e) => handleSave(e, target)}>
              <strong>{target.name}</strong>
              <label className="inline-field">
                {isBgei ? "Benchmark (%)" : "Benchmark (0–5)"}
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  max={isBgei ? 100 : 5}
                  value={draftTarget}
                  onChange={(e) => setDraftTarget(e.target.value)}
                  required
                />
              </label>
              <label className="inline-field inline-field-wide">
                Basis
                <input
                  value={draftSource}
                  maxLength={200}
                  placeholder="e.g. CBN Corporate Governance Guidelines 2023, s.5"
                  onChange={(e) => setDraftSource(e.target.value)}
                />
              </label>
              <button type="submit" className="small" disabled={saving}>
                {saving ? "Saving..." : "Save"}
              </button>
              <button type="button" className="secondary small" onClick={() => setEditingKey(null)} disabled={saving}>
                Cancel
              </button>
            </form>
          </td>
        </tr>
      );
    }
    return (
      <tr key={rowKey(target)}>
        <td>{isBgei ? <strong>Board Governance Effectiveness Index (BGEI)</strong> : target.name}</td>
        <td className="numeric">{formatTarget(target)}</td>
        <td>
          {target.isDefault ? <span className="table-hint">{target.source}</span> : (target.source ?? "Set by your organisation")}
        </td>
        {canEdit && (
          <td className="cell-actions">
            <button type="button" className="link-button" onClick={() => startEdit(target)}>
              Edit
            </button>
            {!target.isDefault && (
              <button
                type="button"
                className="link-button"
                disabled={saving}
                onClick={() => guardedSave(() => resetBenchmark(target.dimensionId))}
              >
                Use default
              </button>
            )}
          </td>
        )}
      </tr>
    );
  }

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <TopBar />
        <div className="page-header">
          <h1>Governance Benchmarks</h1>
          <p>The targets each Board evaluation is compared against in the Regulatory Benchmark.</p>
        </div>

        {error && <p className="form-error">{error}</p>}
        {loading && <p className="page-status">Loading...</p>}

        {settings && (
          <section className="dashboard-section">
            <p className="table-hint section-subtitle">
              Unless you set your own, each dimension is benchmarked at {settings.defaultDimensionTarget.toFixed(2)}{" "}
              (maturity Level 3, Defined) and the BGEI at {settings.defaultBgeiTarget.toFixed(2)}% (Effective). Record
              the regulation or code behind each target as its basis so it appears in the report.
            </p>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Area</th>
                  <th className="numeric">Benchmark</th>
                  <th>Basis</th>
                  {canEdit && <th aria-label="Actions" />}
                </tr>
              </thead>
              <tbody>
                {settings.dimensions.map(renderRow)}
                {renderRow(settings.bgei)}
              </tbody>
            </table>
          </section>
        )}
      </main>
    </div>
  );
}
