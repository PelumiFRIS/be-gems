package com.fris.begems.committee;

import com.fris.begems.board.BoardRepository;
import com.fris.begems.committee.dto.AddCommitteeMemberRequest;
import com.fris.begems.committee.dto.CommitteeMemberSummary;
import com.fris.begems.committee.dto.CommitteeSummary;
import com.fris.begems.committee.dto.CreateCommitteeRequest;
import com.fris.begems.common.ApiException;
import com.fris.begems.security.AppUserPrincipal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CommitteeService {

    private final CommitteeRepository committeeRepository;
    private final CommitteeMemberRepository committeeMemberRepository;
    private final BoardRepository boardRepository;

    public CommitteeService(CommitteeRepository committeeRepository,
            CommitteeMemberRepository committeeMemberRepository, BoardRepository boardRepository) {
        this.committeeRepository = committeeRepository;
        this.committeeMemberRepository = committeeMemberRepository;
        this.boardRepository = boardRepository;
    }

    public List<CommitteeSummary> listForBoard(AppUserPrincipal principal, UUID boardId) {
        requireBoardInOrganization(principal, boardId);
        return committeeRepository.findByBoardId(boardId).stream()
                .map(committee -> CommitteeSummary.from(committee, membersOf(committee.getId())))
                .toList();
    }

    public CommitteeSummary create(AppUserPrincipal principal, CreateCommitteeRequest request) {
        requireBoardInOrganization(principal, request.boardId());
        Committee committee = Committee.create(principal.getOrganizationId(), request.boardId(), request.name(),
                request.meetingFrequency(), request.mandate());
        committeeRepository.save(committee);
        return CommitteeSummary.from(committee, List.of());
    }

    public CommitteeSummary addMember(AppUserPrincipal principal, UUID committeeId, AddCommitteeMemberRequest request) {
        Committee committee = committeeRepository.findByIdAndOrganizationId(committeeId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Committee not found"));
        committeeMemberRepository.save(CommitteeMember.create(committeeId, request.directorId(), request.role()));
        return CommitteeSummary.from(committee, membersOf(committeeId));
    }

    private List<CommitteeMemberSummary> membersOf(UUID committeeId) {
        return committeeMemberRepository.findByCommitteeId(committeeId).stream()
                .map(CommitteeMemberSummary::from)
                .toList();
    }

    private void requireBoardInOrganization(AppUserPrincipal principal, UUID boardId) {
        boardRepository.findByIdAndOrganizationId(boardId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Board not found"));
    }
}
