package com.fris.begems.user;

import com.fris.begems.security.AppUserPrincipal;
import com.fris.begems.user.dto.ChangePasswordRequest;
import com.fris.begems.user.dto.CreateUserRequest;
import com.fris.begems.user.dto.CreatedUserResponse;
import com.fris.begems.user.dto.TemporaryPasswordResponse;
import com.fris.begems.user.dto.UpdateUserRoleRequest;
import com.fris.begems.user.dto.UpdateUserStatusRequest;
import com.fris.begems.user.dto.UserSummary;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserSummary me(@AuthenticationPrincipal AppUserPrincipal principal) {
        return userService.getCurrentUser(principal);
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changeOwnPassword(@AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changeOwnPassword(principal, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public List<UserSummary> listUsers(@AuthenticationPrincipal AppUserPrincipal principal) {
        return userService.listOrganizationUsers(principal);
    }

    @PostMapping
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<CreatedUserResponse> createUser(@AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(principal, request));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public UserSummary changeRole(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRoleRequest request) {
        return userService.changeRole(principal, id, request.role());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public UserSummary changeStatus(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        return userService.changeStatus(principal, id, request.status());
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public TemporaryPasswordResponse resetPassword(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id) {
        return userService.resetPassword(principal, id);
    }
}
