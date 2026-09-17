package com.fris.begems.user;

import com.fris.begems.security.AppUserPrincipal;
import com.fris.begems.user.dto.CreateUserRequest;
import com.fris.begems.user.dto.UserSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

    @GetMapping
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public List<UserSummary> listUsers(@AuthenticationPrincipal AppUserPrincipal principal) {
        return userService.listOrganizationUsers(principal);
    }

    @PostMapping
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<UserSummary> createUser(@AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(principal, request));
    }
}
