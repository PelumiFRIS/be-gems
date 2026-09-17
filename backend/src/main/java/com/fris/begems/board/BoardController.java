package com.fris.begems.board;

import com.fris.begems.board.dto.BoardSummary;
import com.fris.begems.board.dto.CreateBoardRequest;
import com.fris.begems.security.AppUserPrincipal;
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
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    @GetMapping
    public List<BoardSummary> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        return boardService.listForOrganization(principal);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'COMPANY_SECRETARY')")
    public ResponseEntity<BoardSummary> create(@AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateBoardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(boardService.create(principal, request));
    }
}
