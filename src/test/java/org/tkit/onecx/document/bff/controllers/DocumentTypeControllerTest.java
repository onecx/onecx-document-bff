package org.tkit.onecx.document.bff.controllers;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.JsonBody.json;

import java.util.List;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.JsonBody;
import org.tkit.onecx.document.bff.AbstractTest;

import gen.org.tkit.onecx.document.client.model.DocumentType;
import gen.org.tkit.onecx.document.rs.internal.model.DocumentTypeCreateUpdateDTO;
import gen.org.tkit.onecx.document.rs.internal.model.DocumentTypeDTO;
import io.quarkiverse.mockserver.test.InjectMockServerClient;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@TestHTTPEndpoint(DocumentTypeController.class)
class DocumentTypeControllerTest extends AbstractTest {

    private static final String TYPE_ID = "test-type-id";
    private static final String USERNAME_TOKEN = "apm-username";
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
        } catch (Exception e) {
            // mockid not existing
        }
    }

    @Test
    @DisplayName("GET / - should return all document types")
    void getAllTypesOfDocument_shouldReturnDocumentTypes_whenServiceRespondsOk() throws Exception {
        var type = new DocumentType();
        type.setId(TYPE_ID);
        type.setName("Invoice");

        mockServerClient
                .when(request()
                        .withMethod("GET")
                        .withPath("/internal/document-type"))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(List.of(type))));

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(ContentType.JSON)
                .get()
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body().as(DocumentTypeDTO[].class);

        assertThat(response[0].getId()).isEqualTo(TYPE_ID);
        assertThat(response[0].getName()).isEqualTo("Invoice");
    }

    @Test
    @DisplayName("POST / - should create document type")
    void createDocumentType_shouldReturnCreatedDocumentType_whenRequestIsValid() throws Exception {
        var requestDto = new DocumentTypeCreateUpdateDTO();
        requestDto.setName("Invoice");

        var type = new DocumentType();
        type.setId(TYPE_ID);
        type.setName("Invoice");

        mockServerClient
                .when(request()
                        .withMethod("POST")
                        .withPath("/internal/document-type"))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.CREATED.getStatusCode())
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(type)));

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(ContentType.JSON)
                .body(requestDto)
                .post()
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode())
                .extract()
                .body().as(DocumentTypeDTO.class);

        assertThat(response.getId()).isEqualTo(TYPE_ID);
    }

    @Test
    @DisplayName("GET /{id} - should return document type by id")
    void getDocumentTypeById_shouldReturnDocumentType_whenServiceRespondsOk() throws Exception {
        var type = new DocumentType();
        type.setId(TYPE_ID);
        type.setName("Invoice");

        mockServerClient
                .when(request()
                        .withMethod("GET")
                        .withPath("/internal/document-type/" + TYPE_ID))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(type)));

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(ContentType.JSON)
                .get("/{id}", TYPE_ID)
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body().as(DocumentTypeDTO.class);

        assertThat(response.getId()).isEqualTo(TYPE_ID);
    }

    @Test
    @DisplayName("PUT /{id} - should update document type by id")
    void updateDocumentTypeById_shouldReturnUpdatedDocumentType_whenRequestIsValid() throws Exception {
        var requestDto = new DocumentTypeCreateUpdateDTO();
        requestDto.setName("Invoice V2");

        var type = new DocumentType();
        type.setId(TYPE_ID);
        type.setName("Invoice V2");

        mockServerClient
                .when(request()
                        .withMethod("PUT")
                        .withPath("/internal/document-type/" + TYPE_ID))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.CREATED.getStatusCode())
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(type)));

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(ContentType.JSON)
                .body(requestDto)
                .put("/{id}", TYPE_ID)
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode())
                .extract()
                .body().as(DocumentTypeDTO.class);

        assertThat(response.getName()).isEqualTo("Invoice V2");
    }

    @Test
    @DisplayName("DELETE /{id} - should delete document type by id")
    void deleteDocumentTypeById_shouldReturnNoContent_whenServiceRespondsNoContent() {
        mockServerClient
                .when(request()
                        .withMethod("DELETE")
                        .withPath("/internal/document-type/" + TYPE_ID))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.NO_CONTENT.getStatusCode()));

        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(ContentType.JSON)
                .delete("/{id}", TYPE_ID)
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());
    }
}
