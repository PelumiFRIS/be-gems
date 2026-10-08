package com.fris.begems.user;

import com.fris.begems.audit.AuditAction;
import com.fris.begems.audit.AuditEntityType;
import com.fris.begems.audit.AuditLogService;
import com.fris.begems.common.ApiException;
import com.fris.begems.notification.NotificationService;
import com.fris.begems.organization.Organization;
import com.fris.begems.organization.OrganizationRepository;
import com.fris.begems.security.AppUserPrincipal;
import com.fris.begems.security.TemporaryPasswordGenerator;
import com.fris.begems.user.dto.ChangePasswordRequest;
import com.fris.begems.user.dto.CreateUserRequest;
import com.fris.begems.user.dto.CreatedUserResponse;
import com.fris.begems.user.dto.TemporaryPasswordResponse;
import com.fris.begems.user.dto.UserSummary;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    /**
     * Roles an Org Admin can hand out. Directors get accounts through the director
     * invite on Board Setup so the login stays tied to their director record, and
     * SUPER_ADMIN is platform-level, not something an organisation can grant.
     */
    private static final Set<Role> ASSIGNABLE_ROLES = Set.of(Role.ORG_ADMIN, Role.COMPANY_SECRETARY,
            Role.EVALUATOR);

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;
    private final NotificationService notificationService;

    public UserService(UserRepository userRepository, OrganizationRepository organizationRepository,
            PasswordEncoder passwordEncoder, AuditLogService auditLogService,
            TemporaryPasswordGenerator temporaryPasswordGenerator, NotificationService notificationService) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.temporaryPasswordGenerator = temporaryPasswordGenerator;
        this.notificationService = notificationService;
    }

    public UserSummary getCurrentUser(AppUserPrincipal principal) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> ApiException.notFound("User not found"));
        return toSummary(user);
    }

    public List<UserSummary> listOrganizationUsers(AppUserPrincipal principal) {
        String organizationName = organizationName(principal.getOrganizationId());
        return userRepository.findByOrganizationId(principal.getOrganizationId()).stream()
                .sorted(Comparator.comparing(User::getFirstName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(User::getLastName, String.CASE_INSENSITIVE_ORDER))
                .map(user -> UserSummary.from(user, organizationName))
                .toList();
    }

    @Transactional
    public CreatedUserResponse createUser(AppUserPrincipal principal, CreateUserRequest request) {
        requireAssignable(request.role());
        String email = request.email().trim();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with this email already exists");
        }
        String temporaryPassword = temporaryPasswordGenerator.generate();
        User user = User.create(principal.getOrganizationId(), email, passwordEncoder.encode(temporaryPassword),
                request.firstName().trim(), request.lastName().trim(), request.role());
        userRepository.save(user);
        auditLogService.record(principal, AuditAction.USER_CREATED, AuditEntityType.USER, user.getId(),
                "Created user \"" + fullName(user) + "\" (" + user.getRole() + ")");
        notificationService.sendAccountCreated(user);
        return new CreatedUserResponse(toSummary(user), temporaryPassword);
    }

    @Transactional
    public UserSummary changeRole(AppUserPrincipal principal, UUID userId, Role role) {
        User user = requireOtherUserInOrganization(principal, userId, "You can't change your own role");
        if (user.getRole() == Role.DIRECTOR) {
            throw ApiException.badRequest("Director accounts are managed from Board Setup");
        }
        requireAssignable(role);
        if (user.getRole() != role) {
            user.setRole(role);
            if (role != Role.ORG_ADMIN) {
                user.setCompanySecretaryAccess(false);
            }
            touch(user);
            auditLogService.record(principal, AuditAction.USER_ROLE_CHANGED, AuditEntityType.USER, user.getId(),
                    "Changed \"" + fullName(user) + "\" to " + role);
        }
        return toSummary(user);
    }

    @Transactional
    public UserSummary changeStatus(AppUserPrincipal principal, UUID userId, UserStatus status) {
        User user = requireOtherUserInOrganization(principal, userId, "You can't disable your own account");
        if (user.getStatus() != status) {
            user.setStatus(status);
            touch(user);
            AuditAction action = status == UserStatus.DISABLED ? AuditAction.USER_DISABLED : AuditAction.USER_ENABLED;
            auditLogService.record(principal, action, AuditEntityType.USER, user.getId(),
                    (status == UserStatus.DISABLED ? "Disabled" : "Re-enabled") + " \"" + fullName(user) + "\"");
        }
        return toSummary(user);
    }

    /**
     * Lets an Organisation Administrator also act as Company Secretary on the same
     * login. Self-service is allowed: an admin could otherwise just create a second
     * Company Secretary account, so this grants nothing they couldn't already get.
     */
    @Transactional
    public UserSummary changeCompanySecretaryAccess(AppUserPrincipal principal, UUID userId, boolean enabled) {
        User user = userRepository.findByIdAndOrganizationId(userId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("User not found"));
        if (user.getRole() != Role.ORG_ADMIN) {
            throw ApiException.badRequest(
                    "Company Secretary access applies to Organisation Administrators; change this user's role instead");
        }
        if (user.isCompanySecretaryAccess() != enabled) {
            user.setCompanySecretaryAccess(enabled);
            touch(user);
            auditLogService.record(principal,
                    enabled ? AuditAction.COMPANY_SECRETARY_ACCESS_GRANTED : AuditAction.COMPANY_SECRETARY_ACCESS_REVOKED,
                    AuditEntityType.USER, user.getId(),
                    (enabled ? "Gave \"" : "Removed Company Secretary access from \"") + fullName(user)
                            + (enabled ? "\" Company Secretary access" : "\""));
        }
        return toSummary(user);
    }

    @Transactional
    public TemporaryPasswordResponse resetPassword(AppUserPrincipal principal, UUID userId) {
        User user = requireOtherUserInOrganization(principal, userId,
                "Use My Account to change your own password");
        String temporaryPassword = temporaryPasswordGenerator.generate();
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        touch(user);
        auditLogService.record(principal, AuditAction.PASSWORD_RESET, AuditEntityType.USER, user.getId(),
                "Reset the password for \"" + fullName(user) + "\"");
        notificationService.sendPasswordReset(user);
        return new TemporaryPasswordResponse(user.getEmail(), temporaryPassword);
    }

    @Transactional
    public void changeOwnPassword(AppUserPrincipal principal, ChangePasswordRequest request) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> ApiException.notFound("User not found"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("Your current password is incorrect");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw ApiException.badRequest("Choose a new password that's different from your current one");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        touch(user);
        auditLogService.record(principal, AuditAction.PASSWORD_CHANGED, AuditEntityType.USER, user.getId(),
                "Changed their password");
    }

    private User requireOtherUserInOrganization(AppUserPrincipal principal, UUID userId, String selfMessage) {
        if (principal.getUserId().equals(userId)) {
            throw ApiException.badRequest(selfMessage);
        }
        return userRepository.findByIdAndOrganizationId(userId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("User not found"));
    }

    private void requireAssignable(Role role) {
        if (!ASSIGNABLE_ROLES.contains(role)) {
            throw ApiException.badRequest(role == Role.DIRECTOR
                    ? "Directors get portal access through Board Setup"
                    : "This role can't be assigned");
        }
    }

    private void touch(User user) {
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    private static String fullName(User user) {
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }

    private String organizationName(UUID organizationId) {
        return organizationRepository.findById(organizationId).map(Organization::getName).orElse(null);
    }

    private UserSummary toSummary(User user) {
        return UserSummary.from(user, organizationName(user.getOrganizationId()));
    }
}
