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
import java.util.stream.Collectors;

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
            issuerMetadata.credentialSupported(credentialObject.getContent());
        }
        return ServiceResult.success(issuerMetadata.build());
    }

    public ServiceResult<CredentialObject> toCredentialObject(CredentialDefinition credentialDefinition) {

        var profiles = credentialDefinition.getFormats().stream().flatMap(format -> profileRegistry.profilesFor(format).stream())
                .map(DcpProfile::name)
                .collect(Collectors.toSet());

        var credentialObject = CredentialObject.Builder.newInstance()
                .credentialType(credentialDefinition.getCredentialType())
                .bindingMethod("did:web")
                .offerReason("reissue") // todo hardcoded?
                .profiles(profiles.stream().toList())
                .issuancePolicy(PresentationDefinition.Builder.newInstance().id(credentialDefinition.getId()).build())
                .build();

        return ServiceResult.success(credentialObject);
    }


}
