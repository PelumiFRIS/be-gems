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
  hasPortalAccess: boolean;
}

export interface InviteDirectorResponse {
  email: string;
  temporaryPassword: string;
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

export type EvaluationType = "BOARD" | "DIRECTOR_PEER";
export type EvaluationStatus = "DRAFT" | "LAUNCHED" | "CLOSED" | "SCORED";
export type RespondentStatus = "INVITED" | "IN_PROGRESS" | "SUBMITTED";
export type ConfidentialityMode = "IDENTIFIED" | "CONFIDENTIAL" | "ANONYMOUS";
export type ResponseType = "RATING_1_5" | "YES_NO" | "YES_NO_PARTIALLY" | "NARRATIVE" | "PERCENTAGE" | "NUMERIC";

export interface EvaluationSummary {
  id: string;
  boardId: string;
  evaluationType: EvaluationType;
  subjectDirectorId: string | null;
  subjectDirectorName: string | null;
  year: number;
  status: EvaluationStatus;
  startDate: string | null;
  closeDate: string | null;
}

export interface CreateEvaluationPayload {
  boardId: string;
  evaluationType: EvaluationType;
  subjectDirectorId?: string;
  year: number;
}

export interface RespondentSummary {
  id: string;
  directorId: string;
  directorName: string;
  confidentialityMode: ConfidentialityMode;
  status: RespondentStatus;
  submittedAt: string | null;
}

export interface EvaluationDetail {
  evaluation: EvaluationSummary;
  respondents: RespondentSummary[];
}

export interface AddRespondentPayload {
  directorId: string;
  confidentialityMode: ConfidentialityMode;
}

export interface QuestionWithAnswer {
  questionId: string;
  dimensionId: string;
  text: string;
  responseType: ResponseType;
  mandatory: boolean;
  ratingValue: number | null;
  textValue: string | null;
  numericValue: number | null;
}

export interface SaveResponsePayload {
  ratingValue?: number;
  textValue?: string;
  numericValue?: number;
}

export interface MyEvaluationSummary {
  evaluationId: string;
  evaluationType: EvaluationType;
  subjectDirectorName: string | null;
  year: number;
  evaluationStatus: EvaluationStatus;
  myStatus: RespondentStatus;
  totalQuestions: number;
  answeredQuestions: number;
}

export type ScoreScopeType = "DIMENSION" | "BOARD_OVERALL" | "BGEI_CATEGORY" | "BGEI_OVERALL" | "DIRECTOR_OVERALL";

export interface DimensionSummary {
  id: string;
  code: string;
  name: string;
  defaultWeightPct: number;
  bgeiCategory: string | null;
}

export interface FrameworkDetail {
  id: string;
  code: string;
  name: string;
  version: string;
  dimensions: DimensionSummary[];
}

export interface ScoreRowSummary {
  scopeType: ScoreScopeType;
  dimensionId: string | null;
  dimensionName: string | null;
  bgeiCategory: string | null;
  rawScore: number | null;
  weightedScore: number | null;
  maturityLevel: number | null;
  maturityLabel: string | null;
  bgeiBandLabel: string | null;
}

export type FindingSeverity = "CRITICAL" | "HIGH" | "MEDIUM" | "LOW" | "OBSERVATION";

export interface FindingSummary {
  id: string;
  evaluationId: string;
  dimensionId: string | null;
  dimensionName: string | null;
  description: string;
  severity: FindingSeverity;
  evidence: string | null;
  regulatoryReference: string | null;
  rootCause: string | null;
  riskImplication: string | null;
  createdAt: string;
}

export interface CreateFindingPayload {
  dimensionId?: string;
  description: string;
  severity: FindingSeverity;
  evidence?: string;
  regulatoryReference?: string;
  rootCause?: string;
  riskImplication?: string;
}

export type RecommendationPriority = "HIGH" | "MEDIUM" | "LOW";
export type RecommendationStatus = "OPEN" | "IN_PROGRESS" | "COMPLETED" | "DEFERRED";

export interface RecommendationSummary {
  id: string;
  findingId: string;
  recommendedAction: string;
  responsiblePerson: string | null;
  committeeResponsible: string | null;
  targetDate: string | null;
  priority: RecommendationPriority;
  status: RecommendationStatus;
  createdAt: string;
}

export interface CreateRecommendationPayload {
  recommendedAction: string;
  responsiblePerson?: string;
  committeeResponsible?: string;
  targetDate?: string;
  priority: RecommendationPriority;
  status: RecommendationStatus;
}

export type ActionStatus = "NOT_STARTED" | "IN_PROGRESS" | "COMPLETED";

export interface ActionSummary {
  id: string;
  findingId: string;
  description: string;
  owner: string | null;
  approver: string | null;
  dueDate: string | null;
  status: ActionStatus;
  evidence: string | null;
  closureDate: string | null;
  overdue: boolean;
  createdAt: string;
}

export interface CreateActionPayload {
  description: string;
  owner?: string;
  approver?: string;
  dueDate?: string;
  evidence?: string;
}

export interface UpdateActionPayload {
  description: string;
  owner?: string;
  approver?: string;
  dueDate?: string;
  status: ActionStatus;
  evidence?: string;
}

export interface ActionRegisterRow {
  id: string;
  findingId: string;
  description: string;
  owner: string | null;
  approver: string | null;
  dueDate: string | null;
  status: ActionStatus;
  evidence: string | null;
  closureDate: string | null;
  overdue: boolean;
  createdAt: string;
  evaluationId: string | null;
  evaluationYear: number;
  evaluationType: EvaluationType | null;
  findingDescription: string | null;
  findingSeverity: FindingSeverity | null;
}
