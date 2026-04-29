package org.tkit.onecx.document.bff;

import org.eclipse.microprofile.config.ConfigProvider;
import org.mockserver.serialization.ObjectMapperFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.quarkiverse.mockserver.test.MockServerTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import shaded_package.com.fasterxml.jackson.annotation.JsonInclude;

@QuarkusTestResource(MockServerTestResource.class)
public abstract class AbstractTest {

    protected static final String ADMIN = "alice";
    public KeycloakTestClient keycloakClient = new KeycloakTestClient();
    protected static final String APM_HEADER_PARAM = ConfigProvider.getConfig()
            .getValue("%test.tkit.rs.context.token.header-param", String.class);

    static {
        RestAssured.config = RestAssuredConfig.config().objectMapperConfig(
                ObjectMapperConfig.objectMapperConfig().jackson2ObjectMapperFactory(
                        (cls, charset) -> {
                            ObjectMapper objectMapper = new ObjectMapper();
                            objectMapper.registerModule(new JavaTimeModule());
                            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
                            return objectMapper;
                        }));
        ObjectMapperFactory.createObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

}
