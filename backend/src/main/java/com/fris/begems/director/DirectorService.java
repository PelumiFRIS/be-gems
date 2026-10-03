package com.fris.begems.director;

import com.fris.begems.audit.AuditAction;
import com.fris.begems.audit.AuditEntityType;
import com.fris.begems.audit.AuditLogService;
import com.fris.begems.board.BoardRepository;
import com.fris.begems.committee.CommitteeMemberRepository;
import com.fris.begems.committee.CommitteeService;
import com.fris.begems.common.ApiException;
import com.fris.begems.director.dto.CreateDirectorRequest;
import com.fris.begems.director.dto.DirectorCvSummary;
import com.fris.begems.director.dto.DirectorDetail;
import com.fris.begems.director.dto.DirectorSummary;
import com.fris.begems.director.dto.InviteDirectorResponse;
import com.fris.begems.director.dto.UpdateDirectorRequest;
import com.fris.begems.evaluation.EvaluationRepository;
import com.fris.begems.evaluation.EvaluationRespondentRepository;
import com.fris.begems.security.AppUserPrincipal;
import com.fris.begems.user.Role;
import com.fris.begems.user.User;
import com.fris.begems.user.UserRepository;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DirectorService {

    private static final int TEMPORARY_PASSWORD_BYTES = 18;
    private static final long MAX_CV_SIZE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> CV_EXTENSIONS = Set.of("pdf", "doc", "docx");
    private static final Set<Role> BIODATA_VIEWER_ROLES = Set.of(Role.ORG_ADMIN, Role.COMPANY_SECRETARY,
            Role.EVALUATOR);

    private final DirectorRepository directorRepository;
    private final DirectorCvRepository directorCvRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final CommitteeService committeeService;
    private final CommitteeMemberRepository committeeMemberRepository;
    private final EvaluationRepository evaluationRepository;
    private final EvaluationRespondentRepository evaluationRespondentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final SecureRandom secureRandom = new SecureRandom();

    public DirectorService(DirectorRepository directorRepository, DirectorCvRepository directorCvRepository,
            BoardRepository boardRepository, UserRepository userRepository, CommitteeService committeeService,
            CommitteeMemberRepository committeeMemberRepository, EvaluationRepository evaluationRepository,
            EvaluationRespondentRepository evaluationRespondentRepository, PasswordEncoder passwordEncoder,
            AuditLogService auditLogService) {
        this.directorRepository = directorRepository;
        this.directorCvRepository = directorCvRepository;
        this.boardRepository = boardRepository;
        this.userRepository = userRepository;
        this.committeeService = committeeService;
        this.committeeMemberRepository = committeeMemberRepository;
        this.evaluationRepository = evaluationRepository;
        this.evaluationRespondentRepository = evaluationRespondentRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    public List<DirectorSummary> listForBoard(AppUserPrincipal principal, UUID boardId) {
        requireBoardInOrganization(principal, boardId);
        return directorRepository.findByBoardId(boardId).stream()
                .map(DirectorSummary::from)
                .toList();
    }

    public DirectorDetail get(AppUserPrincipal principal, UUID directorId) {
        Director director = requireDirectorWithBiodataAccess(principal, directorId);
        return toDetail(director);
    }

    @Transactional
    public DirectorSummary create(AppUserPrincipal principal, CreateDirectorRequest request) {
        boardRepository.lockByIdAndOrganizationId(request.boardId(), principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Board not found"));
        if (directorRepository.existsByBoardIdAndEmailIgnoreCase(request.boardId(), request.email().trim())) {
            throw ApiException.conflict("A director with this email is already on the board");
        }

        Director director = Director.create(principal.getOrganizationId(), request.boardId(), request);
        directorRepository.save(director);

        auditLogService.record(principal, AuditAction.DIRECTOR_CREATED, AuditEntityType.DIRECTOR, director.getId(),
                "Added \"" + director.getName() + "\" to the board");
        return DirectorSummary.from(director);
    }

    @Transactional
    public DirectorDetail update(AppUserPrincipal principal, UUID directorId, UpdateDirectorRequest request) {
        Director director = requireDirector(principal, directorId);
        boardRepository.lockByIdAndOrganizationId(director.getBoardId(), principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Board not found"));

        String newEmail = request.email().trim();
        if (directorRepository.existsByBoardIdAndEmailIgnoreCaseAndIdNot(director.getBoardId(), newEmail,
                director.getId())) {
            throw ApiException.conflict("A director with this email is already on the board");
        }

        if (director.getUserId() != null) {
            syncPortalAccount(director.getUserId(), request.name().trim(), newEmail);
        }

        director.applyProfile(request);
        directorRepository.save(director);

        auditLogService.record(principal, AuditAction.DIRECTOR_UPDATED, AuditEntityType.DIRECTOR, director.getId(),
                "Updated \"" + director.getName() + "\"");
        return toDetail(director);
    }

    /**
     * Directors who have taken part in (or been the subject of) an evaluation are kept so
     * scores and responses stay attributable; anyone else — e.g. a duplicate entry — can go.
     */
    @Transactional
    public void delete(AppUserPrincipal principal, UUID directorId) {
        Director director = requireDirector(principal, directorId);
        if (evaluationRespondentRepository.existsByDirectorId(directorId)
                || evaluationRepository.existsBySubjectDirectorId(directorId)) {
            throw ApiException.conflict(
                    "This director is part of an evaluation, so they can't be removed. Edit their details instead.");
        }

        committeeMemberRepository.deleteByDirectorId(directorId);
        directorCvRepository.findById(directorId).ifPresent(directorCvRepository::delete);

        UUID userId = director.getUserId();
        directorRepository.delete(director);
        directorRepository.flush();
        if (userId != null) {
            userRepository.deleteById(userId);
        }

        auditLogService.record(principal, AuditAction.DIRECTOR_REMOVED, AuditEntityType.DIRECTOR, directorId,
                "Removed \"" + director.getName() + "\" from the board");
    }

    @Transactional
    public DirectorCvSummary uploadCv(AppUserPrincipal principal, UUID directorId, MultipartFile file) {
        Director director = requireDirector(principal, directorId);

        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Choose a file to upload");
        }
        if (file.getSize() > MAX_CV_SIZE_BYTES) {
            throw ApiException.badRequest("CV files must be 10MB or smaller");
        }
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "cv";
        if (!CV_EXTENSIONS.contains(extensionOf(fileName))) {
            throw ApiException.badRequest("CVs must be a PDF or Word document");
        }

        byte[] fileData;
        try {
            fileData = file.getBytes();
        } catch (IOException e) {
            throw ApiException.badRequest("Could not read the uploaded file");
        }
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

        DirectorCv cv = directorCvRepository.findById(directorId).orElseGet(() -> {
            DirectorCv created = new DirectorCv();
            created.setDirectorId(directorId);
            created.setOrganizationId(principal.getOrganizationId());
            return created;
        });
        cv.replaceFile(fileName, contentType, fileData, principal.getUserId());
        directorCvRepository.save(cv);

        auditLogService.record(principal, AuditAction.DIRECTOR_CV_UPLOADED, AuditEntityType.DIRECTOR,
                director.getId(), "Uploaded CV for \"" + director.getName() + "\"");
        return new DirectorCvSummary(cv.getFileName(), cv.getContentType(), cv.getFileSize(), cv.getUploadedAt());
    }

    public DirectorCv downloadCv(AppUserPrincipal principal, UUID directorId) {
        requireDirectorWithBiodataAccess(principal, directorId);
        return directorCvRepository.findById(directorId)
                .orElseThrow(() -> ApiException.notFound("No CV has been uploaded for this director"));
    }

    @Transactional
    public void deleteCv(AppUserPrincipal principal, UUID directorId) {
        requireDirector(principal, directorId);
        DirectorCv cv = directorCvRepository.findById(directorId)
                .orElseThrow(() -> ApiException.notFound("No CV has been uploaded for this director"));
        directorCvRepository.delete(cv);
    }

    @Transactional
    public InviteDirectorResponse invite(AppUserPrincipal principal, UUID directorId) {
        Director director = requireDirector(principal, directorId);
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

    /** The director's portal login is their email, so keep the account in step with the biodata. */
    private void syncPortalAccount(UUID userId, String name, String email) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }
        if (!user.getEmail().equalsIgnoreCase(email) && userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Another account already uses this email");
        }
        String[] nameParts = splitName(name);
        user.setEmail(email);
        user.setFirstName(nameParts[0]);
        user.setLastName(nameParts[1]);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    private DirectorDetail toDetail(Director director) {
        DirectorCvSummary cv = directorCvRepository.findSummaryByDirectorId(director.getId()).orElse(null);
        return DirectorDetail.from(director, cv, committeeService.membershipsForDirector(director.getId()));
    }

    private Director requireDirector(AppUserPrincipal principal, UUID directorId) {
        return directorRepository.findByIdAndOrganizationId(directorId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("Director not found"));
    }

    private Director requireDirectorWithBiodataAccess(AppUserPrincipal principal, UUID directorId) {
        Director director = requireDirector(principal, directorId);
        boolean isSelf = principal.getUserId().equals(director.getUserId());
        if (!isSelf && !BIODATA_VIEWER_ROLES.contains(principal.getRole())) {
            throw ApiException.forbidden("You do not have permission to view this director's details");
        }
        return director;
    }

    private static String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
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
