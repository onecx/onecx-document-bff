package org.tkit.onecx.document.bff.controllers;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import gen.org.tkit.onecx.document.client.api.SupportedMimeTypeControllerApi;
import gen.org.tkit.onecx.document.rs.internal.SupportedMimeTypeControllerApiService;

@ApplicationScoped
public class SupportedMimeTypeController implements SupportedMimeTypeControllerApiService {

    @Inject
    @RestClient
    SupportedMimeTypeControllerApi supportedMimeTypeControllerApi;

    @Override
    public Response getAllSupportedMimeTypes() {
        try (Response response = supportedMimeTypeControllerApi.getAllSupportedMimeTypes()) {
            return Response.status(response.getStatus())
                    .entity((response.readEntity(new GenericType<List<String>>() {
                    })))
                    .build();
        }
    }
}
