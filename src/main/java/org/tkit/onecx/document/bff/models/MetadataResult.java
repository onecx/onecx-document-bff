package org.tkit.onecx.document.bff.models;

import gen.org.tkit.onecx.filestorage.client.model.FileMetadataResponse;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record MetadataResult(String attachmentId, FileMetadataResponse response) {
}