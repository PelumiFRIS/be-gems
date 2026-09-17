package com.fris.begems.board;

import com.fris.begems.board.dto.BoardSummary;
import com.fris.begems.board.dto.CreateBoardRequest;
import com.fris.begems.common.ApiException;
import com.fris.begems.security.AppUserPrincipal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BoardService {

    private final BoardRepository boardRepository;

    public BoardService(BoardRepository boardRepository) {
        this.boardRepository = boardRepository;
    }

    public List<BoardSummary> listForOrganization(AppUserPrincipal principal) {
        return boardRepository.findByOrganizationId(principal.getOrganizationId()).stream()
                .map(BoardSummary::from)
                .toList();
    }

    public BoardSummary create(AppUserPrincipal principal, CreateBoardRequest request) {
        if (!boardRepository.findByOrganizationId(principal.getOrganizationId()).isEmpty()) {
            throw ApiException.conflict("This organisation already has a board — multi-board support is a later phase");
        }
        Board board = Board.create(principal.getOrganizationId(), request.name(), request.effectiveDate(),
                request.notes());
        boardRepository.save(board);
        return BoardSummary.from(board);
    }
}
