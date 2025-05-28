/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.protocols.dcp.issuer;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.presentationdefinition.PresentationDefinition;
import org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpIssuerMetadataService;
import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpProfileRegistry;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.DcpProfile;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.IssuerMetadata;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.CredentialDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantResource.queryByParticipantContextId;

public class DcpIssuerMetadataServiceImpl implements DcpIssuerMetadataService {

    private final CredentialDefinitionService credentialDefinitionService;

    private final DcpProfileRegistry profileRegistry;

    public DcpIssuerMetadataServiceImpl(CredentialDefinitionService credentialDefinitionService, DcpProfileRegistry profileRegistry) {
        this.credentialDefinitionService = credentialDefinitionService;
        this.profileRegistry = profileRegistry;
    }

    @Override
    public ServiceResult<IssuerMetadata> getIssuerMetadata(ParticipantContext participantContext) {
        return credentialDefinitionService.queryCredentialDefinitions(queryByParticipantContextId(participantContext.getParticipantContextId()).build())
                .compose(credentialDefinitions -> createIssuerMetadata(participantContext, credentialDefinitions));
    }

    public ServiceResult<IssuerMetadata> createIssuerMetadata(ParticipantContext participantContext, Collection<CredentialDefinition> credentialDefinitions) {
        var issuerMetadata = IssuerMetadata.Builder.newInstance().issuer(participantContext.getDid());
        for (var credentialDefinition : credentialDefinitions) {
            var credentialObject = toCredentialObject(credentialDefinition);
            if (credentialObject.failed()) {
                return credentialObject.mapFailure();
            }
            credentialObject.getContent().forEach(issuerMetadata::credentialSupported);
        }
        return ServiceResult.success(issuerMetadata.build());
    }

    public ServiceResult<List<CredentialObject>> toCredentialObject(CredentialDefinition credentialDefinition) {

        var credentialObjects = profileRegistry.profilesFor(credentialDefinition.getFormatAsEnum()).stream()
                .map(DcpProfile::name)
                .map(profile -> CredentialObject.Builder.newInstance()
                        .id(credentialDefinition.getId())
                        .credentialType(credentialDefinition.getCredentialType())
                        .bindingMethod("did:web")
                        .offerReason("reissue") // todo hardcoded?
                        .profile(profile)
                        .issuancePolicy(PresentationDefinition.Builder.newInstance().id(UUID.randomUUID().toString()).build())
                        .build())
                .toList();

        return ServiceResult.success(credentialObjects);
    }


}
