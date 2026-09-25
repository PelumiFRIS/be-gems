import { apiClient } from "./client";
import type { AttachmentSummary } from "./types";

export async function listAttachments(findingId: string): Promise<AttachmentSummary[]> {
  const { data } = await apiClient.get<AttachmentSummary[]>(`/api/findings/${findingId}/attachments`);
  return data;
}

export async function uploadAttachment(findingId: string, file: File): Promise<AttachmentSummary> {
  const formData = new FormData();
  formData.append("file", file);
  const { data } = await apiClient.post<AttachmentSummary>(`/api/findings/${findingId}/attachments`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return data;
}

export async function deleteAttachment(attachmentId: string): Promise<void> {
  await apiClient.delete(`/api/attachments/${attachmentId}`);
}

export async function downloadAttachment(attachment: AttachmentSummary): Promise<void> {
  const { data } = await apiClient.get<Blob>(`/api/attachments/${attachment.id}/download`, {
    responseType: "blob",
  });
  const url = window.URL.createObjectURL(data);
  const link = document.createElement("a");
  link.href = url;
  link.download = attachment.fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}
