package org.tkit.onecx.document.bff.controllers;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.tkit.onecx.document.bff.mappers.DocumentMapper;

import gen.org.tkit.onecx.document.client.api.SupportedMimeTypeControllerApi;
import gen.org.tkit.onecx.document.rs.internal.SupportedMimeTypeControllerApiService;
import gen.org.tkit.onecx.document.rs.internal.model.SupportedMimeTypeCreateUpdateDTO;
import gen.org.tkit.onecx.document.rs.internal.model.SupportedMimeTypeDTO;

@ApplicationScoped
public class SupportedMimeTypeController implements SupportedMimeTypeControllerApiService {

    @Inject
    @RestClient
    SupportedMimeTypeControllerApi supportedMimeTypeControllerApi;

    @Inject
    DocumentMapper mapper;

    @Override
    public Response createSupportedMimeType(SupportedMimeTypeCreateUpdateDTO supportedMimeTypeCreateUpdateDTO) {
        try (Response response = supportedMimeTypeControllerApi
                .createSupportedMimeType(mapper.map(supportedMimeTypeCreateUpdateDTO))) {
            return Response.status(response.getStatus())
                    .entity(mapper.map(response.readEntity((SupportedMimeTypeDTO.class))))
                    .build();
        }
    }

    @Override
    public Response deleteSupportedMimeTypeId(String id) {
        try (Response response = supportedMimeTypeControllerApi.deleteSupportedMimeTypeId(id)) {
            return Response.status(response.getStatus()).build();
        }
    }

    @Override
    public Response getAllSupportedMimeTypes() {
        try (Response response = supportedMimeTypeControllerApi.getAllSupportedMimeTypes()) {
            return Response.status(response.getStatus())
                    .entity(mapper.mapMimeTypeList(response.readEntity(new GenericType<List<SupportedMimeTypeDTO>>() {
                    })))
                    .build();
        }
    }

    @Override
    public Response getSupportedMimeTypeById(String id) {
        try (Response response = supportedMimeTypeControllerApi.getSupportedMimeTypeById(id)) {
            return Response.status(response.getStatus())
                    .entity(mapper.map(response.readEntity(SupportedMimeTypeDTO.class)))
                    .build();
        }
    }

    @Override
    public Response updateSupportedMimeTypeById(String id, SupportedMimeTypeCreateUpdateDTO supportedMimeTypeCreateUpdate) {
        try (Response response = supportedMimeTypeControllerApi.updateSupportedMimeTypeById(id,
                mapper.map(supportedMimeTypeCreateUpdate))) {
            return Response.status(response.getStatus())
                    .entity(mapper.map(response.readEntity(SupportedMimeTypeDTO.class)))
                    .build();
        }
    }
}
