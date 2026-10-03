import axios from "axios";
import { apiClient } from "./client";

/**
 * Opens the print-ready HTML report in a new tab. The tab is opened synchronously by the
 * caller (inside the click handler) so pop-up blockers allow it, then pointed at the report
 * once it has been fetched with the user's token.
 */
export async function openEvaluationReport(evaluationId: string, tab: Window | null): Promise<void> {
  try {
    const { data } = await apiClient.get<Blob>(`/api/evaluations/${evaluationId}/report`, {
      responseType: "blob",
    });
    const url = window.URL.createObjectURL(data);
    if (tab) {
      tab.location.href = url;
    } else {
      window.open(url, "_blank");
    }
    window.setTimeout(() => window.URL.revokeObjectURL(url), 60_000);
  } catch (error) {
    tab?.close();
    // With responseType "blob" the JSON error body arrives as a Blob; decode it so
    // extractErrorMessage can show the server's message.
    if (axios.isAxiosError(error) && error.response?.data instanceof Blob) {
      try {
        error.response.data = JSON.parse(await error.response.data.text());
      } catch {
        // Not JSON — fall through with the generic message.
      }
    }
    throw error;
  }
}
