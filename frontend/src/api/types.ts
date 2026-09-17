export type Role = "SUPER_ADMIN" | "ORG_ADMIN" | "COMPANY_SECRETARY" | "EVALUATOR" | "DIRECTOR";
export type UserStatus = "ACTIVE" | "DISABLED";

export interface UserSummary {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  role: Role;
  status: UserStatus;
  organizationId: string;
  organizationName: string;
}

export interface AuthResponse {
  accessToken: string;
  user: UserSummary;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface SignupPayload {
  organizationName: string;
  adminFirstName: string;
  adminLastName: string;
  adminEmail: string;
  adminPassword: string;
}

export type DirectorClassification =
  | "CHAIRMAN"
  | "EXECUTIVE_DIRECTOR"
  | "NON_EXECUTIVE_DIRECTOR"
  | "INDEPENDENT_NON_EXECUTIVE_DIRECTOR"
  | "CEO_MD";

export interface BoardSummary {
  id: string;
  name: string;
  effectiveDate: string | null;
  notes: string | null;
}

export interface CreateBoardPayload {
  name: string;
  effectiveDate?: string;
  notes?: string;
}

export interface DirectorSummary {
  id: string;
  boardId: string;
  name: string;
  email: string | null;
  classification: DirectorClassification;
  appointmentDate: string | null;
  termExpirationDate: string | null;
}

export interface CreateDirectorPayload {
  boardId: string;
  name: string;
  email?: string;
  classification: DirectorClassification;
  appointmentDate?: string;
  termExpirationDate?: string;
}

export type CommitteeMemberRole = "CHAIR" | "MEMBER";

export interface CommitteeMemberSummary {
  directorId: string;
  role: CommitteeMemberRole;
}

export interface CommitteeSummary {
  id: string;
  boardId: string;
  name: string;
  meetingFrequency: string | null;
  mandate: string | null;
  members: CommitteeMemberSummary[];
}

export interface CreateCommitteePayload {
  boardId: string;
  name: string;
  meetingFrequency?: string;
  mandate?: string;
}
