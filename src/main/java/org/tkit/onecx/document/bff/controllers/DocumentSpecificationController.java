package org.tkit.onecx.document.bff.controllers;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.tkit.onecx.document.bff.mappers.DocumentMapper;

import gen.org.tkit.onecx.document.client.api.DocumentSpecificationControllerApi;
import gen.org.tkit.onecx.document.rs.internal.DocumentSpecificationControllerApiService;
import gen.org.tkit.onecx.document.rs.internal.model.DocumentSpecificationCreateUpdateDTO;
import gen.org.tkit.onecx.document.rs.internal.model.DocumentSpecificationDTO;

@ApplicationScoped
public class DocumentSpecificationController implements DocumentSpecificationControllerApiService {
    @Inject
    @RestClient
    DocumentSpecificationControllerApi documentSpecificationControllerApi;

    @Inject
    DocumentMapper mapper;

    @Override
    public Response createDocumentSpecification(DocumentSpecificationCreateUpdateDTO documentSpecificationCreateUpdateDTO) {
        try (Response response = documentSpecificationControllerApi
                .createDocumentSpecification(mapper.map(documentSpecificationCreateUpdateDTO))) {
            return Response.status(response.getStatus())
                    .entity(mapper.map(response.readEntity(DocumentSpecificationDTO.class)))
                    .build();
        }
    }

    @Override
    public Response deleteDocumentSpecificationById(String id) {
        try (Response response = documentSpecificationControllerApi.deleteDocumentSpecificationById(id)) {
            return Response.status(response.getStatus()).build();
        }
    }

    @Override
    public Response getAllDocumentSpecifications() {
        try (Response response = documentSpecificationControllerApi.getAllDocumentSpecifications()) {
            List<DocumentSpecificationDTO> documentSpecificationList = response
                    .readEntity(new GenericType<List<DocumentSpecificationDTO>>() {
                    });
            return Response.status(response.getStatus())
                    .entity(mapper.mapSpecification(documentSpecificationList))
                    .build();
        }
    }

    @Override
    public Response getDocumentSpecificationById(String id) {
        try (Response response = documentSpecificationControllerApi.getDocumentSpecificationById(id)) {
            return Response.status(response.getStatus())
                    .entity(mapper.map(response.readEntity(DocumentSpecificationDTO.class)))
                    .build();
        }
    }

    @Override
    public Response updateDocumentSpecificationById(String id,
            DocumentSpecificationCreateUpdateDTO documentSpecificationCreateUpdateDTO) {
        try (Response response = documentSpecificationControllerApi.updateDocumentSpecificationById(id,
                mapper.map(documentSpecificationCreateUpdateDTO))) {
            return Response.status(response.getStatus())
                    .entity(mapper.map(response.readEntity(DocumentSpecificationDTO.class)))
                    .build();
        }
    }
}
