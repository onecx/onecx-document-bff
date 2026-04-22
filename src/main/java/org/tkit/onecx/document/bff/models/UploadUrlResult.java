package org.tkit.onecx.document.bff.models;

import java.time.OffsetDateTime;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;

@RegisterForReflection
@Builder
public record UploadUrlResult(
        String documentId,
        String attachmentId,
        Integer operationStatusCode,
        String url,
        OffsetDateTime expiration) {
}
