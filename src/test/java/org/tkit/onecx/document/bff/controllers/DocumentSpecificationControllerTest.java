package org.tkit.onecx.document.bff.controllers;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.JsonBody.json;

import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.JsonBody;
import org.tkit.onecx.document.bff.AbstractTest;

import gen.org.tkit.onecx.document.client.model.DocumentSpecification;
import gen.org.tkit.onecx.document.rs.internal.model.DocumentSpecificationCreateUpdateDTO;
import gen.org.tkit.onecx.document.rs.internal.model.DocumentSpecificationDTO;
import io.quarkiverse.mockserver.test.InjectMockServerClient;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@TestHTTPEndpoint(DocumentSpecificationController.class)
class DocumentSpecificationControllerTest extends AbstractTest {

    private static final String SPEC_ID = "test-spec-id";
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
    @DisplayName("GET / - should return all document specifications")
    void getAllDocumentSpecifications_shouldReturnSpecifications_whenServiceRespondsOk() throws Exception {
        var specification = new DocumentSpecification();
        specification.setId(SPEC_ID);
        specification.setName("Contract Spec");
        specification.setSpecificationVersion("1.0");
        List<DocumentSpecification> specifications = List.of(specification);

        mockServerClient
                .when(request()
                        .withMethod("GET")
                        .withPath("/internal/document-specification"))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(specifications)));

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(ContentType.JSON)
                .get()
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body().as(DocumentSpecificationDTO[].class);

        assertThat(Arrays.stream(response).toList().get(0).getId()).isEqualTo(SPEC_ID);
        assertThat(Arrays.stream(response).toList().get(0).getName()).isEqualTo("Contract Spec");
    }

    @Test
    @DisplayName("POST / - should create document specification")
    void createDocumentSpecification_shouldReturnCreatedSpecification_whenRequestIsValid() throws Exception {
        var requestDto = new DocumentSpecificationCreateUpdateDTO();
        requestDto.setName("Contract Spec");
        requestDto.setSpecificationVersion("1.0");

        var specification = new DocumentSpecification();
        specification.setId(SPEC_ID);
        specification.setName("Contract Spec");
        specification.setSpecificationVersion("1.0");

        mockServerClient
                .when(request()
                        .withMethod("POST")
                        .withPath("/internal/document-specification"))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.CREATED.getStatusCode())
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(specification)));

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
                .body().as(DocumentSpecificationDTO.class);

        assertThat(response.getId()).isEqualTo(SPEC_ID);
    }

    @Test
    @DisplayName("GET /{id} - should return document specification by id")
    void getDocumentSpecificationById_shouldReturnSpecification_whenServiceRespondsOk() throws Exception {
        var specification = new DocumentSpecification();
        specification.setId(SPEC_ID);
        specification.setName("Contract Spec");
        specification.setSpecificationVersion("1.0");

        mockServerClient
                .when(request()
                        .withMethod("GET")
                        .withPath("/internal/document-specification/" + SPEC_ID))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(specification)));

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(ContentType.JSON)
                .get("/{id}", SPEC_ID)
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body().as(DocumentSpecificationDTO.class);

        assertThat(response.getId()).isEqualTo(SPEC_ID);
    }

    @Test
    @DisplayName("PUT /{id} - should update document specification by id")
    void updateDocumentSpecificationById_shouldReturnUpdatedSpecification_whenRequestIsValid() throws Exception {
        var requestDto = new DocumentSpecificationCreateUpdateDTO();
        requestDto.setName("Contract Spec");
        requestDto.setSpecificationVersion("2.0");

        var specification = new DocumentSpecification();
        specification.setId(SPEC_ID);
        specification.setName("Contract Spec");
        specification.setSpecificationVersion("2.0");

        mockServerClient
                .when(request()
                        .withMethod("PUT")
                        .withPath("/internal/document-specification/" + SPEC_ID))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(specification)));

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(ContentType.JSON)
                .body(requestDto)
                .put("/{id}", SPEC_ID)
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body().as(DocumentSpecificationDTO.class);
        assertThat(response.getSpecificationVersion()).isEqualTo("2.0");
    }

    @Test
    @DisplayName("DELETE /{id} - should delete document specification by id")
    void deleteDocumentSpecificationById_shouldReturnNoContent_whenServiceRespondsNoContent() {
        mockServerClient
                .when(request()
                        .withMethod("DELETE")
                        .withPath("/internal/document-specification/" + SPEC_ID))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.NO_CONTENT.getStatusCode()));

        given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(ContentType.JSON)
                .delete("/{id}", SPEC_ID)
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());
    }
}
