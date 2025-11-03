/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.api.verifiablecredential.v1.unstable.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ParticipantManifestTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void verify_serdes() throws JsonProcessingException {
        var manifest = ParticipantManifest.Builder.newInstance()
                .serviceEndpoint(new Service("id", "type", "foobar"))
                .active(true)
                .participantContextId("test-id")
                .key(KeyDescriptor.Builder.newInstance()
                        .keyId("key-id")
                        .privateKeyAlias("alias")
                        .publicKeyJwk(Map.of("foo", "bar"))
                        .publicKeyPem("pem formatted text")
                        .keyGeneratorParams(Map.of("bar", "baz"))
                        .build())
                .build();

        var json = mapper.writeValueAsString(manifest);
        assertThat(json).isNotNull();

        var deser = mapper.readValue(json, ParticipantManifest.class);
        assertThat(deser).usingRecursiveComparison().isEqualTo(manifest);
    }

    @Test
    void verify_deserialize_singleValueAsArray() throws JsonProcessingException {
        var json = """
                {
                    "roles":[],
                    "serviceEndpoints":[
                        {
                            "type": "CredentialService",
                            "serviceEndpoint": "http://foo.com/bar",
                            "id": "cred-srv"
                        }
                    ],
                    "active": true,
                    "participantContextId": "quizzquazz",
                    "did": "did:web:quizzquazz",
                    "keys":{
                        "keyId": "key-1",
                        "privateKeyAlias": "key-1-alias",
                        "usage": "sign_token",
                        "keyGeneratorParams":{
                            "algorithm": "EdDSA",
                            "curve": "Ed25519"
                        }
                    }
                }
                """;
        var manifest = mapper.readValue(json, ParticipantManifest.class);

        assertThat(manifest.getServiceEndpoints()).hasSize(1);
        assertThat(manifest.getKeys()).hasSize(1)
                .allSatisfy(kd -> assertThat(kd.getKeyId()).isEqualTo("key-1"));
    }

    @Test
    void verify_deserialize_array() throws JsonProcessingException {
        var json = """
                {
                    "roles":[],
                    "serviceEndpoints":[
                        {
                            "type": "CredentialService",
                            "serviceEndpoint": "http://foo.com/bar",
                            "id": "cred-srv"
                        }
                    ],
                    "active": true,
                    "participantContextId": "quizzquazz",
                    "did": "did:web:quizzquazz",
                    "keys":[{
                        "keyId": "key-1",
                        "privateKeyAlias": "key-1-alias",
                        "usage": "sign_token",
                        "keyGeneratorParams":{
                            "algorithm": "EdDSA",
                            "curve": "Ed25519"
                        }
                    },
                    {
                        "keyId": "key-2",
                        "privateKeyAlias": "key-2-alias",
                        "usage": "sign_presentation",
                        "keyGeneratorParams":{
                            "algorithm": "EdDSA",
                            "curve": "Ed25519"
                        }
                    }
                    ]
                }
                """;
        var manifest = mapper.readValue(json, ParticipantManifest.class);

        assertThat(manifest.getServiceEndpoints()).hasSize(1);
        assertThat(manifest.getKeys()).hasSize(2)
                .anySatisfy(kd -> assertThat(kd.getKeyId()).isEqualTo("key-1"))
                .anySatisfy(kd -> assertThat(kd.getKeyId()).isEqualTo("key-2"));
    }
}