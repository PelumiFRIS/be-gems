import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { extractErrorMessage } from "../api/client";
import { getFramework } from "../api/frameworks";
import { getMyQuestions, saveMyResponse, submitMyEvaluation } from "../api/responses";
import type { QuestionWithAnswer } from "../api/types";
import { Sidebar } from "../components/Sidebar";
import { TopBar } from "../components/TopBar";

const RATING_OPTIONS = [1, 2, 3, 4, 5];

export function RespondentQuestionnairePage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [questions, setQuestions] = useState<QuestionWithAnswer[]>([]);
  const [dimensionNames, setDimensionNames] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [savingId, setSavingId] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!id) return;
    Promise.all([getMyQuestions(id), getFramework()])
      .then(([questionList, framework]) => {
        setQuestions(questionList);
        const names: Record<string, string> = {};
        framework.dimensions.forEach((d) => {
          names[d.id] = d.name;
        });
        setDimensionNames(names);
      })
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, [id]);

  const answeredCount = questions.filter(
    (q) => q.ratingValue !== null || q.textValue !== null || q.numericValue !== null,
  ).length;

  async function saveRating(question: QuestionWithAnswer, rating: number) {
    if (!id) return;
    setSavingId(question.questionId);
    setError(null);
    try {
      const updated = await saveMyResponse(id, question.questionId, { ratingValue: rating });
      setQuestions((prev) => prev.map((q) => (q.questionId === question.questionId ? updated : q)));
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setSavingId(null);
    }
  }

  async function saveText(question: QuestionWithAnswer, text: string) {
    if (!id) return;
    setError(null);
    try {
      const updated = await saveMyResponse(id, question.questionId, { textValue: text });
      setQuestions((prev) => prev.map((q) => (q.questionId === question.questionId ? updated : q)));
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  async function saveNumeric(question: QuestionWithAnswer, value: number) {
    if (!id) return;
    setError(null);
    try {
      const updated = await saveMyResponse(id, question.questionId, { numericValue: value });
      setQuestions((prev) => prev.map((q) => (q.questionId === question.questionId ? updated : q)));
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  async function handleSubmit() {
    if (!id) return;
    setError(null);
    setSubmitting(true);
    try {
      await submitMyEvaluation(id);
      navigate("/my-evaluations");
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  const questionsByDimension = new Map<string, QuestionWithAnswer[]>();
  for (const question of questions) {
    const list = questionsByDimension.get(question.dimensionId) ?? [];
    list.push(question);
    questionsByDimension.set(question.dimensionId, list);
  }

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <TopBar />
        <div className="page-header">
          <h1>Questionnaire</h1>
          <p>
            {answeredCount}/{questions.length} answered
          </p>
        </div>

        {error && <p className="form-error">{error}</p>}
        {loading && <p>Loading...</p>}

        {!loading &&
          Array.from(questionsByDimension.entries()).map(([dimensionId, dimensionQuestions]) => (
            <section className="dashboard-section" key={dimensionId}>
              <h2>{dimensionNames[dimensionId] ?? "Dimension"}</h2>
              {dimensionQuestions.map((question) => (
                <div key={question.questionId} className="questionnaire-item">
                  <p>{question.text}</p>
                  {question.responseType === "RATING_1_5" && (
                    <div className="rating-options">
                      {RATING_OPTIONS.map((option) => (
                        <button
                          key={option}
                          type="button"
                          className={question.ratingValue === option ? "" : "secondary"}
                          disabled={savingId === question.questionId}
                          onClick={() => saveRating(question, option)}
                        >
                          {option}
                        </button>
                      ))}
                    </div>
                  )}
                  {question.responseType === "YES_NO" && (
                    <div className="rating-options">
                      <button
                        type="button"
                        className={question.ratingValue === 5 ? "" : "secondary"}
                        onClick={() => saveRating(question, 5)}
                      >
                        Yes
                      </button>
                      <button
                        type="button"
                        className={question.ratingValue === 1 ? "" : "secondary"}
                        onClick={() => saveRating(question, 1)}
                      >
                        No
                      </button>
                    </div>
                  )}
                  {question.responseType === "YES_NO_PARTIALLY" && (
                    <div className="rating-options">
                      <button
                        type="button"
                        className={question.ratingValue === 5 ? "" : "secondary"}
                        onClick={() => saveRating(question, 5)}
                      >
                        Yes
                      </button>
                      <button
                        type="button"
                        className={question.ratingValue === 3 ? "" : "secondary"}
                        onClick={() => saveRating(question, 3)}
                      >
                        Partially
                      </button>
                      <button
                        type="button"
                        className={question.ratingValue === 1 ? "" : "secondary"}
                        onClick={() => saveRating(question, 1)}
                      >
                        No
                      </button>
                    </div>
                  )}
                  {question.responseType === "NARRATIVE" && (
                    <textarea
                      defaultValue={question.textValue ?? ""}
                      onBlur={(e) => saveText(question, e.target.value)}
                      rows={3}
                    />
                  )}
                  {(question.responseType === "PERCENTAGE" || question.responseType === "NUMERIC") && (
                    <input
                      type="number"
                      defaultValue={question.numericValue ?? ""}
                      onBlur={(e) => e.target.value && saveNumeric(question, Number(e.target.value))}
                    />
                  )}
                </div>
              ))}
            </section>
          ))}

        {!loading && questions.length > 0 && (
          <button type="button" onClick={handleSubmit} disabled={submitting}>
            {submitting ? "Submitting..." : "Submit evaluation"}
          </button>
        )}
      </main>
    </div>
  );
}
