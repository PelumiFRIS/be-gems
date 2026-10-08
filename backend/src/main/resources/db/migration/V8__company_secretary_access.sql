-- An Organisation Administrator who is also the organisation's Company Secretary
-- holds both sets of permissions on one login (board-portal's dual-role pattern).
ALTER TABLE users ADD COLUMN company_secretary_access BOOLEAN NOT NULL DEFAULT FALSE;

-- The person who signed the organisation up is, in practice, its Company Secretary;
-- new signups get the same default in OrganizationService.
UPDATE users u
SET company_secretary_access = TRUE
WHERE u.role = 'ORG_ADMIN'
  AND u.created_at = (
      SELECT MIN(first_admin.created_at)
      FROM users first_admin
      WHERE first_admin.organization_id = u.organization_id
        AND first_admin.role = 'ORG_ADMIN'
  );
