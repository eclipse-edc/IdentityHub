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

package org.eclipse.dataspaceconnector.identityhub.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.identityhub.models.MessageResponseObject;
import org.eclipse.dataspaceconnector.identityhub.models.MessageStatus;
import org.eclipse.dataspaceconnector.identityhub.models.credentials.VerifiableCredential;
import org.eclipse.dataspaceconnector.identityhub.store.IdentityHubInMemoryStore;
import org.eclipse.dataspaceconnector.identityhub.store.IdentityHubStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.identityhub.models.MessageResponseObject.MESSAGE_ID_VALUE;


public class CollectionsWriteProcessorTest {

    private static final Faker FAKER = new Faker();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private IdentityHubStore identityHubStore;
    private CollectionsWriteProcessor writeProcessor;

    @BeforeEach
    void setUp() {
        identityHubStore = new IdentityHubInMemoryStore();
        writeProcessor = new CollectionsWriteProcessor(identityHubStore, OBJECT_MAPPER);
    }

    @Test
    void writeCredentials() throws Exception {
        // Arrange
        var CREDENTIAL_ID = FAKER.internet().uuid();
        var verifiableCredentialMap = Map.of("id", CREDENTIAL_ID);
        var data = OBJECT_MAPPER.writeValueAsString(verifiableCredentialMap).getBytes(StandardCharsets.UTF_8);

        // Act
        var result = writeProcessor.process(data);

        // Assert
        var expectedResult = MessageResponseObject.Builder.newInstance().messageId(MESSAGE_ID_VALUE).status(MessageStatus.OK).build();
        var expectedVerifiableCredential = VerifiableCredential.Builder.newInstance().id(CREDENTIAL_ID).build();
        assertThat(result).usingRecursiveComparison().isEqualTo(expectedResult);
        assertThat(identityHubStore.getAll()).usingRecursiveFieldByFieldElementComparator().containsExactly(expectedVerifiableCredential);
    }

    @Test
    void writeCredentialsWithWrongJsonFormat() {
        // Arrange
        var malformedJson = "{";
        byte[] data = Base64.getEncoder().encode(malformedJson.getBytes(StandardCharsets.UTF_8));
        var expectedResult = MessageResponseObject.Builder.newInstance().messageId(MESSAGE_ID_VALUE).status(MessageStatus.MALFORMED_MESSAGE).build();

        // Act
        var result = writeProcessor.process(data);

        // Assert
        assertThat(result).usingRecursiveComparison().isEqualTo(expectedResult);
        assertThat(identityHubStore.getAll()).isEmpty();
    }

    @Test
    void writeCredentialsWithInvalidBase64() {
        // Arrange
        byte[] data = "invalid base64".getBytes(StandardCharsets.UTF_8);
        var expectedResult = MessageResponseObject.Builder.newInstance().messageId(MESSAGE_ID_VALUE).status(MessageStatus.MALFORMED_MESSAGE).build();

        // Act
        var result = writeProcessor.process(data);

        // Assert
        assertThat(result).usingRecursiveComparison().isEqualTo(expectedResult);
        assertThat(identityHubStore.getAll()).isEmpty();
    }

    @Test
    void writeNonSupportedCredential() {
        // Arrange
        byte[] data = "{ \"invalid\": \"cred\"}".getBytes(StandardCharsets.UTF_8);
        var expectedResult = MessageResponseObject.Builder.newInstance().messageId(MESSAGE_ID_VALUE).status(MessageStatus.MALFORMED_MESSAGE).build();

        // Act
        var result = writeProcessor.process(data);

        // Assert
        assertThat(result).usingRecursiveComparison().isEqualTo(expectedResult);
        assertThat(identityHubStore.getAll()).isEmpty();
    }
}
