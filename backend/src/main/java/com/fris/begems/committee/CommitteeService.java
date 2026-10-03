package com.fris.begems.committee;

import com.fris.begems.board.BoardRepository;
import com.fris.begems.committee.dto.AddCommitteeMemberRequest;
import com.fris.begems.committee.dto.CommitteeMemberSummary;
import com.fris.begems.committee.dto.CommitteeSummary;
import com.fris.begems.committee.dto.CreateCommitteeRequest;
import com.fris.begems.committee.dto.DirectorCommitteeMembership;
import com.fris.begems.common.ApiException;
import com.fris.begems.director.Director;
import com.fris.begems.director.DirectorRepository;
import com.fris.begems.security.AppUserPrincipal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommitteeService {

    private final CommitteeRepository committeeRepository;
    private final CommitteeMemberRepository committeeMemberRepository;
    private final BoardRepository boardRepository;
    private final DirectorRepository directorRepository;

    public CommitteeService(CommitteeRepository committeeRepository,
            CommitteeMemberRepository committeeMemberRepository, BoardRepository boardRepository,
            DirectorRepository directorRepository) {
        this.committeeRepository = committeeRepository;
        this.committeeMemberRepository = committeeMemberRepository;
        this.boardRepository = boardRepository;
        this.directorRepository = directorRepository;
    }

    public List<CommitteeSummary> listForBoard(AppUserPrincipal principal, UUID boardId) {
        requireBoardInOrganization(principal, boardId);
        List<Committee> committees = committeeRepository.findByBoardId(boardId);
        Map<UUID, List<CommitteeMemberSummary>> membersByCommittee = committeeMemberRepository
                .findByCommitteeIdIn(committees.stream().map(Committee::getId).toList()).stream()
                .collect(Collectors.groupingBy(CommitteeMember::getCommitteeId,
                        Collectors.mapping(CommitteeMemberSummary::from, Collectors.toList())));
        return committees.stream()
                .map(committee -> CommitteeSummary.from(committee,
                        membersByCommittee.getOrDefault(committee.getId(), List.of())))
                .toList();
    }

    @Transactional
    public CommitteeSummary create(AppUserPrincipal principal, CreateCommitteeRequest request) {
        boardRepository.lockByIdAndOrganizationId(request.boardId(), principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Board not found"));
        String name = request.name().trim();
        if (committeeRepository.existsByBoardIdAndNameIgnoreCase(request.boardId(), name)) {
            throw ApiException.conflict("A committee with this name already exists");
        }
        Committee committee = Committee.create(principal.getOrganizationId(), request.boardId(), name,
                request.meetingFrequency(), request.mandate());
        committeeRepository.save(committee);
        return CommitteeSummary.from(committee, List.of());
    }

    /**
     * Adds the director to the committee or changes their role if they're already on it.
     * A committee has at most one chair, so naming a new chair moves the previous one to member.
     */
    @Transactional
    public CommitteeSummary assignMember(AppUserPrincipal principal, UUID committeeId,
            AddCommitteeMemberRequest request) {
        Committee committee = requireCommittee(principal, committeeId);
        Director director = directorRepository.findByIdAndOrganizationId(request.directorId(),
                principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Director not found"));
        if (!director.getBoardId().equals(committee.getBoardId())) {
            throw ApiException.badRequest("This director is not on the committee's board");
        }

        if (request.role() == CommitteeMemberRole.CHAIR) {
            committeeMemberRepository.findByCommitteeIdAndRole(committeeId, CommitteeMemberRole.CHAIR)
                    .filter(chair -> !chair.getDirectorId().equals(director.getId()))
                    .ifPresent(previousChair -> {
                        previousChair.setRole(CommitteeMemberRole.MEMBER);
                        committeeMemberRepository.saveAndFlush(previousChair);
                    });
        }

        CommitteeMember membership = committeeMemberRepository
                .findByCommitteeIdAndDirectorId(committeeId, director.getId())
                .orElseGet(() -> CommitteeMember.create(committeeId, director.getId(), request.role()));
        membership.setRole(request.role());
        committeeMemberRepository.save(membership);

        return CommitteeSummary.from(committee, membersOf(committeeId));
    }

    @Transactional
    public CommitteeSummary removeMember(AppUserPrincipal principal, UUID committeeId, UUID directorId) {
        Committee committee = requireCommittee(principal, committeeId);
        CommitteeMember membership = committeeMemberRepository.findByCommitteeIdAndDirectorId(committeeId, directorId)
                .orElseThrow(() -> ApiException.notFound("This director is not on the committee"));
        committeeMemberRepository.delete(membership);
        committeeMemberRepository.flush();
        return CommitteeSummary.from(committee, membersOf(committeeId));
    }

    public List<DirectorCommitteeMembership> membershipsForDirector(UUID directorId) {
        List<CommitteeMember> memberships = committeeMemberRepository.findByDirectorId(directorId);
        Map<UUID, Committee> committeesById = committeeRepository
                .findAllById(memberships.stream().map(CommitteeMember::getCommitteeId).toList()).stream()
                .collect(Collectors.toMap(Committee::getId, Function.identity()));
        return memberships.stream()
                .filter(m -> committeesById.containsKey(m.getCommitteeId()))
                .map(m -> new DirectorCommitteeMembership(m.getCommitteeId(),
                        committeesById.get(m.getCommitteeId()).getName(), m.getRole()))
                .sorted(Comparator.comparing(DirectorCommitteeMembership::committeeName))
                .toList();
    }

    private Committee requireCommittee(AppUserPrincipal principal, UUID committeeId) {
        return committeeRepository.findByIdAndOrganizationId(committeeId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Committee not found"));
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
