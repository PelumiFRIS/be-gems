package com.fris.begems.director;

import com.fris.begems.director.dto.CreateDirectorRequest;
import com.fris.begems.director.dto.DirectorSummary;
import com.fris.begems.security.AppUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/directors")
public class DirectorController {

    private final DirectorService directorService;

    public DirectorController(DirectorService directorService) {
        this.directorService = directorService;
    }

    @GetMapping
    public List<DirectorSummary> list(@AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam UUID boardId) {
        return directorService.listForBoard(principal, boardId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'COMPANY_SECRETARY')")
    public ResponseEntity<DirectorSummary> create(@AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateDirectorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(directorService.create(principal, request));
    }
}
