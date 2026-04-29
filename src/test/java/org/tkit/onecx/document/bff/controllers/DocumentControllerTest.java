package org.tkit.onecx.document.bff.controllers;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.time.OffsetDateTime;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.JsonBody;
import org.mockserver.model.MediaType;
import org.tkit.onecx.document.bff.AbstractTest;

import gen.org.tkit.onecx.document.client.model.Attachment;
import gen.org.tkit.onecx.document.client.model.DocumentDetail;
import gen.org.tkit.onecx.document.rs.internal.model.AttachmentPresignedUrlResponseDTO;
import gen.org.tkit.onecx.document.rs.internal.model.ChannelCreateUpdateDTO;
import gen.org.tkit.onecx.document.rs.internal.model.ChannelDTO;
import gen.org.tkit.onecx.document.rs.internal.model.DocumentCreateUpdateDTO;
import gen.org.tkit.onecx.document.rs.internal.model.DocumentDetailDTO;
import gen.org.tkit.onecx.document.rs.internal.model.DocumentPageResultDTO;
import gen.org.tkit.onecx.document.rs.internal.model.DocumentSearchCriteriaDTO;
import gen.org.tkit.onecx.document.rs.internal.model.StorageUploadAuditDTO;
import gen.org.tkit.onecx.document.rs.internal.model.UpdateFileMetadataRequestDTO;
import gen.org.tkit.onecx.document.rs.internal.model.UploadAttachmentPresignedUrlRequestDTO;
import gen.org.tkit.onecx.document.rs.internal.model.UploadAttachmentPresignedUrlResponseDTO;
import gen.org.tkit.onecx.filestorage.client.model.FileDeleteRequest;
import gen.org.tkit.onecx.filestorage.client.model.FileMetadataResponse;
import gen.org.tkit.onecx.filestorage.client.model.PresignedUrlResponse;
import io.quarkiverse.mockserver.test.InjectMockServerClient;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(DocumentController.class)
class DocumentControllerTest extends AbstractTest {

    private static final String DOCUMENT_ID = "test-document-id";
    private static final String ATTACHMENT_ID = "test-attachment-id";
    private static final String FILE_NAME = "test-file.pdf";
    private static final String PRESIGNED_URL = "https://mock-storage.example.com/download/test-file.pdf";
    private static final OffsetDateTime EXPIRATION = OffsetDateTime.parse("2026-12-31T23:59:59+01:00");
    private static final String SVC_MOCK_ID = "DOC_MGMT_SVC_MOCK";
    private static final String SEC_SVC_MOCK_ID = "SEC_DOC_MGMT_SVC_MOCK";
    private static final String FILE_STORAGE_MOCK_ID = "FILE_STORAGE_SVC_MOCK";

    @InjectMockServerClient
    MockServerClient mockServerClient;

    @BeforeEach
    public void resetExpectation() {
        try {
            mockServerClient.clear(SVC_MOCK_ID);
            mockServerClient.clear(FILE_STORAGE_MOCK_ID);
            mockServerClient.clear(SEC_SVC_MOCK_ID);
        } catch (Exception _) {
            // mockid not existing
        }
    }

    @Test
    @DisplayName("GET /file/{attachmentId} - should return presigned download URL when attachment and file storage respond successfully")
    void getFile_shouldReturnPresignedUrl_whenAttachmentAndStorageRespondOk() {
        var attachment = new Attachment();
        attachment.setId(ATTACHMENT_ID);
        attachment.setFileName(FILE_NAME);
        attachment.setName("Test Attachment");

        var presignedUrlResponse = new PresignedUrlResponse();
        presignedUrlResponse.setUrl(PRESIGNED_URL);
        presignedUrlResponse.setExpiration(EXPIRATION);

        mockServerClient
                .when(request()
                        .withMethod("GET")
                        .withPath("/internal/attachment/" + ATTACHMENT_ID))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(attachment)));

        mockServerClient
                .when(request()
                        .withMethod("POST")
                        .withPath("/v1/file-storage/presigned/download"))
                .withId(FILE_STORAGE_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(presignedUrlResponse)));

        var responseBody = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .get("/file/{attachmentId}", ATTACHMENT_ID)
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body()
                .as(AttachmentPresignedUrlResponseDTO.class);

        assertThat(responseBody.getUrl()).isEqualTo(PRESIGNED_URL);
        assertThat(responseBody.getExpiration()).isNotNull();
    }

    @Test
    @DisplayName("GET /file/{attachmentId} - should return 404 when attachment does not exist")
    void getFile_shouldReturnNotFound_whenAttachmentNotFound() {
        mockServerClient
                .when(request()
                        .withMethod("GET")
                        .withPath("/internal/attachment/" + ATTACHMENT_ID))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.NOT_FOUND.getStatusCode()));

        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .get("/file/{attachmentId}", ATTACHMENT_ID)
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @DisplayName("GET /file/{attachmentId} - should return 400 when attachment service returns a non-404 error")
    void getFile_shouldReturnBadRequest_whenAttachmentReturnsServerError() {
        mockServerClient
                .when(request()
                        .withMethod("GET")
                        .withPath("/internal/attachment/" + ATTACHMENT_ID))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));

        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .get("/file/{attachmentId}", ATTACHMENT_ID)
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    @DisplayName("GET /file/{attachmentId} - should return 400 when file storage fails to provide presigned download URL")
    void getFile_shouldReturnBadRequest_whenPresignedDownloadUrlFails() {
        var attachment = new Attachment();
        attachment.setId(ATTACHMENT_ID);
        attachment.setFileName(FILE_NAME);

        mockServerClient
                .when(request()
                        .withMethod("GET")
                        .withPath("/internal/attachment/" + ATTACHMENT_ID))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(attachment)));

        mockServerClient
                .when(request()
                        .withMethod("POST")
                        .withPath("/v1/file-storage/presigned/download"))
                .withId(FILE_STORAGE_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.BAD_REQUEST.getStatusCode()));

        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .get("/file/{attachmentId}", ATTACHMENT_ID)
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    // ==================== deleteDocumentById ====================

    @Test
    @DisplayName("DELETE /{id} - should return 204 when document with attachments is successfully deleted and files are removed from storage")
    void deleteDocumentById_shouldReturnNoContent_whenDocumentAndFilesDeletedSuccessfully() {
        var attachment = new Attachment();
        attachment.setId(ATTACHMENT_ID);
        attachment.setFileName(FILE_NAME);

        var documentDetail = new DocumentDetail();
        documentDetail.setId(DOCUMENT_ID);
        documentDetail.setAttachments(List.of(attachment));
        FileDeleteRequest fileDeleteRequest = new FileDeleteRequest();
        fileDeleteRequest.setApplicationId("onecx-document-bff");
        fileDeleteRequest.setProductName("onecx-document");
        fileDeleteRequest.setFileName(ATTACHMENT_ID + "_" + FILE_NAME);
        mockServerClient
                .when(request()
                        .withMethod("GET")
                        .withPath("/internal/document/" + DOCUMENT_ID))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(documentDetail)));

        mockServerClient
                .when(request()
                        .withMethod("DELETE")
                        .withPath("/internal/document/" + DOCUMENT_ID))
                .withId(SEC_SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.NO_CONTENT.getStatusCode()));

        mockServerClient
                .when(request()
                        .withMethod("POST")
                        .withPath("/v1/file-storage/file/delete")
                        .withBody(JsonBody.json(fileDeleteRequest)))
                .withId(FILE_STORAGE_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode()));

        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .delete("/{id}", DOCUMENT_ID)
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());
    }

    @Test
    @DisplayName("DELETE /{id} - should return 204 when document has no attachments")
    void deleteDocumentById_shouldReturnNoContent_whenDocumentHasNoAttachments() {
        var documentDetail = new DocumentDetail();
        documentDetail.setId(DOCUMENT_ID);
        documentDetail.setAttachments(List.of());

        mockServerClient
                .when(request()
                        .withMethod("GET")
                        .withPath("/internal/document/" + DOCUMENT_ID))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(documentDetail)));

        mockServerClient
                .when(request()
                        .withMethod("DELETE")
                        .withPath("/internal/document/" + DOCUMENT_ID))
                .withId(SEC_SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.NO_CONTENT.getStatusCode()));

        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .delete("/{id}", DOCUMENT_ID)
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());
    }

    @Test
    @DisplayName("DELETE /{id} - should return 400 when file storage fails to delete attachment file")
    void deleteDocumentById_shouldReturnBadRequest_whenFileStorageDeleteFails() {
        var attachment = new Attachment();
        attachment.setId(ATTACHMENT_ID);
        attachment.setFileName(FILE_NAME);

        var documentDetail = new DocumentDetail();
        documentDetail.setId(DOCUMENT_ID);
        documentDetail.setAttachments(List.of(attachment));

        FileDeleteRequest fileDeleteRequest = new FileDeleteRequest();
        fileDeleteRequest.setApplicationId("onecx-document-bff");
        fileDeleteRequest.setProductName("onecx-document");
        fileDeleteRequest.setFileName(ATTACHMENT_ID + "_" + FILE_NAME);
        mockServerClient
                .when(request()
                        .withMethod("GET")
                        .withPath("/internal/document/" + DOCUMENT_ID))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(documentDetail)));

        mockServerClient
                .when(request()
                        .withMethod("DELETE")
                        .withPath("/internal/document/" + DOCUMENT_ID))
                .withId(SEC_SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.NO_CONTENT.getStatusCode()));

        mockServerClient
                .when(request()
                        .withMethod("POST")
                        .withPath("/v1/file-storage/file/delete")
                        .withBody(JsonBody.json(fileDeleteRequest)))
                .withId(FILE_STORAGE_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.BAD_REQUEST.getStatusCode()));

        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .delete("/{id}", DOCUMENT_ID)
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    // ==================== uploadAllFiles ====================

    @Test
    @DisplayName("POST /files/upload/{documentId} - should return presigned upload URLs for all matched attachments")
    void uploadAllFiles_shouldReturnPresignedUploadUrls_whenAllSucceed() {
        var attachment = new Attachment();
        attachment.setId(ATTACHMENT_ID);
        attachment.setFileName(FILE_NAME);

        var documentDetail = new DocumentDetail();
        documentDetail.setId(DOCUMENT_ID);
        documentDetail.setAttachments(List.of(attachment));

        var presignedUploadResponse = new PresignedUrlResponse();
        presignedUploadResponse.setUrl(PRESIGNED_URL);
        presignedUploadResponse.setExpiration(EXPIRATION);

        mockServerClient
                .when(request()
                        .withMethod("GET")
                        .withPath("/internal/document/" + DOCUMENT_ID))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(documentDetail)));

        mockServerClient
                .when(request()
                        .withMethod("POST")
                        .withPath("/v1/file-storage/presigned/upload"))
                .withId(FILE_STORAGE_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(presignedUploadResponse)));

        var uploadRequest = new UploadAttachmentPresignedUrlRequestDTO();
        uploadRequest.setAttachmentId(ATTACHMENT_ID);
        uploadRequest.setFileName(FILE_NAME);

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(List.of(uploadRequest))
                .post("/files/upload/{documentId}", DOCUMENT_ID)
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body()
                .as(UploadAttachmentPresignedUrlResponseDTO[].class);

        assertThat(response[0].getUrl()).isEqualTo(PRESIGNED_URL);
        assertThat(response[0].getAttachmentId()).isEqualTo(ATTACHMENT_ID);
    }

    @Test
    @DisplayName("POST /files/upload/{documentId} - should return failed result when attachment file name does not match the request")
    void uploadAllFiles_shouldReturnFailedResult_whenFileNameDoesNotMatch() {
        var attachment = new Attachment();
        attachment.setId(ATTACHMENT_ID);
        attachment.setFileName("different-file-name.pdf");

        var documentDetail = new DocumentDetail();
        documentDetail.setId(DOCUMENT_ID);
        documentDetail.setAttachments(List.of(attachment));

        mockServerClient
                .when(request()
                        .withMethod("GET")
                        .withPath("/internal/document/" + DOCUMENT_ID))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(documentDetail)));

        var uploadRequest = new UploadAttachmentPresignedUrlRequestDTO();
        uploadRequest.setAttachmentId(ATTACHMENT_ID);
        uploadRequest.setFileName(FILE_NAME);

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(List.of(uploadRequest))
                .post("/files/upload/{documentId}", DOCUMENT_ID)
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body()
                .as(UploadAttachmentPresignedUrlResponseDTO[].class);

        assertThat(response[0].getUrl()).isNull();
        assertThat(response[0].getAttachmentId()).isEqualTo(ATTACHMENT_ID);
    }

    @Test
    @DisplayName("POST /files/upload/{documentId} - should return empty list when no document attachment matches the request")
    void uploadAllFiles_shouldReturnBadRequestStatus_whenAttachmentNotMatchingDocument() {
        var attachment = new Attachment();
        attachment.setId("other-attachment-id");
        attachment.setFileName(FILE_NAME);

        var documentDetail = new DocumentDetail();
        documentDetail.setId(DOCUMENT_ID);
        documentDetail.setAttachments(List.of(attachment));

        mockServerClient
                .when(request()
                        .withMethod("GET")
                        .withPath("/internal/document/" + DOCUMENT_ID))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(documentDetail)));

        var uploadRequest = new UploadAttachmentPresignedUrlRequestDTO();
        uploadRequest.setAttachmentId(ATTACHMENT_ID);
        uploadRequest.setFileName(FILE_NAME);

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(List.of(uploadRequest))
                .post("/files/upload/{documentId}", DOCUMENT_ID)
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body()
                .as(UploadAttachmentPresignedUrlResponseDTO[].class);

        assertThat(response).isEmpty();
    }

    @Test
    @DisplayName("POST /files/upload/{documentId} - should return failed result when file storage fails to provide presigned upload URL")
    void uploadAllFiles_shouldReturnFailedResult_whenPresignedUploadUrlFails() {
        var attachment = new Attachment();
        attachment.setId(ATTACHMENT_ID);
        attachment.setFileName(FILE_NAME);

        var documentDetail = new DocumentDetail();
        documentDetail.setId(DOCUMENT_ID);
        documentDetail.setAttachments(List.of(attachment));

        mockServerClient
                .when(request()
                        .withMethod("GET")
                        .withPath("/internal/document/" + DOCUMENT_ID))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(documentDetail)));

        mockServerClient
                .when(request()
                        .withMethod("POST")
                        .withPath("/v1/file-storage/presigned/upload"))
                .withId(FILE_STORAGE_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));

        var uploadRequest = new UploadAttachmentPresignedUrlRequestDTO();
        uploadRequest.setAttachmentId(ATTACHMENT_ID);
        uploadRequest.setFileName(FILE_NAME);

        List<UploadAttachmentPresignedUrlRequestDTO> requestList = List.of(uploadRequest);

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(requestList)
                .post("/files/upload/{documentId}", DOCUMENT_ID)
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body()
                .as(UploadAttachmentPresignedUrlResponseDTO[].class);

        assertThat(response[0].getUrl()).isNull();
    }

    // ==================== updateAttachmentsMetadata ====================

    @Test
    @DisplayName("PATCH /{documentId}/files/metadata - should return 200 when metadata is successfully updated in file storage and attachment service")
    void updateAttachmentsMetadata_shouldReturn200_whenAllSucceed() {
        var attachment = new Attachment();
        attachment.setId(ATTACHMENT_ID);
        attachment.setFileName(FILE_NAME);

        var documentDetail = new DocumentDetail();
        documentDetail.setId(DOCUMENT_ID);
        documentDetail.setAttachments(List.of(attachment));

        var metadataResponse = new FileMetadataResponse();
        metadataResponse.setFileName(ATTACHMENT_ID + "_" + FILE_NAME);
        metadataResponse.setSize(1024L);
        metadataResponse.setType("application/pdf");

        mockServerClient
                .when(request()
                        .withMethod("GET")
                        .withPath("/internal/document/" + DOCUMENT_ID))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(documentDetail)));

        mockServerClient
                .when(request()
                        .withMethod("POST")
                        .withPath("/v1/file-storage/file/metadata"))
                .withId(FILE_STORAGE_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(List.of(metadataResponse))));

        mockServerClient
                .when(request()
                        .withMethod("PATCH")
                        .withPath("/internal/attachment/metadata"))
                .withId(SEC_SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode()));

        var metadataRequest = new UpdateFileMetadataRequestDTO();
        metadataRequest.setAttachmentId(ATTACHMENT_ID);

        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(List.of(metadataRequest))
                .patch("/{documentId}/files/metadata", DOCUMENT_ID)
                .then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    @DisplayName("PATCH /{documentId}/files/metadata - should return 400 when file storage fails to return metadata")
    void updateAttachmentsMetadata_shouldReturnBadRequest_whenFileStorageMetadataFails() {
        var attachment = new Attachment();
        attachment.setId(ATTACHMENT_ID);
        attachment.setFileName(FILE_NAME);

        var documentDetail = new DocumentDetail();
        documentDetail.setId(DOCUMENT_ID);
        documentDetail.setAttachments(List.of(attachment));

        mockServerClient
                .when(request()
                        .withMethod("GET")
                        .withPath("/internal/document/" + DOCUMENT_ID))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(documentDetail)));

        mockServerClient
                .when(request()
                        .withMethod("POST")
                        .withPath("/v1/file-storage/file/metadata"))
                .withId(FILE_STORAGE_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));

        var metadataRequest = new UpdateFileMetadataRequestDTO();
        metadataRequest.setAttachmentId(ATTACHMENT_ID);

        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(List.of(metadataRequest))
                .patch("/{documentId}/files/metadata", DOCUMENT_ID)
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    // ==================== createFailedAttachmentsAuditLogs ====================

    @Test
    @DisplayName("PATCH /{documentId}/files/audit-log - should return 200 when audit logs are successfully created")
    void createFailedAttachmentsAuditLogs_shouldReturn200_whenAuditSucceeds() {
        var auditRequest = new UpdateFileMetadataRequestDTO();
        auditRequest.setAttachmentId(ATTACHMENT_ID);

        mockServerClient
                .when(request()
                        .withMethod("POST")
                        .withPath("/internal/attachment/storage-audit"))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode()));

        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(List.of(auditRequest))
                .patch("/{documentId}/files/audit-log", DOCUMENT_ID)
                .then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    @DisplayName("PATCH /{documentId}/files/audit-log - should return 500 when attachment service fails to create audit logs")
    void createFailedAttachmentsAuditLogs_shouldReturnBadRequest_whenAuditFails() {
        var auditRequest = new UpdateFileMetadataRequestDTO();
        auditRequest.setAttachmentId(ATTACHMENT_ID);

        mockServerClient
                .when(request()
                        .withMethod("POST")
                        .withPath("/internal/attachment/storage-audit"))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.BAD_REQUEST.getStatusCode()));

        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(List.of(auditRequest))
                .patch("/{documentId}/files/audit-log", DOCUMENT_ID)
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }

    // ==================== showAllDocumentsByCriteria ====================

    @Test
    @DisplayName("POST /search/show-all-documents - should return all matching documents")
    void showAllDocumentsByCriteria_shouldReturnMatchingDocuments() {
        var criteria = new DocumentSearchCriteriaDTO();
        criteria.setName("Test Document");

        var detail = new DocumentDetailDTO();
        detail.setId(DOCUMENT_ID);
        detail.setName("Test Document");

        mockServerClient
                .when(request()
                        .withMethod("POST")
                        .withPath("/internal/document/search/show-all-documents"))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(List.of(detail))));

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(criteria)
                .post("/search/show-all-documents")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body()
                .as(DocumentDetail[].class);

        assertThat(response[0].getId()).isEqualTo(DOCUMENT_ID);
    }

    // ==================== getDocumentByCriteria ====================

    @Test
    @DisplayName("POST /search - should return paged documents by criteria")
    void getDocumentByCriteria_shouldReturnDocumentPage_whenCriteriaMatches() {
        var criteria = new DocumentSearchCriteriaDTO();
        criteria.setName("Test Document");
        criteria.setPageNumber(0);
        criteria.setPageSize(10);

        var detail = new DocumentDetailDTO();
        detail.setId(DOCUMENT_ID);
        detail.setName("Test Document");

        var pageResult = new DocumentPageResultDTO();
        pageResult.setNumber(0);
        pageResult.setSize(10);
        pageResult.setTotalElements(1L);
        pageResult.setTotalPages(1L);
        pageResult.setStream(List.of(detail));

        mockServerClient
                .when(request()
                        .withMethod("POST")
                        .withPath("/internal/document/search"))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(pageResult)));

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(criteria)
                .post("/search")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body()
                .as(DocumentPageResultDTO.class);

        assertThat(response.getTotalElements()).isEqualTo(1L);
        assertThat(response.getStream().get(0).getId()).isEqualTo(DOCUMENT_ID);
    }

    // ==================== updateDocument ====================

    @Test
    @DisplayName("PUT /{id} - should return updated document")
    void updateDocument_shouldReturnUpdatedDocument_whenRequestIsValid() {
        var channelRequest = new ChannelCreateUpdateDTO();
        channelRequest.setName("WEB");

        var updateRequest = new DocumentCreateUpdateDTO();
        updateRequest.setName("Updated Document");
        updateRequest.setTypeId("type-1");
        updateRequest.setChannel(channelRequest);

        var channel = new ChannelDTO();
        channel.setName("WEB");

        var updatedDocument = new DocumentDetailDTO();
        updatedDocument.setId(DOCUMENT_ID);
        updatedDocument.setName("Updated Document");
        updatedDocument.setChannel(channel);

        mockServerClient
                .when(request()
                        .withMethod("PUT")
                        .withPath("/internal/document/" + DOCUMENT_ID))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(updatedDocument)));

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(updateRequest)
                .put("/{id}", DOCUMENT_ID)
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body()
                .as(DocumentDetail.class);

        assertThat(response.getId()).isEqualTo(DOCUMENT_ID);
        assertThat(response.getName()).isEqualTo("Updated Document");
    }

    // ==================== bulkUpdateDocument ====================

    @Test
    @DisplayName("PUT /bulkupdate - should return updated document payload")
    void bulkUpdateDocument_shouldReturnUpdatedDocument_whenRequestIsValid() {
        var channelRequest = new ChannelCreateUpdateDTO();
        channelRequest.setName("WEB");

        var updateRequest = new DocumentCreateUpdateDTO();
        updateRequest.setId(DOCUMENT_ID);
        updateRequest.setName("Bulk Updated Document");
        updateRequest.setTypeId("type-1");
        updateRequest.setChannel(channelRequest);

        List<DocumentCreateUpdateDTO> updateRequests = List.of(updateRequest);

        var updatedDocument = new DocumentDetail();
        updatedDocument.setId(DOCUMENT_ID);
        updatedDocument.setName("Bulk Updated Document");
        List<DocumentDetail> documentDetails = List.of(updatedDocument);

        mockServerClient
                .when(request()
                        .withMethod("PUT")
                        .withPath("/internal/document/bulkupdate")
                        .withContentType(MediaType.APPLICATION_JSON))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(documentDetails)));

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(updateRequests)
                .put("/bulkupdate")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body()
                .as(DocumentDetailDTO[].class);

        assertThat(response[0].getId()).isEqualTo(DOCUMENT_ID);
    }

    // ==================== createDocument ====================

    @Test
    @DisplayName("POST / - should create document and return payload")
    void createDocument_shouldReturnCreatedDocument_whenRequestIsValid() {
        var channelRequest = new ChannelCreateUpdateDTO();
        channelRequest.setName("WEB");

        var createRequest = new DocumentCreateUpdateDTO();
        createRequest.setName("Created Document");
        createRequest.setTypeId("type-1");
        createRequest.setChannel(channelRequest);

        var createdDocument = new DocumentDetailDTO();
        createdDocument.setId(DOCUMENT_ID);
        createdDocument.setName("Created Document");

        mockServerClient
                .when(request()
                        .withMethod("POST")
                        .withPath("/internal/document"))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.CREATED.getStatusCode())
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(createdDocument)));

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(createRequest)
                .post()
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode())
                .extract()
                .body()
                .as(DocumentDetailDTO.class);

        assertThat(response.getId()).isEqualTo(DOCUMENT_ID);
        assertThat(response.getName()).isEqualTo("Created Document");
    }

    // ==================== getFailedAttachmentData ====================

    @Test
    @DisplayName("GET /files/upload/failed/{id} - should return failed attachment audit entries")
    void getFailedAttachmentData_shouldReturnAuditEntries_whenDataExists() {
        var failedAudit = new StorageUploadAuditDTO();
        failedAudit.setAttachmentId(ATTACHMENT_ID);
        failedAudit.setDocumentId(DOCUMENT_ID);
        failedAudit.setFileName(FILE_NAME);

        mockServerClient
                .when(request()
                        .withMethod("GET")
                        .withPath("/internal/document/files/upload/failed/" + DOCUMENT_ID))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(List.of(failedAudit))));

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .get("/files/upload/failed/{id}", DOCUMENT_ID)
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body()
                .as(StorageUploadAuditDTO[].class);

        assertThat(response[0].getAttachmentId()).isEqualTo(ATTACHMENT_ID);
        assertThat(response[0].getDocumentId()).isEqualTo(DOCUMENT_ID);
    }

    // ==================== getAllChannels ====================

    @Test
    @DisplayName("GET /channels - should return available channels")
    void getAllChannels_shouldReturnChannels_whenServiceRespondsOk() {
        var channel = new ChannelDTO();
        channel.setId("channel-id");
        channel.setName("WEB");

        mockServerClient
                .when(request()
                        .withMethod("GET")
                        .withPath("/internal/document/channels"))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(List.of(channel))));

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .get("/channels")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body()
                .as(ChannelDTO[].class);

        assertThat(response[0].getId()).isEqualTo("channel-id");
        assertThat(response[0].getName()).isEqualTo("WEB");
    }

    // ==================== getDocumentById ====================

    @Test
    @DisplayName("GET /{id} - should return document by id")
    void getDocumentById_shouldReturnDocument_whenServiceRespondsOk() {
        var document = new DocumentDetailDTO();
        document.setId(DOCUMENT_ID);
        document.setName("Test Document");

        mockServerClient
                .when(request()
                        .withMethod("GET")
                        .withPath("/internal/document/" + DOCUMENT_ID))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(document)));

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .get("/{id}", DOCUMENT_ID)
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body()
                .as(DocumentDetail.class);

        assertThat(response.getId()).isEqualTo(DOCUMENT_ID);
        assertThat(response.getName()).isEqualTo("Test Document");
    }

    // ==================== deleteBulkDocuments ====================

    @Test
    @DisplayName("DELETE /delete-bulk-documents - should return 204 when bulk delete succeeds")
    void deleteBulkDocuments_shouldReturnNoContent_whenServiceRespondsNoContent() {
        mockServerClient
                .when(request()
                        .withMethod("DELETE")
                        .withPath("/internal/document/delete-bulk-documents"))
                .withId(SEC_SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.NO_CONTENT.getStatusCode()));

        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(APPLICATION_JSON)
                .body(List.of(DOCUMENT_ID))
                .delete("/delete-bulk-documents")
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());
    }
}
