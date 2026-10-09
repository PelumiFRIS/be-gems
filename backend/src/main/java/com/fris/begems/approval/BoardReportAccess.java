package com.fris.begems.approval;

import com.fris.begems.director.Director;
import com.fris.begems.director.DirectorClassification;
import com.fris.begems.director.DirectorRepository;
import com.fris.begems.evaluation.Evaluation;
import com.fris.begems.security.AppUserPrincipal;
import com.fris.begems.user.Role;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Who may read and act on a Board Evaluation Report. Staff see it at every stage; the Chairman sees it from
 * Chairman/Board Approval; every director on the board sees it once it is final.
 */
@Component
public class BoardReportAccess {

    private final DirectorRepository directorRepository;

    public BoardReportAccess(DirectorRepository directorRepository) {
        this.directorRepository = directorRepository;
    }

    public static boolean isStaff(AppUserPrincipal principal) {
        return principal.actsAs(Role.ORG_ADMIN) || principal.actsAs(Role.COMPANY_SECRETARY)
                || principal.actsAs(Role.EVALUATOR);
    }

    public Optional<Director> boardSeat(AppUserPrincipal principal, Evaluation evaluation) {
        if (principal.getRole() != Role.DIRECTOR) {
            return Optional.empty();
        }
        return directorRepository.findByUserId(principal.getUserId())
                .filter(d -> d.getBoardId().equals(evaluation.getBoardId()));
    }

    public boolean isChairman(AppUserPrincipal principal, Evaluation evaluation) {
        return boardSeat(principal, evaluation)
                .map(d -> d.getClassification() == DirectorClassification.CHAIRMAN)
                .orElse(false);
    }

    public boolean canView(AppUserPrincipal principal, Evaluation evaluation) {
        if (isStaff(principal)) {
            return true;
        }
        ReportStage stage = evaluation.getReportStage();
        if (stage == ReportStage.FINAL) {
            return boardSeat(principal, evaluation).isPresent();
        }
        return stage == ReportStage.BOARD_APPROVAL && isChairman(principal, evaluation);
    }
}
