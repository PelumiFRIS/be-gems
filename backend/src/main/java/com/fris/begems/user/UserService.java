package com.fris.begems.user;

import com.fris.begems.audit.AuditAction;
import com.fris.begems.audit.AuditEntityType;
import com.fris.begems.audit.AuditLogService;
import com.fris.begems.common.ApiException;
import com.fris.begems.organization.OrganizationRepository;
import com.fris.begems.security.AppUserPrincipal;
import com.fris.begems.user.dto.CreateUserRequest;
import com.fris.begems.user.dto.UserSummary;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public UserService(UserRepository userRepository, OrganizationRepository organizationRepository,
            PasswordEncoder passwordEncoder, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    public UserSummary getCurrentUser(AppUserPrincipal principal) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> ApiException.notFound("User not found"));
        return toSummary(user);
    }

    public List<UserSummary> listOrganizationUsers(AppUserPrincipal principal) {
        return userRepository.findByOrganizationId(principal.getOrganizationId()).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public UserSummary createUser(AppUserPrincipal principal, CreateUserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw ApiException.conflict("An account with this email already exists");
        }
        User user = User.create(principal.getOrganizationId(), request.email(),
                passwordEncoder.encode(request.password()), request.firstName(), request.lastName(), request.role());
        userRepository.save(user);
        auditLogService.record(principal, AuditAction.USER_CREATED, AuditEntityType.USER, user.getId(),
                "Created user \"" + user.getFirstName() + " " + user.getLastName() + "\" (" + user.getRole() + ")");
        return toSummary(user);
    }

    private UserSummary toSummary(User user) {
        String organizationName = organizationRepository.findById(user.getOrganizationId())
                .map(org -> org.getName())
                .orElse(null);
        return UserSummary.from(user, organizationName);
    }
}
