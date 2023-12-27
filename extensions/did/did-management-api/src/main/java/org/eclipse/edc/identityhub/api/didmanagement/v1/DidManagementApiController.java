/*
 *  Copyright (c) 2023 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.api.didmanagement.v1;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.identithub.did.spi.DidDocumentService;
import org.eclipse.edc.identithub.did.spi.model.DidState;
import org.eclipse.edc.identityhub.api.didmanagement.v1.validation.DidRequestValidator;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;

import java.util.Collection;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v1/dids")
public class DidManagementApiController implements DidManagementApi {

    private final DidDocumentService documentService;
    private final DidRequestValidator requestValidator;

    public DidManagementApiController(DidDocumentService documentService) {
        this.documentService = documentService;
        this.requestValidator = new DidRequestValidator();
    }

    @POST
    @Override
    public void createDidDocument(DidDocument document, @QueryParam("publish") boolean publish) {
        requestValidator.validate(document).orElseThrow(ValidationFailureException::new);

        documentService.store(document)
                .compose(v -> publish ? documentService.publish(document.getId()) : ServiceResult.success())
                .orElseThrow(exceptionMapper(DidDocument.class, document.getId()));
    }

    @Override
    @POST
    @Path("/publish")
    public void publishDidFromBody(DidRequestPayload didRequestPayload) {
        documentService.publish(didRequestPayload.did())
                .orElseThrow(exceptionMapper(DidDocument.class, didRequestPayload.did()));
    }

    @Override
    @POST
    @Path("/unpublish")
    public void unpublishDidFromBody(DidRequestPayload didRequestPayload) {
        documentService.unpublish(didRequestPayload.did())
                .orElseThrow(exceptionMapper(DidDocument.class, didRequestPayload.did()));
    }

    @PUT
    @Override
    public void updateDid(DidDocument document, @QueryParam("republish") boolean republish) {
        requestValidator.validate(document).orElseThrow(ValidationFailureException::new);
        var did = document.getId();
        documentService.update(document)
                .compose(v -> republish ? documentService.publish(did) : ServiceResult.success())
                .orElseThrow(exceptionMapper(DidDocument.class, did));
    }

    @Override
    @POST
    @Path("/delete")
    public void deleteDidFromBody(DidRequestPayload request) {
        documentService.deleteById(request.did())
                .orElseThrow(exceptionMapper(DidDocument.class, request.did()));
    }

    @POST
    @Path("/query")
    @Override
    public Collection<DidDocument> queryDid(QuerySpec querySpec) {
        return documentService.queryDocuments(querySpec)
                .orElseThrow(exceptionMapper(DidDocument.class, null));
    }

    @Override
    @POST
    @Path("/state")
    public String getState(DidRequestPayload request) {
        var byId = documentService.findById(request.did());
        return byId != null ? DidState.from(byId.getState()).toString() : null;
    }

}
