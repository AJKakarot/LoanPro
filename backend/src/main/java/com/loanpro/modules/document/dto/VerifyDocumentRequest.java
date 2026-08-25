package com.loanpro.modules.document.dto;

import jakarta.validation.constraints.Size;

public record VerifyDocumentRequest(
        boolean verified,
        @Size(max = 500) String remarks
) {
}
