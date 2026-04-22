package org.tkit.onecx.document.bff.services;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.tkit.onecx.document.bff.config.StorageIntegrationConfig;
import org.tkit.onecx.document.bff.mappers.DocumentMapper;
import org.tkit.onecx.document.bff.models.MetadataResult;
import org.tkit.onecx.document.bff.models.UploadUrlResult;

import gen.org.tkit.onecx.document.client.api.AttachmentControllerApi;
import gen.org.tkit.onecx.document.client.model.Attachment;
import gen.org.tkit.onecx.document.client.model.AttachmentStorageAuditRequest;
import gen.org.tkit.onecx.document.client.model.DocumentDetail;
import gen.org.tkit.onecx.document.rs.internal.model.UpdateFileMetadataRequestDTO;
import gen.org.tkit.onecx.document.rs.internal.model.UploadAttachmentPresignedUrlRequestDTO;
import gen.org.tkit.onecx.filestorage.client.api.FileStorageApi;
import gen.org.tkit.onecx.filestorage.client.model.*;
import io.quarkus.logging.Log;

@ApplicationScoped
public class AttachmentService {

    @Inject
    @RestClient
    FileStorageApi fileStorageApi;

    @Inject
    @RestClient
    AttachmentControllerApi attachmentClient;

    @Inject
    DocumentMapper documentMapper;

    @Inject
    StorageIntegrationConfig storageConfig;

    public List<UploadUrlResult> getUploadPresignedUrls(final DocumentDetail documentDetail,
            List<UploadAttachmentPresignedUrlRequestDTO> request) {
        var attachmentsToProcess = resolveAttachmentsToProcess(documentDetail, request);
        var results = new ArrayList<UploadUrlResult>();
        attachmentsToProcess
                .forEach(attachment -> results.add(processAttachment(documentDetail, attachment, request)));
        return results;
    }

    public PresignedUrlResponse getFilePresignedUrl(final String attachmentId) {
        final var attachment = getAttachment(attachmentId);
        final var downloadRequest = getPresignedUrlRequest(attachment);
        return getPresignedDownloadUrl(downloadRequest);
    }

    public Response updateAttachmentsMetadata(final DocumentDetail documentDetail,
            final List<UpdateFileMetadataRequestDTO> metadataRequests) {
        final var attachmentIds = metadataRequests.stream().map(UpdateFileMetadataRequestDTO::getAttachmentId)
                .collect(Collectors.toSet());
        final var attachmentNameIdMap = documentDetail.getAttachments().stream()
                .filter(att -> attachmentIds.contains(att.getId()))
                .collect(Collectors.toMap(att -> getUploadFileName(att.getId(), att.getFileName()), Attachment::getId));

        final var metadataStorageRequests = attachmentNameIdMap.keySet().stream().map(this::getMetadataRequest).toList();
        try (var storageResponse = fileStorageApi.getMetadataForFiles(metadataStorageRequests)) {
            final var metadataResults = processMetadataResult(storageResponse, attachmentNameIdMap);
            final var updateMetadataRequests = metadataResults.stream().map(documentMapper::mapToMetadataUpload).toList();
            return attachmentClient.uploadAttachmentsMetadata(updateMetadataRequests);
        }
    }

    public Response createAttachmentsAuditLogs(final String documentId,
            final List<UpdateFileMetadataRequestDTO> updateFileMetadataRequestDTO) {
        final var requests = updateFileMetadataRequestDTO.stream().map(req -> {
            final var auditDto = new AttachmentStorageAuditRequest();
            auditDto.setAttachmentId(req.getAttachmentId());
            auditDto.setDocumentId(documentId);
            return auditDto;
        }).toList();
        try (var response = attachmentClient.createStorageAuditsForAttachments(requests)) {
            return response;
        }
    }

    public void deleteDocumentAttachmentFiles(final List<Attachment> attachments) {
        var deleteRequests = attachments.stream().map(this::getFileDeleteRequest).toList();
        deleteRequests.forEach(request -> {
            try (var ignored = fileStorageApi.deleteFile(request)) {
                Log.info(String.format("Deleted file %s from storage", request.getFileName()));
            } catch (Exception ex) {
                throw new ClientWebApplicationException(ex);
            }
        });
    }

    private UploadUrlResult processAttachment(final DocumentDetail documentDetail, final Attachment attachment,
            final List<UploadAttachmentPresignedUrlRequestDTO> requestedAttachments) {
        final var matchedAttachmentOpt = requestedAttachments.stream()
                .filter(requestedAttachment -> attachment.getFileName().equals(requestedAttachment.getFileName()))
                .findFirst();
        if (matchedAttachmentOpt.isEmpty()) {
            return getFailedUploadResult(documentDetail.getId(), attachment.getId());
        }
        final var matchedAttachment = matchedAttachmentOpt.get();
        final var request = getPresignedUrlRequest(matchedAttachment);
        try {
            var urlBody = getPresignedUploadUrl(request);
            return getSuccessfulUploadResult(documentDetail.getId(), attachment.getId(), urlBody.getUrl(),
                    urlBody.getExpiration());
        } catch (Exception e) {
            return getFailedUploadResult(documentDetail.getId(), attachment.getId());
        }
    }

    private String getUploadFileName(final String attachmentId, final String fileName) {
        return String.format("%s%s%s", attachmentId, storageConfig.fileNameSeparator(), fileName);
    }

    private Set<Attachment> resolveAttachmentsToProcess(DocumentDetail document,
            List<UploadAttachmentPresignedUrlRequestDTO> requests) {
        Set<Attachment> attachmentSet = new HashSet<>();
        requests.forEach(uploadRequest -> document.getAttachments().stream()
                .filter(attachment -> uploadRequest.getAttachmentId().equals(attachment.getId()))
                .findFirst()
                .ifPresent(attachmentSet::add));
        return attachmentSet;
    }

    private UploadUrlResult getFailedUploadResult(final String documentId, final String attachmentId) {
        return UploadUrlResult.builder()
                .documentId(documentId)
                .attachmentId(attachmentId)
                .operationStatusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .build();
    }

    private UploadUrlResult getSuccessfulUploadResult(final String documentId, final String attachmentId,
            final String url, final OffsetDateTime expiring) {
        return UploadUrlResult.builder()
                .documentId(documentId)
                .attachmentId(attachmentId)
                .operationStatusCode(Response.Status.OK.getStatusCode())
                .url(url)
                .expiration(expiring).build();

    }

    private PresignedUrlRequest getPresignedUrlRequest(final UploadAttachmentPresignedUrlRequestDTO uploadRequest) {
        var fileName = getUploadFileName(uploadRequest.getAttachmentId(), uploadRequest.getFileName());
        return getPresignedUrlRequest(fileName);
    }

    private PresignedUrlRequest getPresignedUrlRequest(final Attachment attachment) {
        final var fileName = getUploadFileName(attachment.getId(), attachment.getFileName());
        return getPresignedUrlRequest(fileName);
    }

    private PresignedUrlRequest getPresignedUrlRequest(final String fileName) {
        final var request = new PresignedUrlRequest();
        request.setApplicationId(storageConfig.applicationId());
        request.setProductName(storageConfig.productName());
        request.setFileName(fileName);
        return request;
    }

    private Attachment getAttachment(final String attachmentId) {
        Response attResponse;
        try {
            attResponse = attachmentClient.getAttachmentDetails(attachmentId);
        } catch (ClientWebApplicationException e) {
            var exceptionResponse = e.getResponse();
            if (exceptionResponse.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                var msg = String.format("Attachment %s not found", attachmentId);
                throw new ClientWebApplicationException(msg);
            }
            var msg = String.format("Attachment %s could not be retrieved", attachmentId);
            throw new ClientWebApplicationException(msg);
        }
        return attResponse.readEntity(Attachment.class);
    }

    private PresignedUrlResponse getPresignedDownloadUrl(final PresignedUrlRequest downloadRequest) {
        try (var res = fileStorageApi.getPresignedDownloadUrl(downloadRequest)) {
            return handlePresignedUrlResponse(res);
        }
    }

    private PresignedUrlResponse getPresignedUploadUrl(final PresignedUrlRequest updateRequest) {
        Response response;
        try {
            response = fileStorageApi.getPresignedUploadUrl(updateRequest);
        } catch (ClientWebApplicationException e) {
            response = e.getResponse();
        }
        return handlePresignedUrlResponse(response);
    }

    private PresignedUrlResponse handlePresignedUrlResponse(final Response response) {
        return response.readEntity(PresignedUrlResponse.class);
    }

    private FileMetadataRequest getMetadataRequest(final String fileName) {
        final var request = new FileMetadataRequest();
        request.setFileName(fileName);
        request.setProductName(storageConfig.productName());
        request.setApplicationId(storageConfig.applicationId());
        return request;
    }

    private List<MetadataResult> processMetadataResult(final Response response, final Map<String, String> fileNameIdMap) {
        final var results = response.readEntity(new GenericType<List<FileMetadataResponse>>() {
        });
        return results.stream().map(result -> {
            final var receivedFileName = result.getFileName();
            final var attId = fileNameIdMap.get(receivedFileName);
            return new MetadataResult(attId, result);
        }).toList();
    }

    private FileDeleteRequest getFileDeleteRequest(final Attachment attachment) {
        var finalFileName = getUploadFileName(attachment.getId(), attachment.getFileName());
        var deleteRequest = new FileDeleteRequest();
        deleteRequest.setFileName(finalFileName);
        deleteRequest.setApplicationId(storageConfig.applicationId());
        deleteRequest.setProductName(storageConfig.productName());
        return deleteRequest;
    }
}
