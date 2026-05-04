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

import io.quarkiverse.mockserver.test.InjectMockServerClient;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@TestHTTPEndpoint(SupportedMimeTypeController.class)
class SupportedMimeTypeControllerTest extends AbstractTest {

    private static final String SVC_MOCK_ID = "DOC_MGMT_SVC_MOCK";

    @InjectMockServerClient
    MockServerClient mockServerClient;

    @BeforeEach
    public void resetExpectation() {
        try {
            mockServerClient.clear(SVC_MOCK_ID);
        } catch (Exception _) {
            // mockid not existing
        }
    }

    @Test
    @DisplayName("GET / - should return all supported mime types")
    void getAllSupportedMimeTypes_shouldReturnMimeTypes_whenServiceRespondsOk() throws Exception {
        var mimeType = "image/png";

        mockServerClient
                .when(request()
                        .withMethod("GET")
                        .withPath("/internal/supported-mime-type"))
                .withId(SVC_MOCK_ID)
                .respond(response()
                        .withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(List.of(mimeType))));

        var response = given()
                .when()
                .auth().oauth2(keycloakClient.getAccessToken(ADMIN))
                .header(APM_HEADER_PARAM, ADMIN)
                .contentType(ContentType.JSON)
                .get()
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract()
                .body().as(String[].class);

        assertThat(response[0]).isEqualTo(mimeType);
    }
}
