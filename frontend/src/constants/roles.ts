import type { Role } from "../api/types";

export const ROLE_LABELS: Record<Role, string> = {
  SUPER_ADMIN: "Super Administrator",
  ORG_ADMIN: "Organisation Administrator",
  COMPANY_SECRETARY: "Company Secretary",
  EVALUATOR: "Evaluator",
  DIRECTOR: "Director",
};
