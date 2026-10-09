package com.fris.begems.skill;

import com.fris.begems.security.AppUserPrincipal;
import com.fris.begems.skill.dto.SkillRatingRequest;
import com.fris.begems.skill.dto.SkillRequest;
import com.fris.begems.skill.dto.SkillsMatrix;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Individual directors' ratings are sensitive, so the matrix is staff-only; editing
 * follows Board Setup permissions (Org Admin and Company Secretary).
 */
@RestController
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping("/api/boards/{boardId}/skills-matrix")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'COMPANY_SECRETARY', 'EVALUATOR')")
    public SkillsMatrix matrix(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID boardId) {
        return skillService.matrix(principal, boardId);
    }

    @PostMapping("/api/boards/{boardId}/skills")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'COMPANY_SECRETARY')")
    public SkillsMatrix addSkill(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID boardId,
            @Valid @RequestBody SkillRequest request) {
        return skillService.addSkill(principal, boardId, request);
    }

    @PutMapping("/api/skills/{skillId}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'COMPANY_SECRETARY')")
    public SkillsMatrix updateSkill(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID skillId,
            @Valid @RequestBody SkillRequest request) {
        return skillService.updateSkill(principal, skillId, request);
    }

    @DeleteMapping("/api/skills/{skillId}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'COMPANY_SECRETARY')")
    public SkillsMatrix removeSkill(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID skillId) {
        return skillService.removeSkill(principal, skillId);
    }

    @PutMapping("/api/skills/{skillId}/ratings/{directorId}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'COMPANY_SECRETARY')")
    public SkillsMatrix rate(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID skillId,
            @PathVariable UUID directorId, @Valid @RequestBody SkillRatingRequest request) {
        return skillService.rate(principal, skillId, directorId, request);
    }
}
