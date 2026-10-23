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

package org.eclipse.edc.issuerservice.credentials.statuslist.bitstring;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.BitString;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.result.StoreResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC1_0_JWT;
import static org.eclipse.edc.issuerservice.credentials.statuslist.bitstring.StatusListType.BITSTRING_STATUS_LIST;
import static org.eclipse.edc.issuerservice.credentials.statuslist.bitstring.StatusListType.STATUS_LIST_2021;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BitstringStatusListFactoryTest {

    private final CredentialStore credentialStore = mock();
    private final BitstringStatusListFactory factory = new BitstringStatusListFactory(credentialStore, BITSTRING_STATUS_LIST);

    @Test
    void shouldCreateStatusInfo() {
        var status = new CredentialStatus("id", "BitstringStatusListEntry", Map.of(
                "statusPurpose", "revocation",
                "statusListIndex", "1234",
                "statusListCredential", "https://example.com/credentials/status/credentialId"));
        var statusList = VerifiableCredentialResource.Builder.newStatusList().issuerId("issuer").holderId("holder").build();
        when(credentialStore.queryForUpdate(any())).thenReturn(StoreResult.success(List.of(statusList)));

        var result = factory.create(status);

        assertThat(result).isSucceeded().isInstanceOfSatisfying(BitstringStatusInfo.class, info -> {
            assertThat(info.index()).isEqualTo(1234);
            assertThat(info.statusListCredential()).isSameAs(statusList);
        });
        verify(credentialStore).queryForUpdate(argThat(querySpec -> querySpec.getFilterExpression()
                .contains(criterion("verifiableCredential.credential.id", "=", "https://example.com/credentials/status/credentialId"))));
    }

    @Test
    void shouldOnlyConsiderStatusListsOfItsType() {
        var factory = new BitstringStatusListFactory(credentialStore, STATUS_LIST_2021);
        var status = new CredentialStatus("id", "StatusList2021Entry", Map.of(
                "statusPurpose", "revocation",
                "statusListIndex", "1234",
                "statusListCredential", "https://example.com/credentials/status/credentialId"));
        when(credentialStore.queryForUpdate(any())).thenReturn(StoreResult.success(emptyList()));

        var result = factory.create(status);

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(NOT_FOUND);
        verify(credentialStore).queryForUpdate(argThat(querySpec -> querySpec.getFilterExpression()
                .contains(criterion("verifiableCredential.credential.credentialSubject.type", "=", "StatusList2021"))));
    }

    @Test
    void shouldSetStatusOnStatusList2021() {
        var factory = new BitstringStatusListFactory(credentialStore, STATUS_LIST_2021);
        var status = new CredentialStatus("id", "StatusList2021Entry", Map.of(
                "statusPurpose", "revocation",
                "statusListIndex", "7",
                "statusListCredential", "https://example.com/credentials/status/credentialId"));
        var emptyList = STATUS_LIST_2021.encode(BitString.Builder.newInstance().size(16 * 1024 * 8).build()).getContent();
        var statusList = VerifiableCredentialResource.Builder.newStatusList()
                .issuerId("issuer")
                .holderId("holder")
                .credential(new VerifiableCredentialContainer("raw-token", VC1_0_JWT, VerifiableCredential.Builder.newInstance()
                        .id("https://example.com/credentials/status/credentialId")
                        .types(List.of("VerifiableCredential", "StatusList2021Credential"))
                        .issuer(new Issuer("did:web:issuer"))
                        .issuanceDate(Instant.now())
                        .credentialSubject(CredentialSubject.Builder.newInstance()
                                .id("urn:uuid:list")
                                .claim("type", "StatusList2021")
                                .claim("statusPurpose", "revocation")
                                .claim("encodedList", emptyList)
                                .build())
                        .build()))
                .build();
        when(credentialStore.queryForUpdate(any())).thenReturn(StoreResult.success(List.of(statusList)));

        var info = factory.create(status).getContent();

        assertThat(info.getStatus()).isSucceeded().isNull();
        assertThat(info.setStatus(true)).isSucceeded();
        assertThat(info.getStatus()).isSucceeded().isEqualTo("revocation");
        var encodedList = statusList.getVerifiableCredential().credential().getCredentialSubject().get(0).getClaims().get("encodedList").toString();
        // StatusList2021 requires a base64 encoded list, without the multibase prefix of the Bitstring Status List
        assertThat(encodedList).startsWith("H4sI");
        assertThat(BitString.Parser.newInstance().parse(encodedList).getContent().get(7)).isTrue();
    }

    @Test
    void shouldFail_whenNoIndex() {
        var status = new CredentialStatus("id", "BitstringStatusListEntry", Map.of(
                "statusPurpose", "revocation",
                "statusListCredential", "https://example.com/credentials/status/1234"));

        var result = factory.create(status);

        assertThat(result).isFailed().detail().contains("the 'statusListIndex' field is missing");
    }

    @Test
    void shouldFail_whenNoStatusListCredential() {
        var status = new CredentialStatus("id", "BitstringStatusListEntry", Map.of(
                "statusPurpose", "revocation",
                "statusListIndex", "1234"));

        var result = factory.create(status);

        assertThat(result).isFailed().detail().contains("the 'statusListCredential' field is missing");
    }

    @Test
    void shouldFail_whenCredentialNotFound() {
        var status = new CredentialStatus("id", "BitstringStatusListEntry", Map.of("statusPurpose", "revocation",
                "statusListIndex", "1234",
                "statusListCredential", "https://example.com/credentials/status/1234"));
        when(credentialStore.queryForUpdate(any())).thenReturn(StoreResult.success(emptyList()));

        var result = factory.create(status);

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(NOT_FOUND);
    }

    @Test
    void shouldFail_whenQueryFails() {
        var status = new CredentialStatus("id", "BitstringStatusListEntry", Map.of("statusPurpose", "revocation",
                "statusListIndex", "1234",
                "statusListCredential", "https://example.com/credentials/status/1234"));
        when(credentialStore.queryForUpdate(any())).thenReturn(StoreResult.generalError("failure"));

        var result = factory.create(status);

        assertThat(result).isFailed().messages().contains("failure");
    }
}
