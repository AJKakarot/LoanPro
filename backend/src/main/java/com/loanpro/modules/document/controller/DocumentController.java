package com.loanpro.modules.document.controller;

import com.loanpro.common.api.ApiResponse;
import com.loanpro.modules.document.domain.DocumentType;
import com.loanpro.modules.document.dto.DocumentResponse;
import com.loanpro.modules.document.service.DocumentService;
import com.loanpro.security.CurrentUserService;
import com.loanpro.security.UserPrincipal;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class DocumentController {

    private final DocumentService documentService;
    private final CurrentUserService currentUserService;

    public DocumentController(DocumentService documentService, CurrentUserService currentUserService) {
        this.documentService = documentService;
        this.currentUserService = currentUserService;
    }

    @PostMapping(value = "/applications/{applicationId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DocumentResponse> upload(
            @PathVariable UUID applicationId,
            @RequestParam DocumentType documentType,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok("Document uploaded",
                documentService.upload(applicationId, documentType, file, currentUserService.require(principal)));
    }

    @GetMapping("/applications/{applicationId}/documents")
    @PreAuthorize("hasAnyRole('CUSTOMER','MAKER','CHECKER','ADMIN')")
    public ApiResponse<List<DocumentResponse>> list(
            @PathVariable UUID applicationId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.ok(documentService.list(applicationId, currentUserService.require(principal)));
    }

    @GetMapping("/documents/{id}/download")
    @PreAuthorize("hasAnyRole('CUSTOMER','MAKER','CHECKER','ADMIN')")
    public ResponseEntity<byte[]> download(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        var stored = documentService.download(id, currentUserService.require(principal));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(stored.contentType()));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(stored.fileName(), StandardCharsets.UTF_8)
                .build());
        return new ResponseEntity<>(stored.bytes(), headers, HttpStatus.OK);
    }

    @DeleteMapping("/documents/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        documentService.delete(id, currentUserService.require(principal));
        return ApiResponse.ok("Document deleted", null);
    }
}
