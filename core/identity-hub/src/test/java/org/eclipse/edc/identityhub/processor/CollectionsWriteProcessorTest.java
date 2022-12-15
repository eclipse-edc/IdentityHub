/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.identityhub.spi.model.Descriptor;
import org.eclipse.edc.identityhub.spi.model.MessageRequestObject;
import org.eclipse.edc.identityhub.spi.model.MessageResponseObject;
import org.eclipse.edc.identityhub.spi.model.MessageStatus;
import org.eclipse.edc.identityhub.store.spi.IdentityHubRecord;
import org.eclipse.edc.identityhub.store.spi.IdentityHubStore;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.buildSignedJwt;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.eclipse.edc.identityhub.junit.testfixtures.VerifiableCredentialTestUtil.generateVerifiableCredential;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


class CollectionsWriteProcessorTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String ISSUER = "http://some.test.url";
    private static final String SUBJECT = "http://some.test.url";

    private IdentityHubStore identityHubStore;
    private CollectionsWriteProcessor writeProcessor;

    private static Stream<Arguments> invalidInputProvider() throws JOSEException {
        var missingRecordIdDescriptor = descriptorBuilder()
                .method("test")
                .dateCreated(Instant.now().getEpochSecond())
                .build();

        var missingDateCreatedDescriptor = descriptorBuilder()
                .method("test")
                .recordId(UUID.randomUUID().toString())
                .build();

        var verifiableCredentialWithoutId = new JWTClaimsSet.Builder()
                .claim("vc", "{ \"credentialSubject\": { \"foo\": \"bar\" }}")
                .issuer(ISSUER)
                .subject(SUBJECT)
                .expirationTime(null)
                .notBeforeTime(null)
                .build();
        var dataWithInvalidVc = buildSignedJwt(verifiableCredentialWithoutId, generateEcKey()).serialize().getBytes(StandardCharsets.UTF_8);

        var jws = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).build(), new JWTClaimsSet.Builder().build());
        jws.sign(new ECDSASigner(generateEcKey().toECPrivateKey()));
        var dataWithoutMandatoryVcField = jws.serialize().getBytes(StandardCharsets.UTF_8);

        return Stream.of(
                // valid descriptor but invalid data
                Arguments.of(MessageRequestObject.Builder.newInstance().descriptor(getValidDescriptor()).data(dataWithInvalidVc).build()),
                Arguments.of(MessageRequestObject.Builder.newInstance().descriptor(getValidDescriptor()).data(dataWithoutMandatoryVcField).build()),
                Arguments.of(MessageRequestObject.Builder.newInstance().descriptor(getValidDescriptor()).data("{".getBytes(StandardCharsets.UTF_8)).build()),
                Arguments.of(MessageRequestObject.Builder.newInstance().descriptor(getValidDescriptor()).data("invalid base64".getBytes(StandardCharsets.UTF_8)).build()),
                // valid date but invalid descriptor
                Arguments.of(MessageRequestObject.Builder.newInstance().descriptor(missingRecordIdDescriptor).data(getValidData()).build()),
                Arguments.of(MessageRequestObject.Builder.newInstance().descriptor(missingDateCreatedDescriptor).data(getValidData()).build())
        );
    }

    private static Descriptor.Builder descriptorBuilder() {
        return Descriptor.Builder.newInstance();
    }

    private static MessageRequestObject getValidMessageRequestObject() {
        return MessageRequestObject.Builder.newInstance()
                .descriptor(getValidDescriptor())
                .data(getValidData())
                .build();
    }

    private static Descriptor getValidDescriptor() {
        return descriptorBuilder()
                .method("test")
                .dateCreated(Instant.now().getEpochSecond())
                .recordId(UUID.randomUUID().toString())
                .build();
    }

    private static byte[] getValidData() {
        var verifiableCredential = generateVerifiableCredential();
        return buildSignedJwt(verifiableCredential, ISSUER, SUBJECT, generateEcKey()).serialize().getBytes(StandardCharsets.UTF_8);
    }

    @BeforeEach
    void setUp() {
        identityHubStore = mock(IdentityHubStore.class);
        writeProcessor = new CollectionsWriteProcessor(identityHubStore, OBJECT_MAPPER, mock(Monitor.class), new NoopTransactionContext());
    }

    @ParameterizedTest
    @MethodSource("invalidInputProvider")
    void writeCredentials_invalidInput(MessageRequestObject requestObject) {
        // Arrange
        var expectedResult = MessageResponseObject.Builder.newInstance().status(MessageStatus.MALFORMED_MESSAGE).build();

        // Act
        var result = writeProcessor.process(requestObject);

        // Assert
        verify(identityHubStore, never()).add(any());
        assertThat(result).usingRecursiveComparison().isEqualTo(expectedResult);
    }

    @Test
    void writeCredentials_addStoreFailure() {
        // Arrange
        doThrow(new EdcException("store error")).when(identityHubStore).add(any());
        var requestObject = getValidMessageRequestObject();
        var expectedResult = MessageResponseObject.Builder.newInstance().status(MessageStatus.UNHANDLED_ERROR).build();

        // Act
        var result = writeProcessor.process(requestObject);

        // Assert
        assertThat(result).usingRecursiveComparison().isEqualTo(expectedResult);
    }

    @Test
    void writeCredentials() {
        // Arrange
        var requestObject = getValidMessageRequestObject();

        // Act
        var result = writeProcessor.process(requestObject);

        // Assert
        var expectedResult = MessageResponseObject.Builder.newInstance().status(MessageStatus.OK).build();

        var captor = ArgumentCaptor.forClass(IdentityHubRecord.class);
        verify(identityHubStore).add(captor.capture());
        assertThat(result).usingRecursiveComparison().isEqualTo(expectedResult);
        var record = captor.getValue();
        assertThat(record).isNotNull();
        assertThat(record.getId()).isEqualTo(requestObject.getDescriptor().getRecordId());
        assertThat(record.getCreatedAt()).isEqualTo(requestObject.getDescriptor().getDateCreated());
        assertThat(record.getPayload()).isEqualTo(requestObject.getData());
    }
}
