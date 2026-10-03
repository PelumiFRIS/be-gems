package com.fris.begems.director;

import com.fris.begems.director.dto.CreateDirectorRequest;
import com.fris.begems.director.dto.DirectorCvSummary;
import com.fris.begems.director.dto.DirectorDetail;
import com.fris.begems.director.dto.DirectorSummary;
import com.fris.begems.director.dto.InviteDirectorResponse;
import com.fris.begems.director.dto.UpdateDirectorRequest;
import com.fris.begems.security.AppUserPrincipal;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/directors")
public class DirectorController {

    private final DirectorService directorService;

    public DirectorController(DirectorService directorService) {
        this.directorService = directorService;
    }

    @GetMapping
    public List<DirectorSummary> list(@AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam UUID boardId) {
        return directorService.listForBoard(principal, boardId);
    }

    @GetMapping("/{id}")
    public DirectorDetail get(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        return directorService.get(principal, id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'COMPANY_SECRETARY')")
    public ResponseEntity<DirectorSummary> create(@AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateDirectorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(directorService.create(principal, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'COMPANY_SECRETARY')")
    public DirectorDetail update(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id,
            @Valid @RequestBody UpdateDirectorRequest request) {
        return directorService.update(principal, id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'COMPANY_SECRETARY')")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        directorService.delete(principal, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cv")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'COMPANY_SECRETARY')")
    public DirectorCvSummary uploadCv(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        return directorService.uploadCv(principal, id, file);
    }

    @GetMapping("/{id}/cv")
    public ResponseEntity<byte[]> downloadCv(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id) {
        DirectorCv cv = directorService.downloadCv(principal, id);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(cv.getFileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(cv.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(cv.getFileData());
    }

    @DeleteMapping("/{id}/cv")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'COMPANY_SECRETARY')")
    public ResponseEntity<Void> deleteCv(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        directorService.deleteCv(principal, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/invite")
    @PreAuthorize("hasAnyRole('COMPANY_SECRETARY', 'EVALUATOR')")
    public InviteDirectorResponse invite(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        return directorService.invite(principal, id);
    }
}
