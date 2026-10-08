package com.fris.begems.security;

import com.fris.begems.user.Role;
import com.fris.begems.user.User;
import com.fris.begems.user.UserStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AppUserPrincipal implements UserDetails {

    private final UUID userId;
    private final UUID organizationId;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final boolean companySecretaryAccess;
    private final boolean enabled;

    public AppUserPrincipal(User user) {
        this.userId = user.getId();
        this.organizationId = user.getOrganizationId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole();
        this.companySecretaryAccess = user.actsAs(Role.COMPANY_SECRETARY) && role != Role.COMPANY_SECRETARY;
        this.enabled = user.getStatus() == UserStatus.ACTIVE;
    }

    /** True if this user holds {@code role}, directly or through Company Secretary access. */
    public boolean actsAs(Role candidate) {
        return role == candidate || (candidate == Role.COMPANY_SECRETARY && companySecretaryAccess);
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public List<GrantedAuthority> getAuthorities() {
        if (companySecretaryAccess) {
            return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()),
                    new SimpleGrantedAuthority("ROLE_" + Role.COMPANY_SECRETARY.name()));
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
