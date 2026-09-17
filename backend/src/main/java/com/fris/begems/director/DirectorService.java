package com.fris.begems.director;

import com.fris.begems.board.BoardRepository;
import com.fris.begems.common.ApiException;
import com.fris.begems.director.dto.CreateDirectorRequest;
import com.fris.begems.director.dto.DirectorSummary;
import com.fris.begems.security.AppUserPrincipal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DirectorService {

    private final DirectorRepository directorRepository;
    private final BoardRepository boardRepository;

    public DirectorService(DirectorRepository directorRepository, BoardRepository boardRepository) {
        this.directorRepository = directorRepository;
        this.boardRepository = boardRepository;
    }

    public List<DirectorSummary> listForBoard(AppUserPrincipal principal, UUID boardId) {
        requireBoardInOrganization(principal, boardId);
        return directorRepository.findByBoardId(boardId).stream()
                .map(DirectorSummary::from)
                .toList();
    }

    public DirectorSummary create(AppUserPrincipal principal, CreateDirectorRequest request) {
        requireBoardInOrganization(principal, request.boardId());
        Director director = Director.create(principal.getOrganizationId(), request.boardId(), request.name(),
                request.email(), request.classification(), request.appointmentDate(), request.termExpirationDate());
        directorRepository.save(director);
        return DirectorSummary.from(director);
    }

    private void requireBoardInOrganization(AppUserPrincipal principal, UUID boardId) {
        boardRepository.findByIdAndOrganizationId(boardId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Board not found"));
    }
}
