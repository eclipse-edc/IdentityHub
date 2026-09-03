/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.protocols.dcp.issuer;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpProfileRegistry;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.CredentialObject;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.DcpProfile;
import org.eclipse.edc.identityhub.spi.participantcontext.model.IdentityHubParticipantContext;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.CredentialDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.spi.result.ServiceResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DcpIssuerMetadataServiceImplTest {

    private static final String PARTICIPANT_CONTEXT_ID = "issuer-context";
    private static final String ISSUER_DID = "did:web:issuer";
    private static final String DEFINITION_ID = "membership-credential-definition";
    private static final String SCHEMA_URL = "https://example.org/schemas/membership.json";

    private final CredentialDefinitionService credentialDefinitionService = mock();
    private final DcpProfileRegistry profileRegistry = mock();
    private final DcpIssuerMetadataServiceImpl service = new DcpIssuerMetadataServiceImpl(credentialDefinitionService, profileRegistry);

    private final IdentityHubParticipantContext participantContext = IdentityHubParticipantContext.Builder.newInstance()
            .participantContextId(PARTICIPANT_CONTEXT_ID)
            .did(ISSUER_DID)
            .apiTokenAlias("apiAlias")
            .build();

    // IS-META-01: the metadata reports the issuer DID and one CredentialObject per configured definition
    @Test
    @DisplayName("IS-META-01: issuer metadata reports the issuer DID and the configured credential definitions")
    void getIssuerMetadata_reportsIssuerAndSupportedCredentials() {
        stubDefinitions(credentialDefinition());

        var result = service.getIssuerMetadata(participantContext);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent().getIssuer()).isEqualTo(ISSUER_DID);
        assertThat(result.getContent().getCredentialsSupported()).hasSize(1);
    }

    // IS-META-02: §6.7 requires every CredentialObject in credentialsSupported to carry ALL optional properties
    @Test
    @DisplayName("IS-META-02: every CredentialObject carries all optional properties with well-formed values")
    void getIssuerMetadata_credentialObjectIsComplete() {
        stubDefinitions(credentialDefinition());

        var credentialObject = single(service.getIssuerMetadata(participantContext).getContent().getCredentialsSupported());

        assertThat(credentialObject.getId()).isEqualTo(DEFINITION_ID);
        assertThat(credentialObject.getCredentialType()).isEqualTo("MembershipCredential");
        assertThat(credentialObject.getCredentialSchema()).isEqualTo(SCHEMA_URL);
        assertThat(credentialObject.getBindingMethods()).containsExactly("did:web");
        assertThat(credentialObject.getProfile()).isEqualTo("vc11-sl2021/jwt");
        assertThat(credentialObject.getOfferReason()).isNotBlank();
        assertThat(credentialObject.getIssuancePolicy()).isNotNull();
        assertThat(credentialObject.getIssuancePolicy().getId()).isNotBlank();
    }

    // IS-META-03: clients reference and cache CredentialObjects by id, so nothing about them may change between fetches
    @Test
    @DisplayName("IS-META-03: two metadata fetches produce identical CredentialObjects")
    void getIssuerMetadata_isStableAcrossFetches() {
        stubDefinitions(credentialDefinition());

        var first = single(service.getIssuerMetadata(participantContext).getContent().getCredentialsSupported());
        var second = single(service.getIssuerMetadata(participantContext).getContent().getCredentialsSupported());

        assertThat(first.getId()).isEqualTo(second.getId());
        // the issuance policy used to be generated with a random id, which made the object differ on every fetch
        assertThat(first.getIssuancePolicy().getId()).isEqualTo(second.getIssuancePolicy().getId());
    }

    private CredentialObject single(java.util.Collection<CredentialObject> credentialObjects) {
        assertThat(credentialObjects).hasSize(1);
        return credentialObjects.iterator().next();
    }

    private void stubDefinitions(CredentialDefinition... definitions) {
        when(profileRegistry.profilesFor(CredentialFormat.VC1_0_JWT))
                .thenReturn(List.of(new DcpProfile("vc11-sl2021/jwt", CredentialFormat.VC1_0_JWT, "StatusList2021Entry")));
        when(credentialDefinitionService.queryCredentialDefinitions(any())).thenReturn(ServiceResult.success(List.of(definitions)));
    }

    private CredentialDefinition credentialDefinition() {
        return CredentialDefinition.Builder.newInstance()
                .id(DEFINITION_ID)
                .credentialType("MembershipCredential")
                .jsonSchemaUrl(SCHEMA_URL)
                .jsonSchema("{}")
                .participantContextId(PARTICIPANT_CONTEXT_ID)
                .formatFrom(CredentialFormat.VC1_0_JWT)
                .build();
    }
}
