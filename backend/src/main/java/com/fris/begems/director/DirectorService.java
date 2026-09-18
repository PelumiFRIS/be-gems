package com.fris.begems.director;

import com.fris.begems.audit.AuditAction;
import com.fris.begems.audit.AuditEntityType;
import com.fris.begems.audit.AuditLogService;
import com.fris.begems.board.BoardRepository;
import com.fris.begems.common.ApiException;
import com.fris.begems.director.dto.CreateDirectorRequest;
import com.fris.begems.director.dto.DirectorSummary;
import com.fris.begems.director.dto.InviteDirectorResponse;
import com.fris.begems.security.AppUserPrincipal;
import com.fris.begems.user.Role;
import com.fris.begems.user.User;
import com.fris.begems.user.UserRepository;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DirectorService {

    private static final int TEMPORARY_PASSWORD_BYTES = 18;

    private final DirectorRepository directorRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final SecureRandom secureRandom = new SecureRandom();

    public DirectorService(DirectorRepository directorRepository, BoardRepository boardRepository,
            UserRepository userRepository, PasswordEncoder passwordEncoder, AuditLogService auditLogService) {
        this.directorRepository = directorRepository;
        this.boardRepository = boardRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
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

    @Transactional
    public InviteDirectorResponse invite(AppUserPrincipal principal, UUID directorId) {
        Director director = directorRepository.findByIdAndOrganizationId(directorId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Director not found"));
        if (director.getUserId() != null) {
            throw ApiException.conflict("This director already has portal access");
        }
        if (director.getEmail() == null || director.getEmail().isBlank()) {
            throw ApiException.badRequest("This director needs an email address before they can be invited");
        }
        if (userRepository.existsByEmailIgnoreCase(director.getEmail())) {
            throw ApiException.conflict("An account with this email already exists");
        }

        String temporaryPassword = generateTemporaryPassword();
        String[] nameParts = splitName(director.getName());
        User user = User.create(principal.getOrganizationId(), director.getEmail(),
                passwordEncoder.encode(temporaryPassword), nameParts[0], nameParts[1], Role.DIRECTOR);
        userRepository.save(user);

        director.setUserId(user.getId());
        directorRepository.save(director);

        auditLogService.record(principal, AuditAction.DIRECTOR_INVITED, AuditEntityType.DIRECTOR, director.getId(),
                "Invited \"" + director.getName() + "\" to the portal");

        return new InviteDirectorResponse(director.getEmail(), temporaryPassword);
    }

    private String[] splitName(String fullName) {
        String trimmed = fullName.trim();
        int spaceIndex = trimmed.indexOf(' ');
        if (spaceIndex < 0) {
            return new String[] {trimmed, ""};
        }
        return new String[] {trimmed.substring(0, spaceIndex), trimmed.substring(spaceIndex + 1).trim()};
    }

    private String generateTemporaryPassword() {
        byte[] bytes = new byte[TEMPORARY_PASSWORD_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void requireBoardInOrganization(AppUserPrincipal principal, UUID boardId) {
        boardRepository.findByIdAndOrganizationId(boardId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Board not found"));
    }
}
