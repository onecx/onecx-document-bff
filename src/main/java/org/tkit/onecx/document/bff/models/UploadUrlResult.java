package org.tkit.onecx.document.bff.models;

import java.time.OffsetDateTime;

import lombok.Builder;

@Builder
public record UploadUrlResult(
        String documentId,
        String attachmentId,
        Integer operationStatusCode,
        String url,
        OffsetDateTime expiration) {
}
