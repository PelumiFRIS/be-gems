import { useEffect, useRef, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { listBoards } from "../api/boards";
import { extractErrorMessage } from "../api/client";
import { addSkill, getSkillsMatrix, rateSkill, removeSkill, updateSkill } from "../api/skills";
import type { BoardSummary, RequiredLevel, SkillCoverage, SkillPayload, SkillRow, SkillsMatrix } from "../api/types";
import { Badge } from "../components/Badge";
import { Sidebar } from "../components/Sidebar";
import { TopBar } from "../components/TopBar";
import { CLASSIFICATION_LABELS, canManageBoard } from "../constants/directors";
import { useAuth } from "../context/AuthContext";

const RATING_LABELS: Record<number, string> = {
  1: "Basic",
  2: "Developing",
  3: "Competent",
  4: "Advanced",
  5: "Expert",
};

const COVERAGE_LABELS: Record<SkillCoverage, string> = {
  COVERED: "Adequately covered",
  UNDERREPRESENTED: "Underrepresented",
  SINGLE_PERSON_DEPENDENCY: "Single-person dependency",
  CRITICAL_GAP: "Critical gap",
  NOT_ASSESSED: "Not assessed",
};

const LEVEL_LABELS: Record<RequiredLevel, string> = { HIGH: "High", MEDIUM: "Medium", LOW: "Low" };

const SUMMARY_ORDER: SkillCoverage[] = ["COVERED", "UNDERREPRESENTED", "SINGLE_PERSON_DEPENDENCY", "CRITICAL_GAP"];

const EMPTY_SKILL: SkillPayload = { name: "", requiredLevel: "MEDIUM", futureFocus: false };

export function SkillsMatrixPage() {
  const { user } = useAuth();
  const canEdit = canManageBoard(user?.role);

  const [board, setBoard] = useState<BoardSummary | null>(null);
  const [matrix, setMatrix] = useState<SkillsMatrix | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [savingRating, setSavingRating] = useState(false);

  const [newSkill, setNewSkill] = useState<SkillPayload>(EMPTY_SKILL);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [draft, setDraft] = useState<SkillPayload>(EMPTY_SKILL);
  const [confirmingRemoveId, setConfirmingRemoveId] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const savingRef = useRef(false);

  useEffect(() => {
    listBoards()
      .then(async (boards) => {
        if (boards.length === 0) return;
        setBoard(boards[0]);
        setMatrix(await getSkillsMatrix(boards[0].id));
      })
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  async function guardedSave(action: () => Promise<SkillsMatrix>, onDone?: () => void) {
    if (savingRef.current) return;
    savingRef.current = true;
    setSaving(true);
    setError(null);
    try {
      setMatrix(await action());
      onDone?.();
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      savingRef.current = false;
      setSaving(false);
    }
  }

  async function handleRate(skill: SkillRow, directorId: string, value: string) {
    if (savingRating) return;
    setSavingRating(true);
    setError(null);
    try {
      setMatrix(await rateSkill(skill.id, directorId, value === "" ? null : Number(value)));
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setSavingRating(false);
    }
  }

  function handleAdd(event: FormEvent) {
    event.preventDefault();
    if (!board) return;
    guardedSave(
      () => addSkill(board.id, { ...newSkill, name: newSkill.name.trim() }),
      () => setNewSkill(EMPTY_SKILL),
    );
  }

  function startEdit(skill: SkillRow) {
    setError(null);
    setConfirmingRemoveId(null);
    setEditingId(skill.id);
    setDraft({ name: skill.name, requiredLevel: skill.requiredLevel, futureFocus: skill.futureFocus });
  }

  function handleUpdate(event: FormEvent, skillId: string) {
    event.preventDefault();
    guardedSave(
      () => updateSkill(skillId, { ...draft, name: draft.name.trim() }),
      () => setEditingId(null),
    );
  }

  const skills = matrix?.skills ?? [];
  const directors = matrix?.directors ?? [];
  const assessed = skills.filter((s) => s.coverage !== "NOT_ASSESSED");
  const notAssessed = skills.length - assessed.length;
  const criticalGaps = skills.filter((s) => s.coverage === "CRITICAL_GAP");
  const dependencies = skills.filter((s) => s.coverage === "SINGLE_PERSON_DEPENDENCY");
  const underrepresented = skills.filter((s) => s.coverage === "UNDERREPRESENTED");
  const futureSkills = skills.filter((s) => s.futureFocus);

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <TopBar />
        <div className="page-header">
          <h1>Board Skills Matrix</h1>
          <p>Each director&apos;s proficiency against the competencies the Board needs, and where the gaps are.</p>
        </div>

        {error && <p className="form-error">{error}</p>}
        {loading && <p className="page-status">Loading...</p>}

        {!loading && !board && (
          <section className="dashboard-section">
            <p className="table-hint">
              Set up the board first on the{" "}
              <Link className="text-link" to="/board-setup">
                Board Setup
              </Link>{" "}
              page.
            </p>
          </section>
        )}

        {matrix && (
          <>
            <div className="stat-grid">
              {SUMMARY_ORDER.map((coverage) => (
                <div className="stat-card" key={coverage}>
                  <div>
                    <div className="stat-card-value">{matrix.coverageCounts[coverage] ?? 0}</div>
                    <div className="stat-card-label">{COVERAGE_LABELS[coverage]}</div>
                  </div>
                </div>
              ))}
            </div>
            {notAssessed > 0 && (
              <p className="table-hint form-hint">
                {notAssessed} of {skills.length} competencies have no ratings yet and are not included in the counts
                above.
              </p>
            )}

            {(criticalGaps.length > 0 || dependencies.length > 0 || underrepresented.length > 0) && (
              <section className="dashboard-section">
                <h2>Gaps to address</h2>
                <ul className="skills-gap-list">
                  {criticalGaps.map((s) => (
                    <li key={s.id}>
                      <Badge value="CRITICAL_GAP" label="Critical gap" /> <strong>{s.name}</strong> &mdash; High
                      requirement, but no director is rated Advanced or above.
                    </li>
                  ))}
                  {dependencies.map((s) => (
                    <li key={s.id}>
                      <Badge value="SINGLE_PERSON_DEPENDENCY" label="Single-person dependency" />{" "}
                      <strong>{s.name}</strong> &mdash; only {s.proficientDirectors[0]} is rated Advanced or above.
                    </li>
                  ))}
                  {underrepresented.map((s) => (
                    <li key={s.id}>
                      <Badge value="UNDERREPRESENTED" label="Underrepresented" /> <strong>{s.name}</strong> &mdash;{" "}
                      {s.requiredLevel === "LOW"
                        ? "no director is rated Competent or above."
                        : "no director is rated Advanced or above."}
                    </li>
                  ))}
                </ul>
              </section>
            )}

            <section className="dashboard-section">
              <div className="section-heading-row">
                <div>
                  <h2>Matrix</h2>
                  <p className="table-hint section-subtitle">{board?.name}</p>
                </div>
              </div>
              {directors.length === 0 ? (
                <p className="table-hint">Add directors on the Board Setup page to start rating skills.</p>
              ) : (
                <div className="table-scroll">
                  <table className="data-table skills-matrix">
                    <thead>
                      <tr>
                        <th>Competency</th>
                        <th>Requirement</th>
                        {directors.map((d) => (
                          <th key={d.id} className="skills-director" title={CLASSIFICATION_LABELS[d.classification]}>
                            {d.name}
                          </th>
                        ))}
                        <th className="numeric">Average</th>
                        <th>Coverage</th>
                      </tr>
                    </thead>
                    <tbody>
                      {skills.map((s) => (
                        <tr key={s.id}>
                          <td className="skills-name">
                            {s.name}
                            {s.futureFocus && <span className="skills-future">Future</span>}
                          </td>
                          <td>
                            <Badge value={s.requiredLevel} label={LEVEL_LABELS[s.requiredLevel]} />
                          </td>
                          {directors.map((d) => {
                            const rating = s.ratings[d.id];
                            return (
                              <td key={d.id} className={`skills-cell${rating ? ` rating-${rating}` : ""}`}>
                                {canEdit ? (
                                  <select
                                    aria-label={`${s.name} rating for ${d.name}`}
                                    value={rating ?? ""}
                                    disabled={savingRating}
                                    onChange={(e) => handleRate(s, d.id, e.target.value)}
                                  >
                                    <option value="">&mdash;</option>
                                    {[1, 2, 3, 4, 5].map((n) => (
                                      <option key={n} value={n}>
                                        {n}
                                      </option>
                                    ))}
                                  </select>
                                ) : (
                                  <span title={rating ? RATING_LABELS[rating] : "Not rated"}>
                                    {rating ?? "—"}
                                  </span>
                                )}
                              </td>
                            );
                          })}
                          <td className="numeric">{s.average != null ? s.average.toFixed(2) : "—"}</td>
                          <td>
                            <Badge value={s.coverage} label={COVERAGE_LABELS[s.coverage]} />
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
              <div className="skills-legend">
                <p>
                  <strong>Rating scale:</strong>{" "}
                  {Object.entries(RATING_LABELS)
                    .map(([n, label]) => `${n} = ${label}`)
                    .join(" · ")}
                </p>
                <p>
                  <strong>Coverage:</strong> a High requirement needs at least two directors rated Advanced (4) or
                  above &mdash; one is a single-person dependency, none is a critical gap. A Medium requirement needs
                  at least one director rated Advanced or above, and a Low requirement at least one rated Competent
                  (3) or above; otherwise it is underrepresented.
                </p>
              </div>
            </section>

            {futureSkills.length > 0 && (
              <section className="dashboard-section">
                <h2>Future skills requirements</h2>
                <p className="table-hint section-subtitle">Competencies the Board expects to need more of.</p>
                <ul className="skills-gap-list">
                  {futureSkills.map((s) => (
                    <li key={s.id}>
                      <strong>{s.name}</strong> &mdash; {COVERAGE_LABELS[s.coverage].toLowerCase()}
                      {s.average != null && `, board average ${s.average.toFixed(2)}`}
                    </li>
                  ))}
                </ul>
              </section>
            )}

            {canEdit && (
              <section className="dashboard-section">
                <h2>Competencies</h2>
                <p className="table-hint section-subtitle">
                  Add the competencies your Board needs, and set how important each one is.
                </p>
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Competency</th>
                      <th>Board requirement</th>
                      <th>Future skill</th>
                      <th aria-label="Actions" />
                    </tr>
                  </thead>
                  <tbody>
                    {skills.map((s) =>
                      editingId === s.id ? (
                        <tr key={s.id}>
                          <td colSpan={4}>
                            <form className="inline-edit-form" onSubmit={(e) => handleUpdate(e, s.id)}>
                              <input
                                aria-label="Competency name"
                                value={draft.name}
                                maxLength={120}
                                onChange={(e) => setDraft({ ...draft, name: e.target.value })}
                                required
                              />
                              <select
                                aria-label="Board requirement"
                                value={draft.requiredLevel}
                                onChange={(e) => setDraft({ ...draft, requiredLevel: e.target.value as RequiredLevel })}
                              >
                                <option value="HIGH">High</option>
                                <option value="MEDIUM">Medium</option>
                                <option value="LOW">Low</option>
                              </select>
                              <label className="checkbox-label">
                                <input
                                  type="checkbox"
                                  checked={draft.futureFocus}
                                  onChange={(e) => setDraft({ ...draft, futureFocus: e.target.checked })}
                                />
                                Future skill
                              </label>
                              <button type="submit" className="small" disabled={saving}>
                                {saving ? "Saving..." : "Save"}
                              </button>
                              <button
                                type="button"
                                className="secondary small"
                                onClick={() => setEditingId(null)}
                                disabled={saving}
                              >
                                Cancel
                              </button>
                            </form>
                          </td>
                        </tr>
                      ) : (
                        <tr key={s.id}>
                          <td>{s.name}</td>
                          <td>{LEVEL_LABELS[s.requiredLevel]}</td>
                          <td>{s.futureFocus ? "Yes" : <span className="table-hint">No</span>}</td>
                          <td className="cell-actions">
                            {confirmingRemoveId === s.id ? (
                              <span className="inline-confirm">
                                <span>Remove {s.name} and its ratings?</span>
                                <button
                                  type="button"
                                  className="danger small"
                                  disabled={saving}
                                  onClick={() => guardedSave(() => removeSkill(s.id), () => setConfirmingRemoveId(null))}
                                >
                                  {saving ? "Removing..." : "Remove"}
                                </button>
                                <button
                                  type="button"
                                  className="secondary small"
                                  disabled={saving}
                                  onClick={() => setConfirmingRemoveId(null)}
                                >
                                  Cancel
                                </button>
                              </span>
                            ) : (
                              <>
                                <button type="button" className="link-button" onClick={() => startEdit(s)}>
                                  Edit
                                </button>
                                <button
                                  type="button"
                                  className="link-button danger-link"
                                  onClick={() => {
                                    setError(null);
                                    setEditingId(null);
                                    setConfirmingRemoveId(s.id);
                                  }}
                                >
                                  Remove
                                </button>
                              </>
                            )}
                          </td>
                        </tr>
                      ),
                    )}
                  </tbody>
                </table>
                <form className="add-form" onSubmit={handleAdd}>
                  <label>
                    New competency
                    <input
                      value={newSkill.name}
                      maxLength={120}
                      onChange={(e) => setNewSkill({ ...newSkill, name: e.target.value })}
                      required
                    />
                  </label>
                  <label>
                    Board requirement
                    <select
                      value={newSkill.requiredLevel}
                      onChange={(e) => setNewSkill({ ...newSkill, requiredLevel: e.target.value as RequiredLevel })}
                    >
                      <option value="HIGH">High</option>
                      <option value="MEDIUM">Medium</option>
                      <option value="LOW">Low</option>
                    </select>
                  </label>
                  <label className="checkbox-label">
                    <input
                      type="checkbox"
                      checked={newSkill.futureFocus}
                      onChange={(e) => setNewSkill({ ...newSkill, futureFocus: e.target.checked })}
                    />
                    Future skill
                  </label>
                  <button type="submit" disabled={saving}>
                    {saving ? "Adding..." : "Add competency"}
                  </button>
                </form>
              </section>
            )}
          </>
        )}
      </main>
    </div>
  );
}
