package com.fris.begems.organization;

import com.fris.begems.audit.AuditAction;
import com.fris.begems.audit.AuditEntityType;
import com.fris.begems.audit.AuditLogService;
import com.fris.begems.auth.dto.AuthResponse;
import com.fris.begems.common.ApiException;
import com.fris.begems.organization.dto.OrganizationSignupRequest;
import com.fris.begems.security.AppUserPrincipal;
import com.fris.begems.security.JwtService;
import com.fris.begems.user.Role;
import com.fris.begems.user.User;
import com.fris.begems.user.UserRepository;
import com.fris.begems.user.dto.UserSummary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;

    public OrganizationService(OrganizationRepository organizationRepository, UserRepository userRepository,
            PasswordEncoder passwordEncoder, JwtService jwtService, AuditLogService auditLogService) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public AuthResponse signup(OrganizationSignupRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.adminEmail())) {
            throw ApiException.conflict("An account with this email already exists");
        }

        Organization organization = Organization.create(request.organizationName());
        organizationRepository.save(organization);

        User admin = User.create(
                organization.getId(),
                request.adminEmail(),
                passwordEncoder.encode(request.adminPassword()),
                request.adminFirstName(),
                request.adminLastName(),
                Role.ORG_ADMIN);
        userRepository.save(admin);

        auditLogService.record(new AppUserPrincipal(admin), AuditAction.ORGANIZATION_SIGNUP,
                AuditEntityType.ORGANIZATION, organization.getId(),
                "Created organization \"" + organization.getName() + "\" and admin account");

        String token = jwtService.issueToken(admin.getId(), organization.getId(), admin.getEmail(), admin.getRole());
        return new AuthResponse(token, UserSummary.from(admin, organization.getName()));
    }
}
