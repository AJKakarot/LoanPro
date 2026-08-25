package com.loanpro.modules.document.dto;

import com.loanpro.modules.document.domain.DocumentType;
import com.loanpro.modules.document.domain.DocumentVerificationStatus;
import com.loanpro.modules.document.domain.LoanDocument;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        DocumentType documentType,
        String originalFileName,
        String contentType,
        long fileSize,
        DocumentVerificationStatus verificationStatus,
        String verificationRemarks,
        String uploadedBy,
        Instant createdAt
) {
    public static DocumentResponse from(LoanDocument document) {
        return new DocumentResponse(
                document.getId(),
                document.getDocumentType(),
                document.getOriginalFileName(),
                document.getContentType(),
                document.getFileSize(),
                document.getVerificationStatus(),
                document.getVerificationRemarks(),
                document.getUploadedBy().getFullName(),
                document.getCreatedAt()
        );
    }
}
