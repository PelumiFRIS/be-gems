package com.fris.begems.board;

import com.fris.begems.board.dto.BoardSummary;
import com.fris.begems.board.dto.CreateBoardRequest;
import com.fris.begems.common.ApiException;
import com.fris.begems.security.AppUserPrincipal;
import com.fris.begems.skill.SkillService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoardService {

    private final BoardRepository boardRepository;
    private final SkillService skillService;

    public BoardService(BoardRepository boardRepository, SkillService skillService) {
        this.boardRepository = boardRepository;
        this.skillService = skillService;
    }

    public List<BoardSummary> listForOrganization(AppUserPrincipal principal) {
        return boardRepository.findByOrganizationId(principal.getOrganizationId()).stream()
                .map(BoardSummary::from)
                .toList();
    }

    @Transactional
    public BoardSummary create(AppUserPrincipal principal, CreateBoardRequest request) {
        if (!boardRepository.findByOrganizationId(principal.getOrganizationId()).isEmpty()) {
            throw ApiException.conflict("This organisation already has a board — multi-board support is a later phase");
        }
        Board board = Board.create(principal.getOrganizationId(), request.name(), request.effectiveDate(),
                request.notes());
        boardRepository.save(board);
        skillService.seedDefaults(board.getOrganizationId(), board.getId());
        return BoardSummary.from(board);
    }
}
