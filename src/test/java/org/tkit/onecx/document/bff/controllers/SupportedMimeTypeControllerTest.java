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
import org.tkit.onecx.document.bff.AbstractTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import gen.org.tkit.onecx.document.rs.internal.model.SupportedMimeTypeCreateUpdateDTO;
import gen.org.tkit.onecx.document.rs.internal.model.SupportedMimeTypeDTO;
import io.quarkiverse.mockserver.test.InjectMockServerClient;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@TestHTTPEndpoint(SupportedMimeTypeController.class)
class SupportedMimeTypeControllerTest extends AbstractTest {

    private static final String MIME_ID = "test-mime-id";
    private static final String USERNAME_TOKEN = "apm-username";
    private static final String SVC_MOCK_ID = "DOC_MGMT_SVC_MOCK";
    private static final String SEC_SVC_MOCK_ID = "SEC_DOC_MGMT_SVC_MOCK";
    private static final String FILE_STORAGE_MOCK_ID = "FILE_STORAGE_SVC_MOCK";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

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
    @DisplayName("GET / - should return all supported mime types")
    void getAllSupportedMimeTypes_shouldReturnMimeTypes_whenServiceRespondsOk() throws Exception {
        var mimeType = new SupportedMimeTypeDTO();
        mimeType.setId(MIME_ID);
        mimeType.setName("application/pdf");
        mimeType.setDescription("PDF Document");

        mockServerClient
                .when(request()
                        .withMethod("GET")
                        .withPath("/internal/supported-mime-type"))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(json(MAPPER.writeValueAsString(List.of(mimeType)))));

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(USERNAME_TOKEN, ADMIN)
                .header(APM_HEADER_PARAM, createToken(ADMIN, "org1"))
                .contentType(ContentType.JSON)
                .get()
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body()
                .jsonPath();

        assertThat(response.getString("[0].id")).isEqualTo(MIME_ID);
        assertThat(response.getString("[0].name")).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("POST / - should create supported mime type")
    void createSupportedMimeType_shouldReturnCreatedMimeType_whenRequestIsValid() throws Exception {
        var requestDto = new SupportedMimeTypeCreateUpdateDTO();
        requestDto.setName("application/pdf");
        requestDto.setDescription("PDF Document");

        var mimeType = new SupportedMimeTypeDTO();
        mimeType.setId(MIME_ID);
        mimeType.setName("application/pdf");
        mimeType.setDescription("PDF Document");

        mockServerClient
                .when(request()
                        .withMethod("POST")
                        .withPath("/internal/supported-mime-type"))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.CREATED.getStatusCode())
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(json(MAPPER.writeValueAsString(mimeType))));

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(USERNAME_TOKEN, ADMIN)
                .header(APM_HEADER_PARAM, createToken(ADMIN, "org1"))
                .contentType(ContentType.JSON)
                .body(MAPPER.writeValueAsString(requestDto))
                .post()
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode())
                .extract()
                .body()
                .jsonPath();

        assertThat(response.getString("id")).isEqualTo(MIME_ID);
    }

    @Test
    @DisplayName("GET /{id} - should return supported mime type by id")
    void getSupportedMimeTypeById_shouldReturnMimeType_whenServiceRespondsOk() throws Exception {
        var mimeType = new SupportedMimeTypeDTO();
        mimeType.setId(MIME_ID);
        mimeType.setName("application/pdf");
        mimeType.setDescription("PDF Document");

        mockServerClient
                .when(request()
                        .withMethod("GET")
                        .withPath("/internal/supported-mime-type/" + MIME_ID))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(json(MAPPER.writeValueAsString(mimeType))));

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(USERNAME_TOKEN, ADMIN)
                .header(APM_HEADER_PARAM, createToken(ADMIN, "org1"))
                .contentType(ContentType.JSON)
                .get("/{id}", MIME_ID)
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body()
                .jsonPath();

        assertThat(response.getString("id")).isEqualTo(MIME_ID);
    }

    @Test
    @DisplayName("PUT /{id} - should update supported mime type by id")
    void updateSupportedMimeTypeById_shouldReturnUpdatedMimeType_whenRequestIsValid() throws Exception {
        var requestDto = new SupportedMimeTypeCreateUpdateDTO();
        requestDto.setName("application/pdf");
        requestDto.setDescription("Portable Document Format");

        var mimeType = new SupportedMimeTypeDTO();
        mimeType.setId(MIME_ID);
        mimeType.setName("application/pdf");
        mimeType.setDescription("Portable Document Format");

        mockServerClient
                .when(request()
                        .withMethod("PUT")
                        .withPath("/internal/supported-mime-type/" + MIME_ID))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(json(MAPPER.writeValueAsString(mimeType))));

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(USERNAME_TOKEN, ADMIN)
                .header(APM_HEADER_PARAM, createToken(ADMIN, "org1"))
                .contentType(ContentType.JSON)
                .body(MAPPER.writeValueAsString(requestDto))
                .put("/{id}", MIME_ID)
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body()
                .jsonPath();

        assertThat(response.getString("description")).isEqualTo("Portable Document Format");
    }

    @Test
    @DisplayName("DELETE /{id} - should delete supported mime type by id")
    void deleteSupportedMimeTypeId_shouldReturnNoContent_whenServiceRespondsNoContent() {
        mockServerClient
                .when(request()
                        .withMethod("DELETE")
                        .withPath("/internal/supported-mime-type/" + MIME_ID))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.NO_CONTENT.getStatusCode()));

        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(USERNAME_TOKEN, ADMIN)
                .header(APM_HEADER_PARAM, createToken(ADMIN, "org1"))
                .contentType(ContentType.JSON)
                .delete("/{id}", MIME_ID)
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());
    }
}
