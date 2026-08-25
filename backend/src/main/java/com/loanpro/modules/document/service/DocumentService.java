package com.loanpro.modules.document.service;

import com.loanpro.common.exception.BusinessException;
import com.loanpro.common.exception.ForbiddenException;
import com.loanpro.common.exception.ResourceNotFoundException;
import com.loanpro.infrastructure.storage.StorageService;
import com.loanpro.modules.application.domain.ApplicationStatus;
import com.loanpro.modules.application.domain.LoanApplication;
import com.loanpro.modules.application.service.LoanApplicationService;
import com.loanpro.modules.audit.service.AuditService;
import com.loanpro.modules.document.domain.DocumentType;
import com.loanpro.modules.document.domain.DocumentVerificationStatus;
import com.loanpro.modules.document.domain.LoanDocument;
import com.loanpro.modules.document.dto.DocumentResponse;
import com.loanpro.modules.document.repository.LoanDocumentRepository;
import com.loanpro.modules.identity.domain.RoleName;
import com.loanpro.modules.identity.domain.User;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentService {

    private static final long MAX_BYTES = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            MediaType.APPLICATION_PDF_VALUE,
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/webp"
    );

    private final LoanDocumentRepository documentRepository;
    private final LoanApplicationService applicationService;
    private final StorageService storageService;
    private final AuditService auditService;

    public DocumentService(
            LoanDocumentRepository documentRepository,
            LoanApplicationService applicationService,
            StorageService storageService,
            AuditService auditService
    ) {
        this.documentRepository = documentRepository;
        this.applicationService = applicationService;
        this.storageService = storageService;
        this.auditService = auditService;
    }

    @Transactional
    public DocumentResponse upload(UUID applicationId, DocumentType type, MultipartFile file, User actor) {
        LoanApplication application = applicationService.requireVisibleTo(applicationId, actor);
        if (actor.hasRole(RoleName.CUSTOMER) && !application.getCustomer().getId().equals(actor.getId())) {
            throw new ForbiddenException("You can only upload documents for your own applications");
        }
        if (application.getStatus() != ApplicationStatus.DRAFT
                && application.getStatus() != ApplicationStatus.INFO_REQUESTED) {
            if (actor.hasRole(RoleName.CUSTOMER)) {
                throw new BusinessException("Documents can only be uploaded while the application is editable");
            }
        }
        validateFile(file);
        String key = "applications/%s/%s-%s".formatted(
                application.getId(),
                UUID.randomUUID(),
                sanitize(file.getOriginalFilename())
        );
        try {
            storageService.store(key, file.getInputStream(), file.getSize(), file.getContentType());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store document", e);
        }
        LoanDocument document = new LoanDocument();
        document.setApplication(application);
        document.setDocumentType(type);
        document.setOriginalFileName(file.getOriginalFilename());
        document.setContentType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setStorageKey(key);
        document.setUploadedBy(actor);
        document.setVerificationStatus(DocumentVerificationStatus.PENDING);
        documentRepository.save(document);
        auditService.record(actor, "DOCUMENT_UPLOADED", "LoanDocument", document.getId(),
                application, application.getStatus(), application.getStatus(), type.name());
        return DocumentResponse.from(document);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> list(UUID applicationId, User actor) {
        applicationService.requireVisibleTo(applicationId, actor);
        return documentRepository.findByApplicationIdOrderByCreatedAtAsc(applicationId)
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StoredFile download(UUID documentId, User actor) {
        LoanDocument document = require(documentId);
        applicationService.requireVisibleTo(document.getApplication().getId(), actor);
        return new StoredFile(document.getOriginalFileName(), document.getContentType(), storageService.load(document.getStorageKey()));
    }

    @Transactional
    public void delete(UUID documentId, User actor) {
        LoanDocument document = require(documentId);
        LoanApplication application = applicationService.require(document.getApplication().getId());
        if (!application.getCustomer().getId().equals(actor.getId())) {
            throw new ForbiddenException("Only the applicant can delete documents");
        }
        if (application.getStatus() != ApplicationStatus.DRAFT && application.getStatus() != ApplicationStatus.INFO_REQUESTED) {
            throw new BusinessException("Documents cannot be deleted after submission");
        }
        storageService.delete(document.getStorageKey());
        documentRepository.delete(document);
        auditService.record(actor, "DOCUMENT_DELETED", "LoanDocument", documentId,
                application, application.getStatus(), application.getStatus(), document.getDocumentType().name());
    }

    @Transactional
    public DocumentResponse verify(UUID documentId, boolean verified, String remarks, User maker) {
        LoanDocument document = require(documentId);
        document.setVerificationStatus(verified ? DocumentVerificationStatus.VERIFIED : DocumentVerificationStatus.REJECTED);
        document.setVerifiedBy(maker);
        document.setVerifiedAt(Instant.now());
        document.setVerificationRemarks(remarks);
        auditService.record(maker, "DOCUMENT_VERIFIED", "LoanDocument", document.getId(),
                document.getApplication(), document.getApplication().getStatus(),
                document.getApplication().getStatus(), remarks);
        return DocumentResponse.from(document);
    }

    public List<LoanDocument> entities(UUID applicationId) {
        return documentRepository.findByApplicationIdOrderByCreatedAtAsc(applicationId);
    }

    private LoanDocument require(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File is required");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessException("File exceeds the 10MB size limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessException("Only PDF, JPEG, PNG and WEBP files are allowed");
        }
    }

    private String sanitize(String name) {
        if (name == null || name.isBlank()) {
            return "document";
        }
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public record StoredFile(String fileName, String contentType, byte[] bytes) {
    }
}
