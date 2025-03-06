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

package org.eclipse.edc.identityhub.verifiablecredentials.store;

import org.assertj.core.api.Assertions;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.stream.IntStream.range;
import static org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantResource.queryByParticipantContextId;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus.INITIAL;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus.REQUESTING;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus.REVOKED;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

public abstract class CredentialStoreTestBase {
    public static final String EXAMPLE_VC_WITH_PHD_DEGREE = """
            {
              "@context": [
                "https://www.w3.org/2018/credentials/v1",
                "https://www.w3.org/2018/credentials/examples/v1"
              ],
              "id": "http://example.gov/credentials/3732",
              "type": ["VerifiableCredential", "UniversityDegreeCredential"],
              "issuer": "https://example.edu",
              "issuanceDate": "2010-01-01T19:23:24Z",
              "credentialSubject": {
                "id": "did:example:ebfeb1f712ebc6f1c276e12ec21",
                "degree": {
                  "type": "PhdDegree",
                  "name": "Doctor of Philosophy Degree"
                }
              },
              "proof": {
                "type": "Ed25519Signature2020",
                "created": "2021-11-13T18:19:39Z",
                "verificationMethod": "https://example.edu/issuers/14#key-1",
                "proofPurpose": "assertionMethod",
                "proofValue": "z58DAdFfa9SkqZMVPxAQpic7ndSayn1PzZs6ZjWp1CktyGesjuTSwRdo
                               WhAfGFCF5bppETSTojQCrfFPP2oumHKtz"
              }
            }
            """;
    public static final String TEST_PARTICIPANT_CONTEXT_ID = "test-participant";
    private static final String EXAMPLE_VC = """
            {
              "@context": [
                "https://www.w3.org/2018/credentials/v1",
                "https://www.w3.org/2018/credentials/examples/v1"
              ],
              "id": "http://example.gov/credentials/3732",
              "type": ["VerifiableCredential", "UniversityDegreeCredential"],
              "issuer": "https://example.edu",
              "issuanceDate": "2010-01-01T19:23:24Z",
              "credentialSubject": {
                "id": "did:example:ebfeb1f712ebc6f1c276e12ec21",
                "degree": {
                  "type": "BachelorDegree",
                  "name": "Bachelor of Science and Arts"
                }
              },
              "proof": {
                "type": "Ed25519Signature2020",
                "created": "2021-11-13T18:19:39Z",
                "verificationMethod": "https://example.edu/issuers/14#key-1",
                "proofPurpose": "assertionMethod",
                "proofValue": "z58DAdFfa9SkqZMVPxAQpic7ndSayn1PzZs6ZjWp1CktyGesjuTSwRdo
                               WhAfGFCF5bppETSTojQCrfFPP2oumHKtz"
              }
            }
            """;

    @Test
    void create() {
        var result = getStore().create(createCredential());
        assertThat(result).isSucceeded();

    }

    @Test
    void create_whenExists_shouldReturnFailure() {
        var credential = createCredential();
        var result = getStore().create(credential);
        assertThat(result).isSucceeded();
        var result2 = getStore().create(credential);

        assertThat(result2).isFailed().detail().contains("already exists");
    }

    @Test
    void query_byId() {
        range(0, 5)
                .mapToObj(i -> createCredentialBuilder().id("id" + i).build())
                .forEach(getStore()::create);

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("id", "=", "id2"))
                .build();

        assertThat(getStore().query(query)).isSucceeded()
                .satisfies(str -> Assertions.assertThat(str).hasSize(1));
    }

    @Test
    void query_byParticipantId() {
        range(0, 5)
                .mapToObj(i -> createCredentialBuilder()
                        .id("id" + i)
                        .participantContextId("participant" + i)
                        .build())
                .forEach(getStore()::create);

        var query = queryByParticipantContextId("participant2")
                .build();

        assertThat(getStore().query(query)).isSucceeded()
                .satisfies(str -> Assertions.assertThat(str).hasSize(1));
    }

    @Test
    void query_byParticipantIdAndType() {
        var cred1 = createCredentialBuilder()
                .credential(new VerifiableCredentialContainer(EXAMPLE_VC, CredentialFormat.VC1_0_LD, createVerifiableCredential()
                        .type("UniversityDegreeCredential")
                        .build()))
                .participantContextId(TEST_PARTICIPANT_CONTEXT_ID).build();
        var cred2 = createCredentialBuilder().participantContextId("participant-context2").build();
        var cred3 = createCredentialBuilder().participantContextId("participant-context3").build();

        Arrays.asList(cred1, cred2, cred3).forEach(getStore()::create);

        var query = queryByParticipantContextId(TEST_PARTICIPANT_CONTEXT_ID)
                .filter(new Criterion("verifiableCredential.credential.type", "contains", "UniversityDegreeCredential"))
                .build();

        var result = getStore().query(query);
        assertThat(result).isSucceeded()
                .satisfies(resources -> {
                    Assertions.assertThat(resources).hasSize(1);
                });
    }

    @Test
    void query_byVcState() {
        var creds = createCredentials();
        var expectedCred = createCredentialBuilder().state(VcStatus.REQUESTED).id("id-test").build();
        creds.add(expectedCred);
        creds.forEach(getStore()::create);

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("state", "=", VcStatus.REQUESTED.code()))
                .build();

        assertThat(getStore().query(query)).isSucceeded()
                .satisfies(str -> Assertions.assertThat(str).hasSize(1)
                        .usingRecursiveFieldByFieldElementComparator()
                        .containsExactly(expectedCred));
    }

    @Test
    void query_likeRawVc() {
        var creds = createCredentials();

        var expectedCred = createCredentialBuilder()
                .credential(new VerifiableCredentialContainer(EXAMPLE_VC_WITH_PHD_DEGREE, CredentialFormat.VC1_0_LD, createVerifiableCredential().build()))
                .build();
        creds.add(expectedCred);
        creds.forEach(getStore()::create);

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("verifiableCredential.rawVc", "like", "%PhdDegree%"))
                .build();

        assertThat(getStore().query(query)).isSucceeded()
                .satisfies(str -> Assertions.assertThat(str).hasSize(1)
                        .usingRecursiveFieldByFieldElementComparator()
                        .containsExactly(expectedCred));
    }

    @Test
    void query_byVcFormat() {
        var creds = createCredentials();

        var expectedCred = createCredentialBuilder()
                .credential(new VerifiableCredentialContainer(EXAMPLE_VC, CredentialFormat.VC1_0_JWT, createVerifiableCredential().build()))
                .build();
        creds.add(expectedCred);
        creds.forEach(getStore()::create);

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("verifiableCredential.format", "=", CredentialFormat.VC1_0_JWT.ordinal()))
                .build();

        assertThat(getStore().query(query)).isSucceeded()
                .satisfies(str -> Assertions.assertThat(str).hasSize(1)
                        .usingRecursiveFieldByFieldElementComparator()
                        .containsExactly(expectedCred));
    }

    @Test
    void query_byJsonProperty_type() {
        var creds = createCredentials();

        var expectedCred = createCredentialBuilder()
                .credential(new VerifiableCredentialContainer(EXAMPLE_VC, CredentialFormat.VC1_0_LD, createVerifiableCredential()
                        .type("TestType")
                        .build()))
                .build();
        creds.add(expectedCred);
        creds.forEach(getStore()::create);

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("verifiableCredential.credential.type", "contains", "TestType"))
                .build();

        assertThat(getStore().query(query)).isSucceeded()
                .satisfies(str -> Assertions.assertThat(str).hasSize(1)
                        .usingRecursiveFieldByFieldElementComparator()
                        .containsExactly(expectedCred));
    }

    @Test
    void query_byJsonProperty_credentialSubject() {
        var creds = createCredentials();

        var expectedCred = createCredentialBuilder()
                .credential(new VerifiableCredentialContainer(EXAMPLE_VC, CredentialFormat.VC1_0_LD, createVerifiableCredential()
                        .credentialSubject(CredentialSubject.Builder.newInstance()
                                .claim("degreeType", "PhdDegree")
                                .build())
                        .build()))
                .build();
        creds.add(expectedCred);
        creds.forEach(getStore()::create);

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("verifiableCredential.credential.credentialSubject.degreeType", "=", "PhdDegree"))
                .build();

        assertThat(getStore().query(query)).isSucceeded()
                .satisfies(str -> Assertions.assertThat(str).hasSize(1)
                        .usingRecursiveFieldByFieldElementComparator()
                        .containsExactly(expectedCred));
    }

    @Test
    void query_byJsonProperty_credentialSubjectId() {
        var creds = createCredentials();

        var expectedCred = createCredentialBuilder()
                .credential(new VerifiableCredentialContainer(EXAMPLE_VC, CredentialFormat.VC1_0_LD, createVerifiableCredential()
                        .credentialSubject(CredentialSubject.Builder.newInstance()
                                .claim("foo", "bar")
                                .id("test-subject-id")
                                .build())
                        .build()))
                .build();
        creds.add(expectedCred);
        creds.forEach(getStore()::create);

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("verifiableCredential.credential.credentialSubject.id", "=", "test-subject-id"))
                .build();

        assertThat(getStore().query(query)).isSucceeded()
                .satisfies(str -> Assertions.assertThat(str).hasSize(1)
                        .usingRecursiveFieldByFieldElementComparator()
                        .containsExactly(expectedCred));
    }

    @Test
    void query_byJsonProperty_credentialSubjectComplex() {
        var creds = createCredentials();

        var expectedCred = createCredentialBuilder()
                .credential(new VerifiableCredentialContainer(EXAMPLE_VC, CredentialFormat.VC1_0_LD, createVerifiableCredential()
                        .credentialSubject(CredentialSubject.Builder.newInstance()
                                .claim("complexSubject", Map.of(
                                        "sub-key1", "sub-value1",
                                        "sub-key2", Map.of("sub-sub-key1", "sub-sub-value1")))
                                .build())
                        .build()))
                .build();
        creds.add(expectedCred);
        creds.forEach(getStore()::create);

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("verifiableCredential.credential.credentialSubject.complexSubject.sub-key1", "=", "sub-value1"))
                .filter(new Criterion("verifiableCredential.credential.credentialSubject.complexSubject.sub-key2.sub-sub-key1", "=", "sub-sub-value1"))
                .build();

        assertThat(getStore().query(query)).isSucceeded()
                .satisfies(str -> Assertions.assertThat(str).hasSize(1)
                        .usingRecursiveFieldByFieldElementComparator()
                        .containsExactly(expectedCred));
    }

    @Test
    void query_byJsonProperty_issuanceDate() {
        var creds = createCredentials();

        var issuanceDate = Instant.parse("2023-12-11T10:15:30.00Z");
        var expectedCred = createCredentialBuilder()
                .credential(new VerifiableCredentialContainer(EXAMPLE_VC, CredentialFormat.VC1_0_LD, createVerifiableCredential()
                        .issuanceDate(issuanceDate)
                        .build()))
                .build();
        creds.add(expectedCred);
        creds.forEach(getStore()::create);

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("verifiableCredential.credential.issuanceDate", "=", issuanceDate.toString()))
                .build();

        assertThat(getStore().query(query)).isSucceeded()
                .satisfies(str -> Assertions.assertThat(str).hasSize(1)
                        .usingRecursiveFieldByFieldElementComparator()
                        .containsExactly(expectedCred));
    }

    @Test
    void query_byMetadata() {

        var expectedCred = createCredentialBuilder()
                .metadata("currentIndex", 1)
                .metadata("publicUri", "http://foo.bar.com/")
                .build();

        getStore().create(expectedCred);

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("metadata.publicUri", "ilike", "http://foo%"))
                .build();

        assertThat(getStore().query(query)).isSucceeded()
                .satisfies(str -> Assertions.assertThat(str).hasSize(1)
                        .usingRecursiveFieldByFieldElementComparator()
                        .containsExactly(expectedCred));
    }

    @Test
    void query_noQuerySpec() {
        var resources = range(0, 5)
                .mapToObj(i -> createCredentialBuilder().id("id" + i).build())
                .toList();

        resources.forEach(getStore()::create);

        var res = getStore().query(QuerySpec.none());
        assertThat(res).isSucceeded();
        Assertions.assertThat(res.getContent())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(resources.toArray(new VerifiableCredentialResource[0]));
    }

    @Test
    void query_byStatus() {
        var creds = createCredentials();

        var expectedCred = createCredentialBuilder()
                .credential(new VerifiableCredentialContainer(EXAMPLE_VC, CredentialFormat.VC1_0_LD, createVerifiableCredential().build()))
                .state(REVOKED)
                .build();
        var secondCred = createCredentialBuilder()
                .credential(new VerifiableCredentialContainer(EXAMPLE_VC, CredentialFormat.VC1_0_JWT, createVerifiableCredential().build()))
                .state(REQUESTING)
                .build();
        var thirdCred = createCredentialBuilder()
                .credential(new VerifiableCredentialContainer(EXAMPLE_VC, CredentialFormat.VC1_0_JWT, createVerifiableCredential().build()))
                .state(REVOKED)
                .build();
        creds.add(expectedCred);
        creds.add(secondCred);
        creds.add(thirdCred);
        creds.forEach(getStore()::create);

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("state", "=", REVOKED.code()))
                .build();

        assertThat(getStore().query(query)).isSucceeded()
                .satisfies(result -> Assertions.assertThat(result).hasSize(2)
                        .usingRecursiveFieldByFieldElementComparator()
                        .containsExactlyInAnyOrder(expectedCred, thirdCred));

    }

    @Test
    void query_byStatusMultiple() {
        var creds = createCredentials();

        var expectedCred = createCredentialBuilder()
                .credential(new VerifiableCredentialContainer(EXAMPLE_VC, CredentialFormat.VC1_0_LD, createVerifiableCredential().build()))
                .state(REVOKED)
                .build();
        var secondCred = createCredentialBuilder()
                .credential(new VerifiableCredentialContainer(EXAMPLE_VC, CredentialFormat.VC1_0_JWT, createVerifiableCredential().build()))
                .state(INITIAL)
                .build();
        var thirdCred = createCredentialBuilder()
                .credential(new VerifiableCredentialContainer(EXAMPLE_VC, CredentialFormat.VC1_0_JWT, createVerifiableCredential().build()))
                .state(VcStatus.NOT_YET_VALID)
                .build();
        creds.add(expectedCred);
        creds.add(secondCred);
        creds.add(thirdCred);
        creds.forEach(getStore()::create);

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("state", "in", List.of(INITIAL.code(), REVOKED.code())))
                .build();

        assertThat(getStore().query(query)).isSucceeded()
                .satisfies(result -> Assertions.assertThat(result).hasSize(2)
                        .usingRecursiveFieldByFieldElementComparator()
                        .containsExactlyInAnyOrder(expectedCred, secondCred));
    }

    @Test
    void query_whenNotFound() {
        var resources = range(0, 5)
                .mapToObj(i -> createCredentialBuilder()
                        .id("id" + i)
                        .build())
                .toList();

        resources.forEach(getStore()::create);

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("holderId", "=", "some-holder"))
                .build();
        var res = getStore().query(query);
        assertThat(res).isSucceeded();
        Assertions.assertThat(res.getContent()).isEmpty();
    }

    @Test
    void query_byInvalidField_shouldReturnEmptyList() {
        var resources = range(0, 5)
                .mapToObj(i -> createCredentialBuilder()
                        .id("id" + i)
                        .build())
                .toList();

        resources.forEach(getStore()::create);

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("invalidField", "=", "test-value"))
                .build();
        var res = getStore().query(query);
        assertThat(res).isSucceeded();
        Assertions.assertThat(res.getContent()).isNotNull().isEmpty();
    }

    @Test
    void update() {
        var credential = createCredentialBuilder();
        var result = getStore().create(credential.build());
        assertThat(result).isSucceeded();

        var updateRes = getStore().update(credential.state(VcStatus.ISSUED).build());
        assertThat(updateRes).isSucceeded();
    }

    @Test
    void update_whenIdChanges_fails() {
        var credential = createCredentialBuilder();
        var result = getStore().create(credential.build());

        var updateRes = getStore().update(credential.state(VcStatus.ISSUED).id("another-id").build());
        assertThat(updateRes).isFailed().detail().contains("with ID 'another-id' does not exist.");
    }

    @Test
    void update_whenNotExists() {
        var credential = createCredentialBuilder();
        var updateRes = getStore().update(credential.state(VcStatus.ISSUED).id("another-id").build());
        assertThat(updateRes).isFailed().detail().contains("with ID 'another-id' does not exist.");
    }

    @Test
    void delete() {
        var credential = createCredential();
        getStore().create(credential);

        var deleteRes = getStore().deleteById(credential.getId());
        assertThat(deleteRes).isSucceeded();
    }

    @Test
    void delete_whenNotExists() {
        assertThat(getStore().deleteById("not-exist")).isFailed()
                .detail().contains("with ID 'not-exist' does not exist.");
    }

    protected abstract CredentialStore getStore();

    protected VerifiableCredentialResource createCredential() {
        return createCredentialBuilder()
                .build();
    }

    protected VerifiableCredentialResource.Builder createCredentialBuilder() {

        return VerifiableCredentialResource.Builder.newInstance()
                .issuerId("test-issuer")
                .holderId("test-holder")
                .state(VcStatus.ISSUED)
                .metadata("foo", "bar")
                .participantContextId(TEST_PARTICIPANT_CONTEXT_ID)
                .credential(new VerifiableCredentialContainer(EXAMPLE_VC, CredentialFormat.VC1_0_LD, createVerifiableCredential().build()))
                .id(UUID.randomUUID().toString());
    }

    @NotNull
    private ArrayList<VerifiableCredentialResource> createCredentials() {
        return new ArrayList<>(range(0, 5)
                .mapToObj(i -> createCredentialBuilder().id("id" + i).build())
                .toList());
    }

    private VerifiableCredential.Builder createVerifiableCredential() {
        return VerifiableCredential.Builder.newInstance()
                .credentialSubject(CredentialSubject.Builder.newInstance().id("test-subject").claim("test-key", "test-val").build())
                .issuanceDate(Instant.now())
                .type("VerifiableCredential")
                .issuer(new Issuer("test-issuer", Map.of()))
                .id("did:web:test-credential");
    }
}
