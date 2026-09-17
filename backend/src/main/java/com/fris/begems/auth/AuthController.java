package com.fris.begems.auth;

import com.fris.begems.audit.AuditAction;
import com.fris.begems.audit.AuditEntityType;
import com.fris.begems.audit.AuditLogService;
import com.fris.begems.auth.dto.AuthResponse;
import com.fris.begems.auth.dto.LoginRequest;
import com.fris.begems.common.ApiException;
import com.fris.begems.organization.OrganizationRepository;
import com.fris.begems.security.AppUserPrincipal;
import com.fris.begems.security.JwtService;
import com.fris.begems.user.User;
import com.fris.begems.user.UserRepository;
import com.fris.begems.user.dto.UserSummary;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final AuditLogService auditLogService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService,
            UserRepository userRepository, OrganizationRepository organizationRepository,
            AuditLogService auditLogService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();

        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> ApiException.notFound("User not found"));
        String organizationName = organizationRepository.findById(principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Organization not found"))
                .getName();

        String token = jwtService.issueToken(
                principal.getUserId(), principal.getOrganizationId(), principal.getUsername(), principal.getRole());

        auditLogService.record(principal, AuditAction.LOGIN, AuditEntityType.AUTH, null, "Signed in");

        return new AuthResponse(token, UserSummary.from(user, organizationName));
    }
}
