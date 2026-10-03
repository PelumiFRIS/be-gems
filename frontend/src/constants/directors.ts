import type { CommitteeMemberRole, DirectorClassification, Role } from "../api/types";

export const CLASSIFICATION_LABELS: Record<DirectorClassification, string> = {
  CHAIRMAN: "Chairman",
  MD_CEO: "MD/CEO",
  EXECUTIVE_DIRECTOR: "Executive Director",
  NON_EXECUTIVE_DIRECTOR: "Non-Executive Director",
  INDEPENDENT_NON_EXECUTIVE_DIRECTOR: "Independent Non-Executive Director",
};

export const CLASSIFICATIONS = Object.keys(CLASSIFICATION_LABELS) as DirectorClassification[];

export const COMMITTEE_ROLE_LABELS: Record<CommitteeMemberRole, string> = {
  CHAIR: "Chair",
  MEMBER: "Member",
};

export function canManageBoard(role: Role | undefined): boolean {
  return role === "ORG_ADMIN" || role === "COMPANY_SECRETARY";
}
