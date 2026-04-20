package org.tkit.onecx.document.bff.models;

import gen.org.tkit.onecx.filestorage.client.model.FileMetadataResponse;

public record MetadataResult(String attachmentId, FileMetadataResponse response) {
}