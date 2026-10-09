package com.fris.begems.skill;

import com.fris.begems.audit.AuditAction;
import com.fris.begems.audit.AuditEntityType;
import com.fris.begems.audit.AuditLogService;
import com.fris.begems.board.Board;
import com.fris.begems.board.BoardRepository;
import com.fris.begems.common.ApiException;
import com.fris.begems.director.Director;
import com.fris.begems.director.DirectorRepository;
import com.fris.begems.security.AppUserPrincipal;
import com.fris.begems.skill.dto.SkillRatingRequest;
import com.fris.begems.skill.dto.SkillRequest;
import com.fris.begems.skill.dto.SkillsMatrix;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SkillService {

    private final BoardSkillRepository skillRepository;
    private final DirectorSkillRatingRepository ratingRepository;
    private final BoardRepository boardRepository;
    private final DirectorRepository directorRepository;
    private final AuditLogService auditLogService;

    public SkillService(BoardSkillRepository skillRepository, DirectorSkillRatingRepository ratingRepository,
            BoardRepository boardRepository, DirectorRepository directorRepository,
            AuditLogService auditLogService) {
        this.skillRepository = skillRepository;
        this.ratingRepository = ratingRepository;
        this.boardRepository = boardRepository;
        this.directorRepository = directorRepository;
        this.auditLogService = auditLogService;
    }

    public SkillsMatrix matrix(AppUserPrincipal principal, UUID boardId) {
        requireBoard(principal, boardId);
        return matrixFor(boardId);
    }

    /** Also used by the board report, which has already checked access. */
    public SkillsMatrix matrixFor(UUID boardId) {
        List<BoardSkill> skills = skillRepository.findByBoardIdOrderByDisplayOrderAscNameAsc(boardId);
        List<DirectorSkillRating> ratings = ratingRepository
                .findBySkillIdIn(skills.stream().map(BoardSkill::getId).toList());
        return SkillsAnalysis.build(boardId, directorRepository.findByBoardId(boardId), skills, ratings);
    }

    @Transactional
    public void seedDefaults(UUID organizationId, UUID boardId) {
        skillRepository.saveAll(DefaultSkills.forBoard(organizationId, boardId));
    }

    @Transactional
    public SkillsMatrix addSkill(AppUserPrincipal principal, UUID boardId, SkillRequest request) {
        boardRepository.lockByIdAndOrganizationId(boardId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Board not found"));
        String name = request.name().trim();
        if (skillRepository.existsByBoardIdAndNameIgnoreCase(boardId, name)) {
            throw ApiException.conflict("This competency is already in the framework");
        }
        int nextOrder = skillRepository.findByBoardIdOrderByDisplayOrderAscNameAsc(boardId).stream()
                .mapToInt(BoardSkill::getDisplayOrder).max().orElse(0) + 1;
        BoardSkill skill = BoardSkill.create(principal.getOrganizationId(), boardId, name, request.requiredLevel(),
                request.futureFocus(), nextOrder);
        skillRepository.save(skill);
        auditLogService.record(principal, AuditAction.SKILL_ADDED, AuditEntityType.SKILL, skill.getId(),
                "Added \"" + name + "\" to the skills framework (" + request.requiredLevel() + " requirement)");
        return matrixFor(boardId);
    }

    @Transactional
    public SkillsMatrix updateSkill(AppUserPrincipal principal, UUID skillId, SkillRequest request) {
        BoardSkill skill = requireSkill(principal, skillId);
        String name = request.name().trim();
        if (skillRepository.existsByBoardIdAndNameIgnoreCaseAndIdNot(skill.getBoardId(), name, skillId)) {
            throw ApiException.conflict("This competency is already in the framework");
        }
        skill.setName(name);
        skill.setRequiredLevel(request.requiredLevel());
        skill.setFutureFocus(request.futureFocus());
        skillRepository.save(skill);
        auditLogService.record(principal, AuditAction.SKILL_UPDATED, AuditEntityType.SKILL, skill.getId(),
                "Updated \"" + name + "\" (" + request.requiredLevel() + " requirement"
                        + (request.futureFocus() ? ", future priority" : "") + ")");
        return matrixFor(skill.getBoardId());
    }

    @Transactional
    public SkillsMatrix removeSkill(AppUserPrincipal principal, UUID skillId) {
        BoardSkill skill = requireSkill(principal, skillId);
        ratingRepository.deleteBySkillId(skillId);
        skillRepository.delete(skill);
        auditLogService.record(principal, AuditAction.SKILL_REMOVED, AuditEntityType.SKILL, skill.getId(),
                "Removed \"" + skill.getName() + "\" from the skills framework");
        return matrixFor(skill.getBoardId());
    }

    @Transactional
    public SkillsMatrix rate(AppUserPrincipal principal, UUID skillId, UUID directorId, SkillRatingRequest request) {
        BoardSkill skill = requireSkill(principal, skillId);
        Director director = directorRepository.findByIdAndOrganizationId(directorId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Director not found"));
        if (!director.getBoardId().equals(skill.getBoardId())) {
            throw ApiException.badRequest("This director is not on the competency's board");
        }

        DirectorSkillRating existing = ratingRepository.findByDirectorIdAndSkillId(directorId, skillId).orElse(null);
        Integer previous = existing == null ? null : existing.getRating();
        if (request.rating() == null) {
            if (existing != null) {
                ratingRepository.delete(existing);
            }
        } else if (existing == null) {
            ratingRepository.save(DirectorSkillRating.create(principal.getOrganizationId(), directorId, skillId,
                    request.rating()));
        } else {
            existing.setRating(request.rating());
            existing.setUpdatedAt(Instant.now());
            ratingRepository.save(existing);
        }

        if (!Objects.equals(previous, request.rating())) {
            auditLogService.record(principal, AuditAction.SKILL_RATING_CHANGED, AuditEntityType.SKILL,
                    skill.getId(), request.rating() == null
                            ? "Cleared " + director.getName() + "'s " + skill.getName() + " rating"
                            : "Rated " + director.getName() + " " + request.rating() + " in " + skill.getName());
        }
        return matrixFor(skill.getBoardId());
    }

    private Board requireBoard(AppUserPrincipal principal, UUID boardId) {
        return boardRepository.findByIdAndOrganizationId(boardId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Board not found"));
    }

    private BoardSkill requireSkill(AppUserPrincipal principal, UUID skillId) {
        return skillRepository.findByIdAndOrganizationId(skillId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Competency not found"));
    }
}
