package org.tkit.onecx.document.bff.controllers;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.tkit.onecx.document.bff.mappers.DocumentMapper;
import org.tkit.onecx.document.bff.mappers.ExceptionMapper;
import org.tkit.onecx.document.bff.services.AttachmentService;

import gen.org.tkit.onecx.document.client.api.DocumentControllerApi;
import gen.org.tkit.onecx.document.client.model.DocumentDetail;
import gen.org.tkit.onecx.document.rs.internal.DocumentControllerApiService;
import gen.org.tkit.onecx.document.rs.internal.model.*;

@ApplicationScoped
public class DocumentController implements DocumentControllerApiService {

    @Inject
    @RestClient
    DocumentControllerApi documentControllerApi;

    @Inject
    DocumentMapper mapper;

    @Inject
    AttachmentService fileService;

    @Inject
    ExceptionMapper exceptionMapper;

    @Override
    public Response bulkUpdateDocument(List<DocumentCreateUpdateDTO> documentCreateUpdateDTOS) {
        try (Response response = documentControllerApi.bulkUpdateDocument(mapper.map(documentCreateUpdateDTOS))) {
            return Response.status(response.getStatus())
                    .entity(mapper.map(response.readEntity(DocumentDetailDTO.class)))
                    .build();
        }
    }

    @Override
    public Response createDocument(DocumentCreateUpdateDTO documentCreateUpdateDTO) {
        try (Response response = documentControllerApi.createDocument(mapper.map(documentCreateUpdateDTO))) {
            return Response.status(response.getStatus())
                    .entity(mapper.map(response.readEntity(DocumentDetailDTO.class)))
                    .build();
        }
    }

    @Override
    public Response deleteBulkDocuments(List<String> requestBody) {
        try (Response response = documentControllerApi.deleteBulkDocuments(requestBody)) {
            return Response.status(response.getStatus()).build();
        }
    }

    @Override
    public Response deleteDocumentById(String id) {
        var docDetail = documentControllerApi.getDocumentById(id).readEntity(DocumentDetail.class);
        var attachments = docDetail.getAttachments();
        try (Response response = documentControllerApi.deleteDocumentById(id)) {
            fileService.deleteDocumentAttachmentFiles(attachments);
            return Response.status(response.getStatus()).build();
        }
    }

    @Override
    public Response getAllChannels() {
        try (Response response = documentControllerApi.getAllChannels()) {
            return Response.status(response.getStatus())
                    .entity(mapper.mapChannel(response.readEntity(new GenericType<List<ChannelDTO>>() {
                    })))
                    .build();
        }
    }

    @Override
    public Response getDocumentByCriteria(DocumentSearchCriteriaDTO criteriaDTO) {
        var internalCriteria = mapper.mapToInternalCriteria(criteriaDTO);
        try (Response response = documentControllerApi.getDocumentByCriteria(internalCriteria)) {
            return Response.status(response.getStatus())
                    .entity(mapper.map(response.readEntity(DocumentPageResultDTO.class)))
                    .build();
        }
    }

    @Override
    public Response getDocumentById(String id) {
        try (Response response = documentControllerApi.getDocumentById(id)) {
            DocumentDetailDTO detailDTO = response.readEntity(DocumentDetailDTO.class);
            return Response.status(response.getStatus())
                    .entity(mapper.map(detailDTO))
                    .build();
        }
    }

    @Override
    public Response getFailedAttachmentData(String id) {
        try (Response response = documentControllerApi.getFailedAttachmentData(id)) {
            return Response.status(response.getStatus())
                    .entity(mapper.mapAuditList(response
                            .readEntity(new GenericType<List<StorageUploadAuditDTO>>() {
                            })))
                    .build();
        }
    }

    @Override
    public Response getFile(String attachmentId) {
        var presignedUrl = fileService.getFilePresignedUrl(attachmentId);
        return Response
                .ok(mapper.mapPresignedUrl(presignedUrl))
                .build();
    }

    @Override
    public Response showAllDocumentsByCriteria(DocumentSearchCriteriaDTO criteriaDTO) {
        var internalCriteria = mapper.mapToInternalCriteria(criteriaDTO);
        try (Response response = documentControllerApi.showAllDocumentsByCriteria(internalCriteria)) {
            return Response.status(response.getStatus())
                    .entity(mapper.mapDetailList(response.readEntity(new GenericType<List<DocumentDetailDTO>>() {
                    })))
                    .build();
        }
    }

    @Override
    public Response updateDocument(String id, DocumentCreateUpdateDTO documentCreateUpdateDTO) {
        try (Response response = documentControllerApi.updateDocument(id, mapper.map(documentCreateUpdateDTO))) {
            return Response.status(response.getStatus())
                    .entity(mapper.map(response.readEntity(DocumentDetailDTO.class)))
                    .build();
        }
    }

    @Override
    public Response uploadAllFiles(String documentId, List<UploadAttachmentPresignedUrlRequestDTO> request) {
        final var documentDetail = documentControllerApi.getDocumentById(documentId)
                .readEntity(DocumentDetail.class);
        final var uploadResults = fileService.getUploadPresignedUrls(documentDetail, request);
        return Response.ok()
                .entity(mapper.mapUploadResponse(uploadResults))
                .build();
    }

    @Override
    public Response updateAttachmentsMetadata(String documentId,
            List<UpdateFileMetadataRequestDTO> updateFileMetadataRequestDTO) {
        final var documentDetail = documentControllerApi.getDocumentById(documentId)
                .readEntity(DocumentDetail.class);
        return fileService.updateAttachmentsMetadata(documentDetail, updateFileMetadataRequestDTO);
    }

    @Override
    public Response createFailedAttachmentsAuditLogs(String documentId,
            List<UpdateFileMetadataRequestDTO> updateFileMetadataRequestDTO) {
        try (Response res = fileService.createAttachmentsAuditLogs(documentId, updateFileMetadataRequestDTO)) {
            return Response.status(res.getStatus()).build();
        }
    }

    @ServerExceptionMapper
    public Response restException(ClientWebApplicationException ex) {
        return exceptionMapper.clientException(ex);
    }
}
