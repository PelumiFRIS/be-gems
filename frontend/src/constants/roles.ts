import type { Role, UserSummary } from "../api/types";

export const ROLE_LABELS: Record<Role, string> = {
  SUPER_ADMIN: "Super Administrator",
  ORG_ADMIN: "Organisation Administrator",
  COMPANY_SECRETARY: "Company Secretary",
  EVALUATOR: "Evaluator",
  DIRECTOR: "Director",
};

type RoleHolder = Pick<UserSummary, "role" | "companySecretaryAccess">;

/** Mirrors the backend: an Organisation Administrator with Company Secretary access holds both roles. */
export function actsAs(user: RoleHolder | null | undefined, role: Role): boolean {
  if (!user) return false;
  return user.role === role || (role === "COMPANY_SECRETARY" && user.role === "ORG_ADMIN" && user.companySecretaryAccess);
}

export function canManageEvaluations(user: RoleHolder | null | undefined): boolean {
  return actsAs(user, "COMPANY_SECRETARY") || actsAs(user, "EVALUATOR");
}

export function roleLabel(user: RoleHolder): string {
  return user.role === "ORG_ADMIN" && user.companySecretaryAccess
    ? `${ROLE_LABELS.ORG_ADMIN} & ${ROLE_LABELS.COMPANY_SECRETARY}`
    : ROLE_LABELS[user.role];
}
