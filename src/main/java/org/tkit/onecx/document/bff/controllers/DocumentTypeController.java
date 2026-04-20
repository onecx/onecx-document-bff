package org.tkit.onecx.document.bff.controllers;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.tkit.onecx.document.bff.mappers.DocumentMapper;

import gen.org.tkit.onecx.document.client.api.DocumentTypeControllerApi;
import gen.org.tkit.onecx.document.rs.internal.DocumentTypeControllerApiService;
import gen.org.tkit.onecx.document.rs.internal.model.DocumentTypeCreateUpdateDTO;
import gen.org.tkit.onecx.document.rs.internal.model.DocumentTypeDTO;

@ApplicationScoped
public class DocumentTypeController implements DocumentTypeControllerApiService {

    @Inject
    @RestClient
    DocumentTypeControllerApi documentTypeControllerApi;

    @Inject
    DocumentMapper mapper;

    @Override
    public Response createDocumentType(DocumentTypeCreateUpdateDTO documentTypeCreateUpdateDTO) {
        try (Response response = documentTypeControllerApi.createDocumentType(mapper.map(documentTypeCreateUpdateDTO))) {
            return Response.status(response.getStatus())
                    .entity(mapper.mapDocumentType(response.readEntity(DocumentTypeDTO.class)))
                    .build();
        }
    }

    @Override
    public Response deleteDocumentTypeById(String id) {
        try (Response response = documentTypeControllerApi.deleteDocumentTypeById(id)) {
            return Response.status(response.getStatus()).build();
        }
    }

    @Override
    public Response getAllTypesOfDocument() {
        try (Response response = documentTypeControllerApi.getAllTypesOfDocument()) {
            List<DocumentTypeDTO> documentTypeList = response.readEntity(new GenericType<List<DocumentTypeDTO>>() {
            });
            return Response.status(response.getStatus())
                    .entity(mapper.mapType(documentTypeList))
                    .build();
        }
    }

    @Override
    public Response getDocumentTypeById(String id) {
        try (Response response = documentTypeControllerApi.getDocumentTypeById(id)) {
            return Response.status(response.getStatus())
                    .entity(mapper.mapDocumentType(response.readEntity(DocumentTypeDTO.class)))
                    .build();
        }
    }

    @Override
    public Response updateDocumentTypeById(String id, DocumentTypeCreateUpdateDTO documentTypeCreateUpdateDTO) {
        try (Response response = documentTypeControllerApi.updateDocumentTypeById(id,
                mapper.map(documentTypeCreateUpdateDTO))) {
            return Response.status(response.getStatus())
                    .entity(mapper.mapDocumentType(response.readEntity(DocumentTypeDTO.class)))
                    .build();
        }
    }
}
