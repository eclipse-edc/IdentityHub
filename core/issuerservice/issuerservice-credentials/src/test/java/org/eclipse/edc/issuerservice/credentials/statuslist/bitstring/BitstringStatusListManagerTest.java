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

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.DataModelVersion;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.BitString;
import org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.model.IdentityHubParticipantContext;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListCredentialPublisher;
import org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGeneratorRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC1_0_JWT;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC2_0_JOSE;
import static org.eclipse.edc.issuerservice.credentials.statuslist.bitstring.BitstringStatusListConfiguration.DEFAULT_RENEWAL_PERIOD_SECONDS;
import static org.eclipse.edc.issuerservice.credentials.statuslist.bitstring.BitstringStatusListConfiguration.DEFAULT_RENEWAL_THRESHOLD_SECONDS;
import static org.eclipse.edc.issuerservice.credentials.statuslist.bitstring.BitstringStatusListConfiguration.MIN_BITSTRING_SIZE;
import static org.eclipse.edc.issuerservice.credentials.statuslist.bitstring.BitstringStatusListManager.BITSTRING_SIZE;
import static org.eclipse.edc.issuerservice.credentials.statuslist.bitstring.BitstringStatusListManager.CURRENT_INDEX;
import static org.eclipse.edc.issuerservice.credentials.statuslist.bitstring.BitstringStatusListManager.IS_ACTIVE;
import static org.eclipse.edc.issuerservice.credentials.statuslist.bitstring.BitstringStatusListManager.PUBLIC_URL;
import static org.eclipse.edc.issuerservice.credentials.statuslist.bitstring.StatusListType.BITSTRING_STATUS_LIST;
import static org.eclipse.edc.issuerservice.credentials.statuslist.bitstring.StatusListType.STATUS_LIST_2021;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.ServiceResult.success;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class BitstringStatusListManagerTest {
    private static final String PARTICIPANT_CONTEXT_ID = "participant-context-id";
    private static final String CREDENTIAL_URL = "http://foo.bar.com/barbaz/credential1.json";
    private final CredentialStore store = mock();
    private final CredentialGeneratorRegistry generator = mock();
    private final IdentityHubParticipantContextService participantContextService = mock();
    private final StatusListCredentialPublisher publisher = mock();
    private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");
    private static final Duration VALIDITY = Duration.ofDays(365);
    private final Monitor monitor = mock();
    private final BitstringStatusListManager manager = manager(BITSTRING_STATUS_LIST);

    @BeforeEach
    void setUp() {
        when(publisher.publish(any())).thenReturn(Result.success(CREDENTIAL_URL));
        when(publisher.publicUrl(any())).thenAnswer(i -> "https://issuer.example.com/statuslist/" + i.getArgument(0));
        when(participantContextService.getParticipantContext(eq(PARTICIPANT_CONTEXT_ID)))
                .thenReturn(success(IdentityHubParticipantContext.Builder.newInstance()
                        .participantContextId(PARTICIPANT_CONTEXT_ID)
                        .apiTokenAlias("api-token-alias")
                        .did("did:web:" + PARTICIPANT_CONTEXT_ID)
                        .build()));

        when(generator.signCredential(any(), any(), any()))
                .thenAnswer(i -> Result.success(new VerifiableCredentialContainer("test-raw-token", i.getArgument(2), i.getArgument(1))));
        // no status lists of a type, unless a test has some
        when(store.query(any())).thenReturn(StoreResult.success(List.of()));
    }

    @Test
    void getActiveCredential() {
        when(store.queryForUpdate(any())).thenReturn(StoreResult.success(List.of(createVerifiableCredentialResource()
                .metadata(CURRENT_INDEX, 42)
                .metadata(PUBLIC_URL, "http://bar.com/quizz")
                .metadata(IS_ACTIVE, true)
                .build())));
        var entry = manager.getActiveCredential(PARTICIPANT_CONTEXT_ID);
        assertThat(entry).isSucceeded()
                .satisfies(e -> {
                    assertThat(e.credentialUrl()).isNotNull();
                    assertThat(e.statusListIndex()).isEqualTo(42);
                });

        // oldest first, so that all runtimes lock the status list credentials in the same order
        verify(store).queryForUpdate(argThat(query -> "timestamp".equals(query.getSortField()) && query.getSortOrder() == SortOrder.ASC));
        verifyNoMoreInteractions(store, generator, participantContextService);
    }

    @Test
    void getActiveCredential_shouldConsiderAllStatusLists() {
        when(store.queryForUpdate(any())).thenReturn(StoreResult.success(List.of(createVerifiableCredentialResource()
                .metadata(CURRENT_INDEX, 42)
                .metadata(IS_ACTIVE, true)
                .build())));

        manager.getActiveCredential(PARTICIPANT_CONTEXT_ID);

        // not only as many as by default, which could leave out the active one
        verify(store).queryForUpdate(argThat(query -> query.getLimit() == Integer.MAX_VALUE));
    }

    @Test
    void getActiveCredential_whenExpired_shouldRenewAndPublish() {
        var expired = statusListResource(MIN_BITSTRING_SIZE, NOW.minus(Duration.ofDays(1)), 7)
                .metadata(BITSTRING_SIZE, MIN_BITSTRING_SIZE)
                .metadata(CURRENT_INDEX, 42)
                .metadata(PUBLIC_URL, "http://bar.com/expired")
                .metadata(IS_ACTIVE, true)
                .build();
        when(store.queryForUpdate(any())).thenReturn(StoreResult.success(List.of(expired)));
        when(store.update(any())).thenReturn(StoreResult.success());

        var entry = manager.getActiveCredential(PARTICIPANT_CONTEXT_ID);

        assertThat(entry).isSucceeded().satisfies(e -> {
            assertThat(e.statusListCredential().getId()).isEqualTo(expired.getId());
            assertThat(e.statusListIndex()).isEqualTo(42);
            // it keeps the URL it was published at first, not the one returned when it is published again
            assertThat(e.credentialUrl()).isEqualTo("http://bar.com/expired");
            assertThat(e.statusListCredential().getVerifiableCredential().credential().getExpirationDate()).isEqualTo(NOW.plus(VALIDITY));
            var bitString = bitString(e.statusListCredential());
            assertThat(IntStream.range(0, bitString.length()).filter(bitString::get)).containsExactly(7);
        });
        var inOrder = inOrder(publisher, store);
        inOrder.verify(publisher).publish(argThat(r -> r.getId().equals(expired.getId())));
        inOrder.verify(store).update(argThat(r -> r.getId().equals(expired.getId()) &&
                r.getVerifiableCredential().credential().getExpirationDate().equals(NOW.plus(VALIDITY))));
        verify(store, never()).create(any());
    }

    @Test
    void getActiveCredential_whenExpired_andPublishingFails_shouldUseItAsItIs() {
        var expired = statusListResource(MIN_BITSTRING_SIZE, NOW.minus(Duration.ofDays(1)))
                .metadata(BITSTRING_SIZE, MIN_BITSTRING_SIZE)
                .metadata(CURRENT_INDEX, 42)
                .metadata(PUBLIC_URL, "http://bar.com/expired")
                .metadata(IS_ACTIVE, true)
                .build();
        var originalCredential = expired.getVerifiableCredential();
        when(store.queryForUpdate(any())).thenReturn(StoreResult.success(List.of(expired)));
        when(publisher.publish(any())).thenReturn(Result.failure("publication target unavailable"));

        var entry = manager.getActiveCredential(PARTICIPANT_CONTEXT_ID);

        assertThat(entry).isSucceeded().satisfies(e -> {
            assertThat(e.statusListCredential().getVerifiableCredential()).isSameAs(originalCredential);
            assertThat(e.statusListIndex()).isEqualTo(42);
        });
        verify(store, never()).update(any());
        verify(store, never()).create(any());
        verify(monitor).warning(contains("publication target unavailable"));
    }

    @Test
    void getActiveCredential_whenExpired_andSigningFails_shouldUseItAsItIs() {
        var expired = statusListResource(MIN_BITSTRING_SIZE, NOW.minus(Duration.ofDays(1)))
                .metadata(BITSTRING_SIZE, MIN_BITSTRING_SIZE)
                .metadata(CURRENT_INDEX, 42)
                .metadata(IS_ACTIVE, true)
                .build();
        var originalCredential = expired.getVerifiableCredential();
        when(store.queryForUpdate(any())).thenReturn(StoreResult.success(List.of(expired)));
        when(generator.signCredential(any(), any(), any())).thenReturn(Result.failure("vault unavailable"));

        var entry = manager.getActiveCredential(PARTICIPANT_CONTEXT_ID);

        assertThat(entry).isSucceeded().satisfies(e -> assertThat(e.statusListCredential().getVerifiableCredential()).isSameAs(originalCredential));
        verify(store, never()).update(any());
        verify(publisher, never()).publish(any());
        verify(monitor).warning(contains("vault unavailable"));
    }

    @Test
    void getActiveCredential_whenOtherStatusListExpired_shouldNotRenewIt() {
        var expiredInactive = statusListResource(MIN_BITSTRING_SIZE, NOW.minus(Duration.ofDays(1)))
                .metadata(BITSTRING_SIZE, MIN_BITSTRING_SIZE)
                .metadata(CURRENT_INDEX, MIN_BITSTRING_SIZE)
                .metadata(IS_ACTIVE, false)
                .build();
        var active = createVerifiableCredentialResource()
                .metadata(CURRENT_INDEX, 42)
                .metadata(IS_ACTIVE, true)
                .build();
        when(store.queryForUpdate(any())).thenReturn(StoreResult.success(List.of(expiredInactive, active)));

        var entry = manager.getActiveCredential(PARTICIPANT_CONTEXT_ID);

        // left to the periodic renewal
        assertThat(entry).isSucceeded().satisfies(e -> assertThat(e.statusListCredential().getId()).isEqualTo(active.getId()));
        verify(generator, never()).signCredential(any(), any(), any());
        verify(store, never()).update(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void getActiveCredential_credentialNotFound() {
        when(store.queryForUpdate(any())).thenReturn(StoreResult.notFound("foobar"));
        var entry = manager.getActiveCredential(PARTICIPANT_CONTEXT_ID);
        assertThat(entry).isFailed().detail().contains("foobar");
        verify(store).queryForUpdate(any());
        verifyNoMoreInteractions(store, generator, participantContextService);
    }

    @Test
    void getActiveCredential_whenEmptyResult_shouldCreateNew() {
        when(store.queryForUpdate(any())).thenReturn(StoreResult.success(List.of()));
        when(store.update(any())).thenReturn(StoreResult.success());
        when(store.create(any())).thenReturn(StoreResult.success());

        var entry = manager.getActiveCredential(PARTICIPANT_CONTEXT_ID);
        assertThat(entry).isSucceeded()
                .satisfies(e -> {
                    assertThat(e.credentialUrl()).isNotNull();
                    assertThat(e.statusListIndex()).isEqualTo(0);
                    assertThat(e.credentialUrl()).isEqualTo(CREDENTIAL_URL);
                });

        verify(store, times(2)).queryForUpdate(any());
        verify(store).create(hasParticipantId(PARTICIPANT_CONTEXT_ID));
        verify(store).update(hasParticipantId(PARTICIPANT_CONTEXT_ID));
        verify(participantContextService).getParticipantContext(PARTICIPANT_CONTEXT_ID);
        verify(generator).signCredential(eq(PARTICIPANT_CONTEXT_ID), any(), eq(CredentialFormat.VC2_0_JOSE));
        verifyNoMoreInteractions(store, generator, participantContextService);
    }

    @Test
    void getActiveCredential_whenEmptyResult_shouldIdentifyNewOneByItsUrl() {
        when(store.queryForUpdate(any())).thenReturn(StoreResult.success(List.of()));
        when(store.update(any())).thenReturn(StoreResult.success());
        when(store.create(any())).thenReturn(StoreResult.success());

        var entry = manager.getActiveCredential(PARTICIPANT_CONTEXT_ID);

        assertThat(entry).isSucceeded().satisfies(e -> {
            var credential = e.statusListCredential().getVerifiableCredential().credential();
            assertThat(credential.getId()).startsWith("https://issuer.example.com/statuslist/");
            assertThat(credential.getCredentialSubject()).singleElement().satisfies(subject -> assertThat(subject.getId()).startsWith("urn:uuid:"));
            assertThat(e.createCredentialStatus().id()).startsWith("urn:uuid:");
        });
    }

    @Test
    void getActiveCredential_whenFull_andNewOneCreatedMeanwhile_shouldUseIt() {
        var fullCredential = createVerifiableCredentialResource()
                .metadata(CURRENT_INDEX, MIN_BITSTRING_SIZE)
                .metadata(PUBLIC_URL, "http://bar.com/full")
                .metadata(IS_ACTIVE, true)
                .build();
        // created by another runtime while this one waited for the lock, so only the second query sees it
        var newCredential = createVerifiableCredentialResource()
                .metadata(CURRENT_INDEX, 1)
                .metadata(PUBLIC_URL, "http://bar.com/new")
                .metadata(IS_ACTIVE, true)
                .build();
        when(store.queryForUpdate(any()))
                .thenReturn(StoreResult.success(List.of(fullCredential)))
                .thenReturn(StoreResult.success(List.of(fullCredential, newCredential)));

        var entry = manager.getActiveCredential(PARTICIPANT_CONTEXT_ID);
        assertThat(entry).isSucceeded()
                .satisfies(e -> {
                    assertThat(e.statusListCredential().getId()).isEqualTo(newCredential.getId());
                    assertThat(e.statusListIndex()).isEqualTo(1);
                    assertThat(e.credentialUrl()).isEqualTo("http://bar.com/new");
                });

        verify(store, times(2)).queryForUpdate(any());
        verifyNoMoreInteractions(store, generator, participantContextService);
    }

    @Test
    void getActiveCredential_whenFull_andSecondQueryFails_shouldReturnFailure() {
        when(store.queryForUpdate(any()))
                .thenReturn(StoreResult.success(List.of(createVerifiableCredentialResource()
                        .metadata(CURRENT_INDEX, MIN_BITSTRING_SIZE)
                        .metadata(PUBLIC_URL, "http://bar.com/full")
                        .metadata(IS_ACTIVE, true)
                        .build())))
                .thenReturn(StoreResult.generalError("database unavailable"));

        var entry = manager.getActiveCredential(PARTICIPANT_CONTEXT_ID);
        assertThat(entry).isFailed().detail().contains("database unavailable");

        verify(store, times(2)).queryForUpdate(any());
        verifyNoMoreInteractions(store, generator, participantContextService);
    }

    @Test
    void getActiveCredential_whenFull_shouldCreateNew() {
        when(store.queryForUpdate(any())).thenReturn(StoreResult.success(List.of(createVerifiableCredentialResource()
                .metadata(CURRENT_INDEX, MIN_BITSTRING_SIZE) // credential is saturated
                .metadata(PUBLIC_URL, "http://bar.com/quizz")
                .metadata(IS_ACTIVE, true)
                .build())));
        when(store.create(any())).thenReturn(StoreResult.success());
        when(store.update(any())).thenReturn(StoreResult.success());

        var entry = manager.getActiveCredential(PARTICIPANT_CONTEXT_ID);
        assertThat(entry).isSucceeded()
                .satisfies(e -> {
                    assertThat(e.credentialUrl()).isNotNull();
                    assertThat(e.statusListIndex()).isEqualTo(0);
                });

        verify(store, times(2)).queryForUpdate(any());
        verify(store).create(hasParticipantId(PARTICIPANT_CONTEXT_ID));
        verify(store).update(hasParticipantId(PARTICIPANT_CONTEXT_ID));
        verify(participantContextService).getParticipantContext(PARTICIPANT_CONTEXT_ID);
        verify(generator).signCredential(eq(PARTICIPANT_CONTEXT_ID), any(), eq(CredentialFormat.VC2_0_JOSE));
        verifyNoMoreInteractions(store, generator, participantContextService);
    }

    @Test
    void getActiveCredential_whenNotActive_shouldCreateNew() {
        when(store.queryForUpdate(any())).thenReturn(StoreResult.success(List.of(createVerifiableCredentialResource()
                .metadata(CURRENT_INDEX, 42)
                .metadata(PUBLIC_URL, "http://bar.com/quizz")
                .metadata(IS_ACTIVE, false) // triggers creation
                .build())));
        when(store.create(any())).thenReturn(StoreResult.success());
        when(store.update(any())).thenReturn(StoreResult.success());

        var entry = manager.getActiveCredential(PARTICIPANT_CONTEXT_ID);
        assertThat(entry).isSucceeded()
                .satisfies(e -> {
                    assertThat(e.credentialUrl()).isNotNull();
                    assertThat(e.statusListIndex()).isEqualTo(0);
                });

        verify(store, times(2)).queryForUpdate(any());
        verify(store).create(hasParticipantId(PARTICIPANT_CONTEXT_ID));
        verify(store).update(hasParticipantId(PARTICIPANT_CONTEXT_ID));
        verify(participantContextService).getParticipantContext(PARTICIPANT_CONTEXT_ID);
        verify(generator).signCredential(eq(PARTICIPANT_CONTEXT_ID), any(), eq(CredentialFormat.VC2_0_JOSE));
        verifyNoMoreInteractions(store, generator, participantContextService);
    }

    @Test
    void getActiveCredential_whenCreateNew_signingFails() {
        when(store.queryForUpdate(any())).thenReturn(StoreResult.success(List.of()));
        when(generator.signCredential(anyString(), any(), any()))
                .thenReturn(Result.failure("signing failure"));
        var entry = manager.getActiveCredential(PARTICIPANT_CONTEXT_ID);
        assertThat(entry).isFailed()
                .detail().contains("signing failure");

        verify(store, times(2)).queryForUpdate(any());
        verify(participantContextService).getParticipantContext(PARTICIPANT_CONTEXT_ID);
        verify(generator).signCredential(eq(PARTICIPANT_CONTEXT_ID), any(), eq(CredentialFormat.VC2_0_JOSE));
        verifyNoMoreInteractions(store, generator, participantContextService);
    }

    @Test
    void getActiveCredential_whenCreateNew_participantNotFound() {
        when(participantContextService.getParticipantContext(PARTICIPANT_CONTEXT_ID))
                .thenReturn(ServiceResult.notFound("foobar"));
        when(store.queryForUpdate(any())).thenReturn(StoreResult.success(List.of()));
        var entry = manager.getActiveCredential(PARTICIPANT_CONTEXT_ID);
        assertThat(entry).isFailed()
                .detail().contains("foobar");

        verify(store, times(2)).queryForUpdate(any());
        verify(participantContextService).getParticipantContext(PARTICIPANT_CONTEXT_ID);
        verifyNoMoreInteractions(store, generator, participantContextService);
    }

    @Test
    void incrementIndex() {
        var entry = new BitstringStatusListCredentialEntry(BITSTRING_STATUS_LIST, 42, createVerifiableCredentialResource().build(), "http://bar.com/quizz");
        when(store.update(any())).thenReturn(StoreResult.success());
        assertThat(manager.incrementIndex(entry)).isSucceeded();

        verify(store).update(any());
        verifyNoMoreInteractions(store, generator, participantContextService);
    }

    @Test
    void incrementIndex_whenNotExist_shouldInsert() {
        when(store.update(any())).thenReturn(StoreResult.notFound("foo"));
        var entry = new BitstringStatusListCredentialEntry(BITSTRING_STATUS_LIST, 42, createVerifiableCredentialResource().build(), "http://bar.com/quizz");
        when(store.create(any())).thenReturn(StoreResult.success());
        assertThat(manager.incrementIndex(entry)).isSucceeded();

        verify(store).update(any());
        verify(store).create(any());
        verifyNoMoreInteractions(store, generator, participantContextService);
    }

    @Test
    void incrementIndex_whenUpdateFails() {
        when(store.update(any())).thenReturn(StoreResult.generalError("foo"));
        var entry = new BitstringStatusListCredentialEntry(BITSTRING_STATUS_LIST, 42, createVerifiableCredentialResource().build(), "http://bar.com/quizz");
        assertThat(manager.incrementIndex(entry)).isFailed().detail().contains("foo");

        verify(store).update(any());
        verifyNoMoreInteractions(store, generator, participantContextService);
    }

    @Test
    void incrementIndex_whenStoreFails_shouldReturnFailure() {
        when(store.update(any())).thenReturn(StoreResult.notFound("foo"));
        when(store.create(any())).thenReturn(StoreResult.generalError("bar"));
        var entry = new BitstringStatusListCredentialEntry(BITSTRING_STATUS_LIST, 42, createVerifiableCredentialResource().build(), "http://bar.com/quizz");
        assertThat(manager.incrementIndex(entry)).isFailed().detail().contains("bar");

        verify(store).update(any());
        verify(store).create(any());
        verifyNoMoreInteractions(store, generator, participantContextService);
    }

    @Test
    void getActiveCredential_whenCreateNew_shouldUseConfiguredLengthAndValidity() {
        when(store.queryForUpdate(any())).thenReturn(StoreResult.success(List.of()));
        when(store.update(any())).thenReturn(StoreResult.success());
        when(store.create(any())).thenReturn(StoreResult.success());

        var entry = manager.getActiveCredential(PARTICIPANT_CONTEXT_ID);

        assertThat(entry).isSucceeded().satisfies(e -> {
            var credential = e.statusListCredential().getVerifiableCredential().credential();
            assertThat(credential.getIssuanceDate()).isEqualTo(NOW);
            assertThat(credential.getExpirationDate()).isEqualTo(NOW.plus(VALIDITY));
            assertThat(bitString(e.statusListCredential()).length()).isEqualTo(MIN_BITSTRING_SIZE);
            assertThat(e.statusListCredential().getMetadata()).containsEntry(BITSTRING_SIZE, MIN_BITSTRING_SIZE);
        });
    }

    @Test
    void getActiveCredential_whenCreateNew_shouldCreateVcdm20Credential() {
        when(store.queryForUpdate(any())).thenReturn(StoreResult.success(List.of()));
        when(store.update(any())).thenReturn(StoreResult.success());
        when(store.create(any())).thenReturn(StoreResult.success());

        var entry = manager.getActiveCredential(PARTICIPANT_CONTEXT_ID);

        assertThat(entry).isSucceeded().satisfies(e -> {
            assertThat(e.statusListCredential().getVerifiableCredential().format()).isEqualTo(CredentialFormat.VC2_0_JOSE);
            var credential = e.statusListCredential().getVerifiableCredential().credential();
            assertThat(credential.getContext()).containsExactly("https://www.w3.org/ns/credentials/v2");
            assertThat(credential.getDataModelVersion()).isEqualTo(DataModelVersion.V_2_0);
            assertThat(credential.getType()).containsExactly("VerifiableCredential", "BitstringStatusListCredential");
        });
        verify(generator).signCredential(eq(PARTICIPANT_CONTEXT_ID), argThat(credential -> credential.getDataModelVersion() == DataModelVersion.V_2_0),
                eq(CredentialFormat.VC2_0_JOSE));
    }

    @Test
    void getActiveCredential_whenCreateNew_withStatusList2021_shouldCreateVcdm11Credential() {
        when(store.queryForUpdate(any())).thenReturn(StoreResult.success(List.of()));
        when(store.update(any())).thenReturn(StoreResult.success());
        when(store.create(any())).thenReturn(StoreResult.success());

        var entry = manager(STATUS_LIST_2021).getActiveCredential(PARTICIPANT_CONTEXT_ID);

        assertThat(entry).isSucceeded().satisfies(e -> {
            assertThat(e.statusListCredential().getVerifiableCredential().format()).isEqualTo(VC1_0_JWT);
            var credential = e.statusListCredential().getVerifiableCredential().credential();
            assertThat(credential.getContext()).containsExactly("https://www.w3.org/2018/credentials/v1", "https://w3id.org/vc/status-list/2021/v1");
            assertThat(credential.getDataModelVersion()).isEqualTo(DataModelVersion.V_1_1);
            assertThat(credential.getType()).containsExactly("VerifiableCredential", "StatusList2021Credential");
            assertThat(credential.getCredentialSubject()).singleElement().satisfies(subject -> {
                assertThat(subject.getClaims()).containsEntry("type", "StatusList2021").containsEntry("statusPurpose", "revocation");
                // StatusList2021 requires a base64 encoded list, without the multibase prefix of the Bitstring Status List
                assertThat(subject.getClaims().get("encodedList").toString()).startsWith("H4sI");
            });
            assertThat(bitString(e.statusListCredential()).length()).isEqualTo(MIN_BITSTRING_SIZE);
        });
        verify(store, times(2)).queryForUpdate(isStatusListQuery("StatusList2021"));
        verify(generator).signCredential(eq(PARTICIPANT_CONTEXT_ID), any(), eq(VC1_0_JWT));
    }

    @ParameterizedTest
    @CsvSource({ "STATUS_LIST_2021, StatusList2021, StatusList2021Entry", "BITSTRING_STATUS_LIST, BitstringStatusList, BitstringStatusListEntry" })
    void getActiveCredential_shouldCreateStatusEntryOfItsType(StatusListType type, String subjectType, String entryType) {
        var statusList = statusListResource(type, MIN_BITSTRING_SIZE, NOW.plus(Duration.ofDays(265)))
                .metadata(BITSTRING_SIZE, MIN_BITSTRING_SIZE)
                .metadata(CURRENT_INDEX, 42)
                .metadata(PUBLIC_URL, "http://bar.com/quizz")
                .metadata(IS_ACTIVE, true)
                .build();
        when(store.queryForUpdate(isStatusListQuery(subjectType))).thenReturn(StoreResult.success(List.of(statusList)));

        var entry = manager(type).getActiveCredential(PARTICIPANT_CONTEXT_ID);

        assertThat(entry).isSucceeded().satisfies(e -> {
            assertThat(e.statusListCredential()).isSameAs(statusList);
            var status = e.createCredentialStatus();
            assertThat(status.type()).isEqualTo(entryType);
            // both status lists require the index as a base-10 string
            assertThat(status.additionalProperties())
                    .containsEntry("statusListIndex", "42")
                    .containsEntry("statusPurpose", "revocation")
                    .containsEntry("statusListCredential", "http://bar.com/quizz");
        });
    }

    @Test
    void getActiveCredential_whenShorterThanConfigured_shouldExtendAndRenew() {
        var shortList = statusListResource(16 * 1024, 3, 16 * 1024 - 1)
                .metadata(BITSTRING_SIZE, 16 * 1024)
                .metadata(CURRENT_INDEX, 100)
                .metadata(PUBLIC_URL, "http://bar.com/short")
                .metadata(IS_ACTIVE, true)
                .build();
        var originalCredential = shortList.getVerifiableCredential().credential();
        when(store.queryForUpdate(any())).thenReturn(StoreResult.success(List.of(shortList)));
        when(store.update(any())).thenReturn(StoreResult.success());

        var entry = manager.getActiveCredential(PARTICIPANT_CONTEXT_ID);

        assertThat(entry).isSucceeded().satisfies(e -> {
            assertThat(e.statusListIndex()).isEqualTo(100);
            assertThat(e.credentialUrl()).isEqualTo("http://bar.com/short");
            var extended = e.statusListCredential();
            assertThat(extended.getId()).isEqualTo(shortList.getId());
            assertThat(extended.getMetadata()).containsEntry(BITSTRING_SIZE, MIN_BITSTRING_SIZE).containsEntry(CURRENT_INDEX, 100);
            var bitString = bitString(extended);
            assertThat(bitString.length()).isEqualTo(MIN_BITSTRING_SIZE);
            // the existing entries keep their indexes
            assertThat(IntStream.range(0, bitString.length()).filter(bitString::get)).containsExactly(3, 16 * 1024 - 1);
            var credential = extended.getVerifiableCredential().credential();
            assertThat(credential.getIssuanceDate()).isEqualTo(NOW);
            assertThat(credential.getExpirationDate()).isEqualTo(NOW.plus(VALIDITY));
            // the status list credential keeps its identity, so that the credentials that reference it still find it
            assertThat(credential.getId()).isEqualTo(originalCredential.getId());
            assertThat(credential.getType()).containsExactlyElementsOf(originalCredential.getType());
            assertThat(credential.getIssuer()).isEqualTo(originalCredential.getIssuer());
            assertThat(credential.getCredentialSubject()).singleElement().satisfies(subject -> {
                assertThat(subject.getId()).isEqualTo("status-list-subject-id");
                assertThat(subject.getClaims()).containsEntry("type", "BitstringStatusList").containsEntry("statusPurpose", "revocation");
            });
        });
        verify(generator).signCredential(eq(PARTICIPANT_CONTEXT_ID), any(), eq(CredentialFormat.VC1_0_JWT));
        var inOrder = inOrder(publisher, store);
        inOrder.verify(publisher).publish(argThat(r -> MIN_BITSTRING_SIZE == (int) r.getMetadata().get(BITSTRING_SIZE)));
        inOrder.verify(store).update(argThat(r -> MIN_BITSTRING_SIZE == (int) r.getMetadata().get(BITSTRING_SIZE)));
        verify(store, never()).create(any());
    }

    @Test
    void getActiveCredential_whenExtended_andPublishingFails_shouldUseShorterList() {
        var shortList = statusListResource(16 * 1024)
                .metadata(BITSTRING_SIZE, 16 * 1024)
                .metadata(CURRENT_INDEX, 100)
                .metadata(IS_ACTIVE, true)
                .build();
        var originalCredential = shortList.getVerifiableCredential();
        when(store.queryForUpdate(any())).thenReturn(StoreResult.success(List.of(shortList)));
        when(publisher.publish(any())).thenReturn(Result.failure("publication target unavailable"));

        var entry = manager.getActiveCredential(PARTICIPANT_CONTEXT_ID);

        assertThat(entry).isSucceeded().satisfies(e -> {
            assertThat(e.statusListCredential().getVerifiableCredential()).isSameAs(originalCredential);
            assertThat(e.statusListCredential().getMetadata()).containsEntry(BITSTRING_SIZE, 16 * 1024);
        });
        verify(store, never()).update(any());
        verify(monitor).warning(contains("publication target unavailable"));
    }

    @Test
    void getActiveCredential_whenFullButShorterThanConfigured_shouldExtendAndUseItAgain() {
        var fullShortList = statusListResource(16 * 1024)
                .metadata(BITSTRING_SIZE, 16 * 1024)
                .metadata(CURRENT_INDEX, 16 * 1024)
                .metadata(PUBLIC_URL, "http://bar.com/full")
                .metadata(IS_ACTIVE, true)
                .build();
        when(store.queryForUpdate(any())).thenReturn(StoreResult.success(List.of(fullShortList)));
        when(store.update(any())).thenReturn(StoreResult.success());

        var entry = manager.getActiveCredential(PARTICIPANT_CONTEXT_ID);

        // the extension adds new entries to it, which are used before a new status list is created
        assertThat(entry).isSucceeded().satisfies(e -> {
            assertThat(e.statusListCredential().getId()).isEqualTo(fullShortList.getId());
            assertThat(e.statusListIndex()).isEqualTo(16 * 1024);
        });
        verify(store, never()).create(any());
    }

    @Test
    void getActiveCredential_whenExtendingFails_shouldUseShorterList() {
        var shortList = statusListResource(16 * 1024)
                .metadata(BITSTRING_SIZE, 16 * 1024)
                .metadata(CURRENT_INDEX, 100)
                .metadata(PUBLIC_URL, "http://bar.com/short")
                .metadata(IS_ACTIVE, true)
                .build();
        var originalCredential = shortList.getVerifiableCredential();
        when(store.queryForUpdate(any())).thenReturn(StoreResult.success(List.of(shortList)));
        when(generator.signCredential(any(), any(), any())).thenReturn(Result.failure("vault unavailable"));

        var entry = manager.getActiveCredential(PARTICIPANT_CONTEXT_ID);

        assertThat(entry).isSucceeded().satisfies(e -> {
            assertThat(e.statusListIndex()).isEqualTo(100);
            assertThat(e.statusListCredential().getVerifiableCredential()).isSameAs(originalCredential);
            assertThat(e.statusListCredential().getMetadata()).containsEntry(BITSTRING_SIZE, 16 * 1024);
        });
        verify(store, never()).update(any());
        verify(monitor).warning(contains("vault unavailable"));
    }

    @Test
    void getActiveCredential_withoutLengthInMetadata_shouldTakeItFromBitstring() {
        var fullList = statusListResource(MIN_BITSTRING_SIZE)
                .metadata(CURRENT_INDEX, MIN_BITSTRING_SIZE)
                .metadata(PUBLIC_URL, "http://bar.com/full")
                .metadata(IS_ACTIVE, true)
                .build();
        when(store.queryForUpdate(any())).thenReturn(StoreResult.success(List.of(fullList)));
        when(store.create(any())).thenReturn(StoreResult.success());
        when(store.update(any())).thenReturn(StoreResult.success());

        var entry = manager.getActiveCredential(PARTICIPANT_CONTEXT_ID);

        // its bitstring has the configured length, so it is neither extended nor used, but full
        assertThat(entry).isSucceeded().satisfies(e -> assertThat(e.statusListCredential().getId()).isNotEqualTo(fullList.getId()));
        verify(store).create(any());
        verify(generator, times(1)).signCredential(any(), any(), any());
    }

    @Test
    void getActiveCredential_whenShorterWithoutLengthInMetadata_shouldExtend() {
        var shortList = statusListResource(16 * 1024)
                .metadata(CURRENT_INDEX, 100)
                .metadata(PUBLIC_URL, "http://bar.com/short")
                .metadata(IS_ACTIVE, true)
                .build();
        when(store.queryForUpdate(any())).thenReturn(StoreResult.success(List.of(shortList)));
        when(store.update(any())).thenReturn(StoreResult.success());

        var entry = manager.getActiveCredential(PARTICIPANT_CONTEXT_ID);

        // its length is taken from its bitstring, not from the configuration, which it would not match
        assertThat(entry).isSucceeded().satisfies(e -> {
            assertThat(bitString(e.statusListCredential()).length()).isEqualTo(MIN_BITSTRING_SIZE);
            assertThat(e.statusListCredential().getMetadata()).containsEntry(BITSTRING_SIZE, MIN_BITSTRING_SIZE);
        });
    }

    @Test
    void renew_shouldRenewValidityAndKeepEntries() {
        var statusList = statusListResource(MIN_BITSTRING_SIZE, 7)
                .metadata(BITSTRING_SIZE, MIN_BITSTRING_SIZE)
                .metadata(CURRENT_INDEX, 42)
                .build();

        var result = manager.renew(statusList);

        assertThat(result).isSucceeded().satisfies(renewed -> {
            var credential = renewed.getVerifiableCredential().credential();
            assertThat(credential.getIssuanceDate()).isEqualTo(NOW);
            assertThat(credential.getExpirationDate()).isEqualTo(NOW.plus(VALIDITY));
            var bitString = bitString(renewed);
            assertThat(bitString.length()).isEqualTo(MIN_BITSTRING_SIZE);
            assertThat(IntStream.range(0, bitString.length()).filter(bitString::get)).containsExactly(7);
            assertThat(renewed.getMetadata()).containsEntry(CURRENT_INDEX, 42).containsEntry(BITSTRING_SIZE, MIN_BITSTRING_SIZE);
        });
        verify(generator).signCredential(eq(PARTICIPANT_CONTEXT_ID), any(), eq(CredentialFormat.VC1_0_JWT));
        // the caller stores it
        verifyNoInteractions(store);
    }

    @ParameterizedTest
    @EnumSource(value = CredentialFormat.class, names = { "VC1_0_JWT", "VC2_0_JOSE" })
    void renew_shouldKeepFormat(CredentialFormat format) {
        var statusList = statusListResource(format, MIN_BITSTRING_SIZE, NOW.plus(Duration.ofDays(265)))
                .metadata(BITSTRING_SIZE, MIN_BITSTRING_SIZE)
                .build();

        var result = manager.renew(statusList);

        // a status list credential that was issued before VCDM 2.0 was used for new ones stays in its format
        assertThat(result).isSucceeded().satisfies(renewed -> assertThat(renewed.getVerifiableCredential().format()).isEqualTo(format));
        verify(generator).signCredential(eq(PARTICIPANT_CONTEXT_ID), any(), eq(format));
    }

    @Test
    void renew_whenStatusList2021_shouldKeepItsEncoding() {
        var statusList = statusListResource(STATUS_LIST_2021, MIN_BITSTRING_SIZE, NOW.plus(Duration.ofDays(265)), 7)
                .metadata(BITSTRING_SIZE, MIN_BITSTRING_SIZE)
                .build();

        var result = manager.renew(statusList);

        assertThat(result).isSucceeded().satisfies(renewed -> {
            var encodedList = renewed.getVerifiableCredential().credential().getCredentialSubject().get(0).getClaims().get("encodedList");
            assertThat(encodedList.toString()).startsWith("H4sI");
            var bitString = bitString(renewed);
            assertThat(IntStream.range(0, bitString.length()).filter(bitString::get)).containsExactly(7);
        });
        verify(generator).signCredential(eq(PARTICIPANT_CONTEXT_ID), any(), eq(VC1_0_JWT));
    }

    @Test
    void renew_whenVerifiableCredentialTypeIsMissing_shouldAddIt() {
        var statusList = statusListResource(MIN_BITSTRING_SIZE).metadata(BITSTRING_SIZE, MIN_BITSTRING_SIZE).build();
        // like the status list credentials that were created before
        statusList.getVerifiableCredential().credential().getType().remove("VerifiableCredential");

        var result = manager.renew(statusList);

        assertThat(result).isSucceeded().satisfies(renewed -> assertThat(renewed.getVerifiableCredential().credential().getType())
                .containsExactly("VerifiableCredential", "BitstringStatusListCredential"));
        verify(generator).signCredential(eq(PARTICIPANT_CONTEXT_ID),
                argThat(credential -> credential.getType().equals(List.of("VerifiableCredential", "BitstringStatusListCredential"))), any());
    }

    @Test
    void renew_whenSigningFails_shouldLeaveResourceUnchanged() {
        var statusList = statusListResource(16 * 1024, 7)
                .metadata(BITSTRING_SIZE, 16 * 1024)
                .build();
        var originalCredential = statusList.getVerifiableCredential();
        when(generator.signCredential(any(), any(), any())).thenReturn(Result.failure("vault unavailable"));

        var result = manager.renew(statusList);

        assertThat(result).isFailed().detail().contains("vault unavailable");
        assertThat(statusList.getVerifiableCredential()).isSameAs(originalCredential);
        assertThat(statusList.getVerifiableCredential().credential().getExpirationDate()).isEqualTo(NOW.plus(Duration.ofDays(265)));
        assertThat(bitString(statusList).length()).isEqualTo(16 * 1024);
        assertThat(statusList.getMetadata()).containsEntry(BITSTRING_SIZE, 16 * 1024);
    }

    @Test
    void renewExpiring_shouldRenewCredentialsThatExpireWithinThreshold() {
        var expiring = statusListResource(MIN_BITSTRING_SIZE, NOW.plus(Duration.ofDays(10))).metadata(BITSTRING_SIZE, MIN_BITSTRING_SIZE).build();
        var notExpiring = statusListResource(MIN_BITSTRING_SIZE, NOW.plus(Duration.ofDays(200))).metadata(BITSTRING_SIZE, MIN_BITSTRING_SIZE).build();
        when(store.query(isStatusListQuery("BitstringStatusList"))).thenReturn(StoreResult.success(List.of(expiring, notExpiring)));
        when(store.queryForUpdate(isIdQuery(expiring.getId()))).thenReturn(StoreResult.success(List.of(expiring)));
        when(store.update(any())).thenReturn(StoreResult.success());

        manager.renewExpiring(Duration.ofDays(30));

        var inOrder = inOrder(publisher, store);
        inOrder.verify(publisher).publish(argThat(r -> r.getId().equals(expiring.getId()) &&
                r.getVerifiableCredential().credential().getExpirationDate().equals(NOW.plus(VALIDITY))));
        inOrder.verify(store).update(argThat(r -> r.getId().equals(expiring.getId()) &&
                r.getVerifiableCredential().credential().getExpirationDate().equals(NOW.plus(VALIDITY))));
        verify(store, never()).queryForUpdate(isIdQuery(notExpiring.getId()));
        verify(generator, times(1)).signCredential(any(), any(), any());
        verify(publisher, times(1)).publish(any());
    }

    @Test
    void renewExpiring_whenPublishingFails_shouldNotStoreIt() {
        var expiring = statusListResource(MIN_BITSTRING_SIZE, NOW.plus(Duration.ofDays(10))).metadata(BITSTRING_SIZE, MIN_BITSTRING_SIZE).build();
        when(store.query(isStatusListQuery("BitstringStatusList"))).thenReturn(StoreResult.success(List.of(expiring)));
        when(store.queryForUpdate(isIdQuery(expiring.getId()))).thenReturn(StoreResult.success(List.of(expiring)));
        when(publisher.publish(any())).thenReturn(Result.failure("publication target unavailable"));

        manager.renewExpiring(Duration.ofDays(30));

        // renewed again on the next call
        verify(store, never()).update(any());
        verify(monitor).warning(contains("publication target unavailable"));
    }

    @Test
    void renewExpiring_whenRenewedByAnotherRuntimeMeanwhile_shouldSkipIt() {
        var expiring = statusListResource(MIN_BITSTRING_SIZE, NOW.plus(Duration.ofDays(10))).metadata(BITSTRING_SIZE, MIN_BITSTRING_SIZE).build();
        var renewedMeanwhile = statusListResource(MIN_BITSTRING_SIZE, NOW.plus(Duration.ofDays(365))).id(expiring.getId()).metadata(BITSTRING_SIZE, MIN_BITSTRING_SIZE).build();
        when(store.query(isStatusListQuery("BitstringStatusList"))).thenReturn(StoreResult.success(List.of(expiring)));
        when(store.queryForUpdate(isIdQuery(expiring.getId()))).thenReturn(StoreResult.success(List.of(renewedMeanwhile)));

        manager.renewExpiring(Duration.ofDays(30));

        verify(generator, never()).signCredential(any(), any(), any());
        verify(store, never()).update(any());
    }

    @Test
    void renewExpiring_whenOneFails_shouldRenewTheOthers() {
        var failing = statusListResource(MIN_BITSTRING_SIZE, NOW.plus(Duration.ofDays(10))).metadata(BITSTRING_SIZE, MIN_BITSTRING_SIZE).build();
        var other = statusListResource(MIN_BITSTRING_SIZE, NOW.plus(Duration.ofDays(10))).metadata(BITSTRING_SIZE, MIN_BITSTRING_SIZE).build();
        when(store.query(isStatusListQuery("BitstringStatusList"))).thenReturn(StoreResult.success(List.of(failing, other)));
        when(store.queryForUpdate(isIdQuery(failing.getId()))).thenThrow(new EdcPersistenceException("database unavailable"));
        when(store.queryForUpdate(isIdQuery(other.getId()))).thenReturn(StoreResult.success(List.of(other)));
        when(store.update(any())).thenReturn(StoreResult.success());

        manager.renewExpiring(Duration.ofDays(30));

        verify(store).update(argThat(r -> r.getId().equals(other.getId())));
        verify(monitor).warning(contains(failing.getId()), any(EdcPersistenceException.class));
    }

    @Test
    void renewExpiring_shouldRenewStatusListsOfAllTypes() {
        var statusList2021 = statusListResource(STATUS_LIST_2021, MIN_BITSTRING_SIZE, NOW.plus(Duration.ofDays(10))).metadata(BITSTRING_SIZE, MIN_BITSTRING_SIZE).build();
        var bitstringStatusList = statusListResource(BITSTRING_STATUS_LIST, MIN_BITSTRING_SIZE, NOW.plus(Duration.ofDays(10))).metadata(BITSTRING_SIZE, MIN_BITSTRING_SIZE).build();
        when(store.query(isStatusListQuery("StatusList2021"))).thenReturn(StoreResult.success(List.of(statusList2021)));
        when(store.query(isStatusListQuery("BitstringStatusList"))).thenReturn(StoreResult.success(List.of(bitstringStatusList)));
        when(store.queryForUpdate(isIdQuery(statusList2021.getId()))).thenReturn(StoreResult.success(List.of(statusList2021)));
        when(store.queryForUpdate(isIdQuery(bitstringStatusList.getId()))).thenReturn(StoreResult.success(List.of(bitstringStatusList)));
        when(store.update(any())).thenReturn(StoreResult.success());

        manager.renewExpiring(Duration.ofDays(30));

        verify(store).update(argThat(r -> r.getId().equals(statusList2021.getId())));
        verify(store).update(argThat(r -> r.getId().equals(bitstringStatusList.getId())));
        verify(generator).signCredential(eq(PARTICIPANT_CONTEXT_ID), any(), eq(VC1_0_JWT));
        verify(generator).signCredential(eq(PARTICIPANT_CONTEXT_ID), any(), eq(VC2_0_JOSE));
    }

    private BitstringStatusListManager manager(StatusListType type) {
        return new BitstringStatusListManager(type, store, new NoopTransactionContext(), generator, participantContextService, publisher,
                new BitstringStatusListConfiguration(MIN_BITSTRING_SIZE, VALIDITY.toSeconds(), DEFAULT_RENEWAL_PERIOD_SECONDS, DEFAULT_RENEWAL_THRESHOLD_SECONDS),
                Clock.fixed(NOW, ZoneOffset.UTC), monitor);
    }

    private QuerySpec isStatusListQuery(String subjectType) {
        return argThat(query -> query != null && query.getFilterExpression().stream()
                .anyMatch(criterion -> "verifiableCredential.credential.credentialSubject.type".equals(criterion.getOperandLeft()) &&
                        subjectType.equals(criterion.getOperandRight())));
    }

    private QuerySpec isIdQuery(String id) {
        return argThat(query -> query != null && query.getFilterExpression().stream()
                .anyMatch(criterion -> "id".equals(criterion.getOperandLeft()) && id.equals(criterion.getOperandRight())));
    }

    private VerifiableCredentialResource hasParticipantId(String participantContextId) {
        return argThat(res -> res.getParticipantContextId().equals(participantContextId));
    }

    private VerifiableCredentialResource.Builder createVerifiableCredentialResource() {
        return statusListResource(MIN_BITSTRING_SIZE)
                .metadata(BITSTRING_SIZE, MIN_BITSTRING_SIZE);
    }

    /**
     * A status list credential with a bitstring of the given length, in which the given indexes are set.
     */
    private VerifiableCredentialResource.Builder statusListResource(int bitstringSize, int... setIndexes) {
        return statusListResource(bitstringSize, NOW.plus(Duration.ofDays(265)), setIndexes);
    }

    private VerifiableCredentialResource.Builder statusListResource(int bitstringSize, Instant expirationDate, int... setIndexes) {
        return statusListResource(CredentialFormat.VC1_0_JWT, bitstringSize, expirationDate, setIndexes);
    }

    private VerifiableCredentialResource.Builder statusListResource(CredentialFormat format, int bitstringSize, Instant expirationDate, int... setIndexes) {
        return statusListResource(BITSTRING_STATUS_LIST, format, bitstringSize, expirationDate, setIndexes);
    }

    private VerifiableCredentialResource.Builder statusListResource(StatusListType type, int bitstringSize, Instant expirationDate, int... setIndexes) {
        return statusListResource(type, type.format(), bitstringSize, expirationDate, setIndexes);
    }

    private VerifiableCredentialResource.Builder statusListResource(StatusListType type, CredentialFormat format, int bitstringSize, Instant expirationDate, int... setIndexes) {
        var bitString = BitString.Builder.newInstance().size(bitstringSize).build();
        for (var index : setIndexes) {
            bitString.set(index, true);
        }
        var credential = VerifiableCredential.Builder.newInstance()
                .id("status-list-credential-id")
                .type("VerifiableCredential")
                .type(type.credentialType())
                .issuer(new Issuer("did:web:issuer"))
                .issuanceDate(NOW.minus(Duration.ofDays(100)))
                .expirationDate(expirationDate)
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id("status-list-subject-id")
                        .claim("type", type.subjectType())
                        .claim("statusPurpose", "revocation")
                        .claim("encodedList", type.encode(bitString).getContent())
                        .build())
                .build();
        return VerifiableCredentialResource.Builder.newStatusList()
                .id(UUID.randomUUID().toString())
                .participantContextId(PARTICIPANT_CONTEXT_ID)
                .issuerId("issuer-id")
                .holderId("holder-id")
                .credential(new VerifiableCredentialContainer("raw-token", format, credential));
    }

    private BitString bitString(VerifiableCredentialResource statusListCredential) {
        var encodedList = statusListCredential.getVerifiableCredential().credential().getCredentialSubject().get(0).getClaims().get("encodedList");
        return BitString.Parser.newInstance().parse(encodedList.toString()).getContent();
    }

}
