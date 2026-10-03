import { useEffect, useRef, useState, type FormEvent } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { listBoards } from "../api/boards";
import { extractErrorMessage } from "../api/client";
import { createDirector, getDirector, updateDirector, uploadDirectorCv } from "../api/directors";
import type { DirectorClassification, DirectorDetail, DirectorProfilePayload } from "../api/types";
import { Sidebar } from "../components/Sidebar";
import { TopBar } from "../components/TopBar";
import { CLASSIFICATIONS, CLASSIFICATION_LABELS } from "../constants/directors";

interface FormState {
  name: string;
  email: string;
  classification: DirectorClassification;
  phone: string;
  dateOfBirth: string;
  address: string;
  appointmentDate: string;
  reElectionDate: string;
  termExpirationDate: string;
  profession: string;
  qualification: string;
  experience: string;
}

const EMPTY_FORM: FormState = {
  name: "",
  email: "",
  classification: "NON_EXECUTIVE_DIRECTOR",
  phone: "",
  dateOfBirth: "",
  address: "",
  appointmentDate: "",
  reElectionDate: "",
  termExpirationDate: "",
  profession: "",
  qualification: "",
  experience: "",
};

function fromDetail(director: DirectorDetail): FormState {
  return {
    name: director.name,
    email: director.email ?? "",
    classification: director.classification,
    phone: director.phone ?? "",
    dateOfBirth: director.dateOfBirth ?? "",
    address: director.address ?? "",
    appointmentDate: director.appointmentDate ?? "",
    reElectionDate: director.reElectionDate ?? "",
    termExpirationDate: director.termExpirationDate ?? "",
    profession: director.profession ?? "",
    qualification: director.qualification ?? "",
    experience: director.experience ?? "",
  };
}

function toPayload(form: FormState): DirectorProfilePayload {
  const optional = (value: string) => (value.trim() ? value.trim() : undefined);
  return {
    name: form.name.trim(),
    email: form.email.trim(),
    classification: form.classification,
    phone: optional(form.phone),
    dateOfBirth: optional(form.dateOfBirth),
    address: optional(form.address),
    appointmentDate: optional(form.appointmentDate),
    reElectionDate: optional(form.reElectionDate),
    termExpirationDate: optional(form.termExpirationDate),
    profession: optional(form.profession),
    qualification: optional(form.qualification),
    experience: optional(form.experience),
  };
}

export function DirectorFormPage() {
  const { id } = useParams<{ id: string }>();
  const isEdit = Boolean(id);
  const navigate = useNavigate();

  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [boardId, setBoardId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const savingRef = useRef(false);
  const cvInputRef = useRef<HTMLInputElement>(null);
  const today = new Date().toISOString().slice(0, 10);

  useEffect(() => {
    const load = id
      ? getDirector(id).then((director) => {
          setForm(fromDetail(director));
          setBoardId(director.boardId);
        })
      : listBoards().then((boards) => setBoardId(boards[0]?.id ?? null));
    load.catch((err) => setError(extractErrorMessage(err))).finally(() => setLoading(false));
  }, [id]);

  function update<K extends keyof FormState>(field: K, value: FormState[K]) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    // A ref, not just state: a second click can land before React re-renders the disabled button.
    if (savingRef.current || !boardId) return;
    savingRef.current = true;
    setSaving(true);
    setError(null);
    try {
      if (id) {
        await updateDirector(id, toPayload(form));
        navigate(`/board-setup/directors/${id}`);
        return;
      }
      const created = await createDirector({ boardId, ...toPayload(form) });
      const cvFile = cvInputRef.current?.files?.[0];
      let cvError: string | undefined;
      if (cvFile) {
        try {
          await uploadDirectorCv(created.id, cvFile);
        } catch (err) {
          cvError = `Director saved, but the CV didn't upload: ${extractErrorMessage(err)}`;
        }
      }
      navigate(`/board-setup/directors/${created.id}`, { state: cvError ? { notice: cvError } : undefined });
    } catch (err) {
      setError(extractErrorMessage(err));
      savingRef.current = false;
      setSaving(false);
    }
  }

  const backTo = id ? `/board-setup/directors/${id}` : "/board-setup";

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <TopBar />
        <nav className="breadcrumb">
          <Link to="/board-setup">Board Setup</Link>
          <span>/</span>
          {isEdit ? <Link to={backTo}>{form.name || "Director"}</Link> : <span>New director</span>}
          {isEdit && (
            <>
              <span>/</span>
              <span>Edit</span>
            </>
          )}
        </nav>
        <div className="page-header">
          <h1>{isEdit ? "Edit director" : "Add a director"}</h1>
          <p>Capture the director's biodata. Fields marked * are required.</p>
        </div>

        {error && <p className="form-error">{error}</p>}
        {loading && <p className="page-status">Loading...</p>}
        {!loading && !isEdit && !boardId && (
          <p className="form-error">
            Create your board on the <Link to="/board-setup">Board Setup</Link> page before adding directors.
          </p>
        )}

        {!loading && (isEdit || boardId) && (
          <form onSubmit={handleSubmit}>
            <section className="dashboard-section">
              <h2>Personal details</h2>
              <div className="form-grid">
                <label>
                  Full name *
                  <input value={form.name} onChange={(e) => update("name", e.target.value)} required maxLength={255} />
                </label>
                <label>
                  Email *
                  <input
                    type="email"
                    value={form.email}
                    onChange={(e) => update("email", e.target.value)}
                    required
                    maxLength={255}
                  />
                </label>
                <label>
                  Telephone
                  <input type="tel" value={form.phone} onChange={(e) => update("phone", e.target.value)} maxLength={50} />
                </label>
                <label>
                  Date of birth
                  <input
                    type="date"
                    value={form.dateOfBirth}
                    max={today}
                    onChange={(e) => update("dateOfBirth", e.target.value)}
                  />
                </label>
                <label className="span-2">
                  Address
                  <textarea value={form.address} onChange={(e) => update("address", e.target.value)} rows={2} />
                </label>
              </div>
            </section>

            <section className="dashboard-section">
              <h2>Board appointment</h2>
              <div className="form-grid">
                <label>
                  Classification *
                  <select
                    value={form.classification}
                    onChange={(e) => update("classification", e.target.value as DirectorClassification)}
                  >
                    {CLASSIFICATIONS.map((c) => (
                      <option key={c} value={c}>
                        {CLASSIFICATION_LABELS[c]}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  Date of first appointment
                  <input
                    type="date"
                    value={form.appointmentDate}
                    onChange={(e) => update("appointmentDate", e.target.value)}
                  />
                </label>
                <label>
                  Date re-elected
                  <input
                    type="date"
                    value={form.reElectionDate}
                    onChange={(e) => update("reElectionDate", e.target.value)}
                  />
                </label>
                <label>
                  Term expiration date
                  <input
                    type="date"
                    value={form.termExpirationDate}
                    onChange={(e) => update("termExpirationDate", e.target.value)}
                  />
                </label>
              </div>
            </section>

            <section className="dashboard-section">
              <h2>Professional background</h2>
              <div className="form-grid">
                <label className="span-2">
                  Profession
                  <input
                    value={form.profession}
                    onChange={(e) => update("profession", e.target.value)}
                    maxLength={255}
                  />
                </label>
                <label className="span-2">
                  Qualification / education
                  <textarea
                    value={form.qualification}
                    onChange={(e) => update("qualification", e.target.value)}
                    rows={3}
                  />
                </label>
                <label className="span-2">
                  Experience
                  <textarea value={form.experience} onChange={(e) => update("experience", e.target.value)} rows={4} />
                </label>
                {!isEdit && (
                  <label className="span-2">
                    CV (PDF or Word, up to 10MB)
                    <input ref={cvInputRef} type="file" accept=".pdf,.doc,.docx" />
                  </label>
                )}
              </div>
            </section>

            <div className="form-actions">
              <button type="button" className="secondary" onClick={() => navigate(backTo)} disabled={saving}>
                Cancel
              </button>
              <button type="submit" disabled={saving}>
                {saving ? "Saving..." : isEdit ? "Save changes" : "Add director"}
              </button>
            </div>
          </form>
        )}
      </main>
    </div>
  );
}
