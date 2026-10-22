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

package org.eclipse.edc.identityhub.core.services.verifiablecredential;

import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import org.assertj.core.api.Assertions;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderCredentialRequest;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState;
import org.eclipse.edc.identityhub.spi.credential.request.store.HolderCredentialRequestStore;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.generator.CredentialWriteRequest;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialProfile;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.validation.CredentialFormatValidator;
import org.eclipse.edc.identityhub.transaction.TrackingTransactionContext;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.keys.spi.PublicKeyResolver;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.token.TokenValidationServiceImpl;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.VC_PREFIX_V2;
import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.W3C_CREDENTIALS_URL;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.ISSUED;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.REQUESTED;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.REQUESTING;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.VerifiableCredentialTestUtil.buildSignedJwt;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})
class CredentialWriterImplTest {

    public static final String TEST_CREDENTIAL_TYPE = "TestCredential";
    public static final String TEST_CREDENTIAL_FORMAT = CredentialFormat.VC1_0_JWT.toString();
    private static final String PARTICIPANT_ID = "participant";
    private static final String ISSUER_DID = "did:web:issuer";
    private static final String HOLDER_DID = "did:web:holder";
    private static final String VCDM11_JWT = unsignedJwt(Map.of("vc", Map.of("@context", List.of(W3C_CREDENTIALS_URL))));
    private static final String VCDM20_JWT = unsignedJwt(Map.of("@context", List.of(VC_PREFIX_V2), "type", List.of("VerifiableCredential")));
    private static final String VCDM11_LD = "{\"@context\": [\"%s\"], \"type\": [\"VerifiableCredential\"]}".formatted(W3C_CREDENTIALS_URL);
    private final CredentialStore credentialStore = mock();
    private final TypeTransformerRegistry credentialTransformerRegistry = mock();
    private final HolderCredentialRequestStore holderCredentialRequestStore = mock();
    private final TokenValidationService tokenValidationService = mock();
    private final DidPublicKeyResolver publicKeyResolver = mock();
    private final Monitor monitor = mock();
    private final TrackingTransactionContext transactionContext = new TrackingTransactionContext();
    private final CredentialWriterImpl credentialWriter = new CredentialWriterImpl(credentialStore, credentialTransformerRegistry, transactionContext,
            JacksonJsonLd.createObjectMapper(), holderCredentialRequestStore, tokenValidationService, publicKeyResolver, new CredentialFormatValidator(), monitor, Clock.systemUTC(), false);

    @BeforeEach
    void setUp() {
        when(tokenValidationService.validate(anyString(), any(PublicKeyResolver.class), anyList())).thenReturn(Result.success(ClaimToken.Builder.newInstance().build()));
        when(credentialStore.queryForUpdate(any())).thenReturn(StoreResult.success(List.of()));
        when(holderCredentialRequestStore.save(any())).thenReturn(StoreResult.success());
        storeHolds(HolderCredentialRequest.Builder.newInstance()
                .issuerDid(ISSUER_DID)
                .requestedCredential("test-id", TEST_CREDENTIAL_TYPE, TEST_CREDENTIAL_FORMAT)
                .state(REQUESTED.code())
                .participantContextId(PARTICIPANT_ID)
                .build());
    }

    @Test
    @DisplayName("CS-STOR-01: a valid CredentialMessage stores the credential and completes the request")
    void write() {
        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class)))
                .thenReturn(Result.success(createCredential().build()));

        when(credentialStore.create(any())).thenReturn(StoreResult.success());

        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(new CredentialWriteRequest(VCDM11_JWT, TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);
        assertThat(result).isSucceeded();
        verify(holderCredentialRequestStore).save(argThat(request -> request.getIssuerPid() != null));
    }

    @Test
    void write_saveRequestFails_expectFailureAndRollback() {
        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class)))
                .thenReturn(Result.success(createCredential().build()));
        when(credentialStore.create(any())).thenReturn(StoreResult.success());
        var saveTransaction = new AtomicInteger();
        when(holderCredentialRequestStore.save(any())).thenAnswer(i -> {
            saveTransaction.set(transactionContext.currentTransaction());
            return StoreResult.generalError("save failed");
        });

        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(new CredentialWriteRequest(VCDM11_JWT, TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);

        assertThat(result).isFailed().detail().isEqualTo("save failed");
        // the credential was stored already, and must not be committed while the request stays REQUESTED
        verify(credentialStore).create(any());
        Assertions.assertThat(saveTransaction.get()).isPositive();
        Assertions.assertThat(transactionContext.isRolledBack(saveTransaction.get())).isTrue();
    }

    @Test
    void write_invalidProof_shouldNotLeaseRequest() {
        when(tokenValidationService.validate(anyString(), any(PublicKeyResolver.class), anyList())).thenReturn(Result.failure("invalid signature"));

        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(new CredentialWriteRequest(VCDM11_JWT, TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);

        assertThat(result).isFailed().detail().contains("Could not verify the credential's proof").contains("invalid signature");
        verify(holderCredentialRequestStore, never()).findByIdAndLease(anyString());
        verifyNoInteractions(credentialStore);
    }

    @Test
    void write_signedByKeyOfIssuer_shouldStore() {
        var result = writeSigned(claims().issuer(ISSUER_DID), ISSUER_DID + "#key-1");

        assertThat(result).isSucceeded();
        verify(credentialStore).create(any());
    }

    @Test
    void write_withoutIssClaim_shouldStore() {
        // a credential secured with JOSE may name its issuer in the credential only
        var result = writeSigned(claims(), ISSUER_DID + "#key-1");

        assertThat(result).isSucceeded();
    }

    @Test
    void write_notYetValid_shouldStore() {
        var result = writeSigned(claims().issuer(ISSUER_DID).notBeforeTime(Date.from(Instant.now().plusSeconds(3600))), ISSUER_DID + "#key-1");

        assertThat(result).isSucceeded();
    }

    @ParameterizedTest
    @ValueSource(strings = {"did:web:other#key-1", "did:web:issuer:sub#key-1", "did:web:issuerX#key-1", "#key-1"})
    void write_signedByKeyOfOtherDid_shouldNotStore(String keyId) {
        var result = writeSigned(claims().issuer(ISSUER_DID), keyId);

        assertThat(result).isFailed().detail().contains("does not belong to Issuer");
        verify(publicKeyResolver, never()).resolveKey(anyString());
        verifyNoInteractions(credentialStore);
    }

    @Test
    void write_issClaimOfOtherIssuer_shouldNotStore() {
        var result = writeSigned(claims().issuer("did:web:other"), ISSUER_DID + "#key-1");

        assertThat(result).isFailed().detail().contains("does not match Issuer");
        verifyNoInteractions(credentialStore);
    }

    @Test
    void write_expired_shouldNotStore() {
        var result = writeSigned(claims().issuer(ISSUER_DID).expirationTime(Date.from(Instant.now().minusSeconds(60))), ISSUER_DID + "#key-1");

        assertThat(result).isFailed().detail().contains("expired");
        verifyNoInteractions(credentialStore);
    }

    @Test
    void write_unverifiableFormat_whenRejectionEnabled_shouldNotStore() {
        var rejectingWriter = new CredentialWriterImpl(credentialStore, credentialTransformerRegistry, transactionContext,
                JacksonJsonLd.createObjectMapper(), holderCredentialRequestStore, tokenValidationService, publicKeyResolver, new CredentialFormatValidator(), monitor, Clock.systemUTC(), true);

        var result = rejectingWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(new CredentialWriteRequest(VCDM11_LD, CredentialFormat.VC1_0_LD.toString())), PARTICIPANT_ID);

        assertThat(result).isFailed().detail().contains("cannot be verified");
        verifyNoInteractions(tokenValidationService, credentialStore);
        verify(holderCredentialRequestStore, never()).findByIdAndLease(anyString());
    }

    @Test
    void write_fromDifferentIssuer_shouldNotVerifyProofs() {
        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", "did:web:another-issuer", Set.of(new CredentialWriteRequest(VCDM11_JWT, TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);

        // a delivery that may not act on the request at all is rejected before any DID is resolved for it
        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(ServiceFailure.Reason.UNAUTHORIZED);
        verifyNoInteractions(tokenValidationService, credentialStore);
        verify(holderCredentialRequestStore, never()).findByIdAndLease(anyString());
    }

    @Test
    void write_noHolderRequest_shouldNotVerifyProofs() {
        when(holderCredentialRequestStore.findById(anyString())).thenReturn(null);

        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(new CredentialWriteRequest(VCDM11_JWT, TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(ServiceFailure.Reason.NOT_FOUND);
        verifyNoInteractions(tokenValidationService, credentialStore);
        verify(holderCredentialRequestStore, never()).findByIdAndLease(anyString());
    }

    @Test
    void write_shouldVerifyProofsOutsideTransaction() {
        var transactionsOfProofs = new ArrayList<Integer>();
        when(tokenValidationService.validate(anyString(), any(PublicKeyResolver.class), anyList())).thenAnswer(i -> {
            transactionsOfProofs.add(transactionContext.currentTransaction());
            return Result.success(ClaimToken.Builder.newInstance().build());
        });
        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class)))
                .thenReturn(Result.success(createCredential().build()));
        when(credentialStore.create(any())).thenReturn(StoreResult.success());

        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(new CredentialWriteRequest(VCDM11_JWT, TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);

        assertThat(result).isSucceeded();
        // verifying a proof may resolve the Issuer's DID, which must neither hold a database connection nor the leased request
        Assertions.assertThat(transactionsOfProofs).containsExactly(0);
        var inOrder = inOrder(tokenValidationService, holderCredentialRequestStore);
        inOrder.verify(tokenValidationService).validate(anyString(), any(PublicKeyResolver.class), anyList());
        inOrder.verify(holderCredentialRequestStore).findByIdAndLease("holderPid");
    }

    @Test
    void write_invalidFormat() {
        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class))).thenReturn(Result.success(createCredential().build()));

        when(credentialStore.create(any())).thenReturn(StoreResult.success());

        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(new CredentialWriteRequest(VCDM11_JWT, "invalid-format")), PARTICIPANT_ID);
        assertThat(result).isFailed().detail().contains("Invalid format");
    }

    @Test
    @DisplayName("CS-STOR-08: a credential type that was not requested is rejected and nothing is stored")
    void write_typeNotRequested() {
        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class))).thenReturn(Result.success(createCredential().types(List.of("NotRequestedCredential")).build()));

        when(credentialStore.create(any())).thenReturn(StoreResult.success());

        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(new CredentialWriteRequest(VCDM11_JWT, TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);
        assertThat(result).isFailed().detail().contains("No credential request was made for Credentials of type");
    }

    @Test
    @DisplayName("CS-STOR-08: a credential format that was not requested is rejected and nothing is stored")
    void write_formatNotRequested() {
        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class))).thenReturn(Result.success(createCredential().build()));

        when(credentialStore.create(any())).thenReturn(StoreResult.success());

        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(new CredentialWriteRequest(VCDM20_JWT, CredentialFormat.VC2_0_JOSE.toString())), PARTICIPANT_ID);
        assertThat(result).isFailed().detail().contains("No credential request was made for Credentials ");
    }

    @Test
    @DisplayName("a delivered credential expires the credential it supersedes, e.g. on re-issuance after renewal")
    void write_expiresSupersededCredential() {
        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class)))
                .thenReturn(Result.success(createCredential().build()));
        when(credentialStore.create(any())).thenReturn(StoreResult.success());
        when(credentialStore.queryForUpdate(any())).thenReturn(StoreResult.success(List.of(createResource("old-credential-id"))));
        when(credentialStore.update(any())).thenReturn(StoreResult.success());

        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(new CredentialWriteRequest(VCDM11_JWT, TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);

        assertThat(result).isSucceeded();
        var newCredential = ArgumentCaptor.forClass(VerifiableCredentialResource.class);
        verify(credentialStore).create(newCredential.capture());
        verify(credentialStore).update(argThat(resource -> resource.getId().equals("old-credential-id") &&
                resource.getStateAsEnum() == VcStatus.EXPIRED &&
                newCredential.getValue().getId().equals(resource.getMetadata().get(VerifiableCredentialResource.METADATA_SUPERSEDED_BY))));
        // the lookup must be scoped to the participant's own holder credentials from the same Issuer for the same
        // credential object, must not return the credential just stored, and must leave revoked credentials alone. The credentials are
        // locked, so that the watchdog cannot overwrite the supersession, in the order of their IDs, to avoid deadlocks
        verify(credentialStore).queryForUpdate(argThat(querySpec -> {
            var filters = querySpec.getFilterExpression().toString();
            return filters.contains("participantContextId = " + PARTICIPANT_ID) &&
                    filters.contains("usage = Holder") &&
                    filters.contains("metadata.credentialObjectId = test-id") &&
                    filters.contains("issuerId = " + ISSUER_DID) &&
                    filters.contains("id != " + newCredential.getValue().getId()) &&
                    filters.contains("state != " + VcStatus.REVOKED.code()) &&
                    "id".equals(querySpec.getSortField()) && querySpec.getSortOrder() == SortOrder.ASC;
        }));
    }

    @Test
    void write_credentialIssuedByOtherIssuer_expectUnauthorized() {
        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class)))
                .thenReturn(Result.success(createCredential().issuer(new Issuer("did:web:other-issuer", Map.of())).build()));

        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(new CredentialWriteRequest(VCDM11_JWT, TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(ServiceFailure.Reason.UNAUTHORIZED);
        verify(credentialStore, never()).create(any());
        verify(credentialStore, never()).queryForUpdate(any());
        verify(holderCredentialRequestStore, never()).save(any());
    }

    @Test
    @DisplayName("a first-time issuance finds nothing to supersede and modifies no other credential")
    void write_noSupersededCredential_updatesNothing() {
        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class)))
                .thenReturn(Result.success(createCredential().build()));
        when(credentialStore.create(any())).thenReturn(StoreResult.success());

        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(new CredentialWriteRequest(VCDM11_JWT, TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);

        assertThat(result).isSucceeded();
        verify(credentialStore, never()).update(any());
    }

    @Test
    void write_supersededCredentialUpdateFails_expectFailure() {
        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class)))
                .thenReturn(Result.success(createCredential().build()));
        when(credentialStore.create(any())).thenReturn(StoreResult.success());
        when(credentialStore.queryForUpdate(any())).thenReturn(StoreResult.success(List.of(createResource("old-credential-id"))));
        when(credentialStore.update(any())).thenReturn(StoreResult.generalError("update failed"));

        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(new CredentialWriteRequest(VCDM11_JWT, TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);

        assertThat(result).isFailed().detail().contains("update failed");
    }

    @Test
    void write_supersededCredentialLookupFails_expectFailure() {
        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class)))
                .thenReturn(Result.success(createCredential().build()));
        when(credentialStore.create(any())).thenReturn(StoreResult.success());
        when(credentialStore.queryForUpdate(any())).thenReturn(StoreResult.generalError("query failed"));

        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(new CredentialWriteRequest(VCDM11_JWT, TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);

        assertThat(result).isFailed().detail().contains("query failed");
    }

    @Test
    void write_storeFailure() {
        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class)))
                .thenReturn(Result.success(createCredential().build()));

        when(credentialStore.create(any())).thenReturn(StoreResult.alreadyExists("foo"));

        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(new CredentialWriteRequest(VCDM11_JWT, TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);
        // the Issuer is not told the ID of the credential that is stored already
        assertThat(result).isFailed().satisfies(failure -> {
            Assertions.assertThat(failure.getReason()).isEqualTo(ServiceFailure.Reason.CONFLICT);
            Assertions.assertThat(failure.getFailureDetail()).isEqualTo("A delivered credential is already stored");
        });
    }

    @Test
    void write_transformationFailure() {

        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class)))
                .thenReturn(Result.failure("foo"));
        when(credentialStore.create(any())).thenReturn(StoreResult.success());

        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(new CredentialWriteRequest(VCDM11_JWT, TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);
        assertThat(result).isFailed().detail().isEqualTo("foo");
    }

    @Test
    void write_multipleFail_expectSingleFailures() {
        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class)))
                .thenReturn(Result.success(createCredential().build()))
                .thenReturn(Result.failure("foo"));

        when(credentialStore.create(any()))
                .thenReturn(StoreResult.alreadyExists("bar"))
                .thenReturn(StoreResult.success());

        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, List.of(
                new CredentialWriteRequest(VCDM11_JWT, TEST_CREDENTIAL_FORMAT),
                new CredentialWriteRequest(VCDM20_JWT, CredentialFormat.VC2_0_JOSE.toString())), PARTICIPANT_ID);
        assertThat(result).isFailed().detail()
                .isEqualTo("A delivered credential is already stored");
    }

    @Test
    void write_whenLaterCredentialCannotBeStored_shouldRollBackWholeDelivery() {
        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class)))
                .thenReturn(Result.success(createCredential().build()));
        var createTransactions = new ArrayList<Integer>();
        when(credentialStore.create(any())).thenAnswer(i -> {
            createTransactions.add(transactionContext.currentTransaction());
            return createTransactions.size() == 1 ? StoreResult.success() : StoreResult.alreadyExists("exists");
        });

        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, List.of(
                new CredentialWriteRequest(VCDM11_JWT, TEST_CREDENTIAL_FORMAT),
                new CredentialWriteRequest(VCDM11_JWT, TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);

        assertThat(result).isFailed();
        // the first credential was stored already, and must not be committed without the second one
        Assertions.assertThat(createTransactions).hasSize(2).allSatisfy(transaction ->
                Assertions.assertThat(transactionContext.isRolledBack(transaction)).isTrue());
        verify(holderCredentialRequestStore).breakLease(any());
        verify(holderCredentialRequestStore, never()).save(any());
    }

    @Test
    @DisplayName("CS-STOR-07: a holderPid matching no pending request is rejected and nothing is stored")
    void write_noHolderRequestFound_expectFailure() {
        // e.g. the request was deleted after its origin was checked
        when(holderCredentialRequestStore.findByIdAndLease(anyString())).thenReturn(StoreResult.notFound("foo"));

        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(new CredentialWriteRequest(VCDM11_JWT, TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);
        assertThat(result).isFailed().detail().contains("foo");
        verifyNoInteractions(credentialStore, credentialTransformerRegistry);
    }

    @Test
    void write_holderRequestInWrongState_expectFailure() {
        storeHolds(HolderCredentialRequest.Builder.newInstance()
                .issuerDid(ISSUER_DID)
                .requestedCredential("test-id", TEST_CREDENTIAL_TYPE, TEST_CREDENTIAL_FORMAT)
                .state(REQUESTING.code())
                .participantContextId(PARTICIPANT_ID)
                .build());

        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(new CredentialWriteRequest(VCDM11_JWT, TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);
        assertThat(result).isFailed()
                .detail().startsWith("HolderCredentialRequest is expected to be in ");
        verifyNoInteractions(credentialStore, credentialTransformerRegistry);
    }

    // CS-STOR-06: status=ISSUED with empty credentials list -> accepted as no-op: success, nothing stored, holder request state unchanged
    @Test
    @DisplayName("CS-STOR-06: an ISSUED message with an empty credentials list is a no-op and leaves the request state unchanged")
    void write_emptyCredentials_expectNoOpSuccess() {
        // default fixture (setUp()) returns a request in state REQUESTED

        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(), PARTICIPANT_ID);

        assertThat(result).isSucceeded();
        verifyNoInteractions(credentialStore, credentialTransformerRegistry);
        // the request must not be transitioned to ISSUED when no credentials were delivered
        verify(holderCredentialRequestStore, never()).save(any());
        verify(holderCredentialRequestStore).breakLease(argThat(request -> request.getState() == REQUESTED.code()));
    }

    // CS-STOR-07: message issuerPid differs from the issuerPid already stored on the HolderCredentialRequest -> rejected, not silently overwritten
    @Test
    @DisplayName("CS-STOR-07: a message whose issuerPid differs from the one stored on the request is rejected")
    void write_issuerPidMismatch_expectFailure() {
        // the stored request already carries a different issuerPid
        storeHolds(HolderCredentialRequest.Builder.newInstance()
                .issuerDid(ISSUER_DID)
                .requestedCredential("test-id", TEST_CREDENTIAL_TYPE, TEST_CREDENTIAL_FORMAT)
                .state(REQUESTED.code())
                .participantContextId(PARTICIPANT_ID)
                .issuerPid("stored-issuer-pid")
                .build());
        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class)))
                .thenReturn(Result.success(createCredential().build()));
        when(credentialStore.create(any())).thenReturn(StoreResult.success());

        var result = credentialWriter.write("holderPid", HOLDER_DID, "a-different-issuer-pid", ISSUER_DID, Set.of(new CredentialWriteRequest(VCDM11_JWT, TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);

        assertThat(result).isFailed();
        verify(holderCredentialRequestStore, never()).save(argThat(request -> "a-different-issuer-pid".equals(request.getIssuerPid())));
    }

    // CS-STOR-07: cross-tenant — holderPid belongs to a request of participant B, but write() is called with participantContextId A -> not-found/unauthorized, nothing stored
    @Test
    @DisplayName("CS-STOR-07: a holderPid belonging to another participant context is rejected and nothing is stored")
    void write_holderRequestBelongsToOtherParticipantContext_expectFailure() {
        // the stored request belongs to participant B
        storeHolds(HolderCredentialRequest.Builder.newInstance()
                .issuerDid(ISSUER_DID)
                .requestedCredential("test-id", TEST_CREDENTIAL_TYPE, TEST_CREDENTIAL_FORMAT)
                .state(REQUESTED.code())
                .participantContextId("participant-b")
                .build());
        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class)))
                .thenReturn(Result.success(createCredential().build()));
        when(credentialStore.create(any())).thenReturn(StoreResult.success());

        // write is invoked for participant A - the lookup must be scoped to that participant context
        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(new CredentialWriteRequest(VCDM11_JWT, TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);

        // rejected (not-found/unauthorized), nothing stored, no existence leak across tenants
        assertThat(result).isFailed();
        verify(credentialStore, never()).create(any());
    }

    // CS-STOR-13: exact re-delivery of the same credential to a request already in ISSUED -> no-op success, credentialStore.create not called again, no duplicate
    @Test
    @DisplayName("CS-STOR-13: re-delivery of the same credential to an ISSUED request is an idempotent no-op without duplicate storage")
    void write_redeliveryToIssuedRequest_expectIdempotentNoOp() {
        // the request is already in state ISSUED
        storeHolds(HolderCredentialRequest.Builder.newInstance()
                .issuerDid(ISSUER_DID)
                .requestedCredential("test-id", TEST_CREDENTIAL_TYPE, TEST_CREDENTIAL_FORMAT)
                .state(ISSUED.code())
                .participantContextId(PARTICIPANT_ID)
                .issuerPid("issuerPid")
                .build());
        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class)))
                .thenReturn(Result.success(createCredential().build()));
        when(credentialStore.create(any())).thenReturn(StoreResult.success());

        // the very same credential is delivered a second time
        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(new CredentialWriteRequest(VCDM11_JWT, TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);

        // accepted, but no duplicate is written
        assertThat(result).isSucceeded();
        verify(credentialStore, never()).create(any());
        // the existing credential request is not modified, no storage interaction
        verify(holderCredentialRequestStore, never()).save(any());
    }

    // A3.25 / CS-STOR-05: a REJECTED CredentialMessage fails the pending request instead of leaving it waiting forever
    @Test
    @DisplayName("CS-STOR-05: a rejection from the request's issuer moves the request to ERROR and stores nothing")
    void reject_pendingRequest_expectTransitionToError() {
        var request = HolderCredentialRequest.Builder.newInstance()
                .issuerDid(ISSUER_DID)
                .requestedCredential("test-id", TEST_CREDENTIAL_TYPE, TEST_CREDENTIAL_FORMAT)
                .state(REQUESTED.code())
                .participantContextId(PARTICIPANT_ID)
                .issuerPid("issuerPid")
                .build();
        storeHolds(request);

        var result = credentialWriter.reject("holderPid", "issuerPid", ISSUER_DID, "attestation not satisfied", PARTICIPANT_ID);

        assertThat(result).isSucceeded();
        Assertions.assertThat(request.stateAsEnum()).isEqualTo(HolderRequestState.ERROR);
        Assertions.assertThat(request.getErrorDetail()).contains("attestation not satisfied");
        verify(holderCredentialRequestStore).save(request);
        verifyNoInteractions(credentialStore);
    }

    @Test
    @DisplayName("CS-STOR-05: a rejection without a reason still fails the request")
    void reject_withoutReason_expectTransitionToError() {
        var request = HolderCredentialRequest.Builder.newInstance()
                .issuerDid(ISSUER_DID)
                .requestedCredential("test-id", TEST_CREDENTIAL_TYPE, TEST_CREDENTIAL_FORMAT)
                .state(REQUESTED.code())
                .participantContextId(PARTICIPANT_ID)
                .issuerPid("issuerPid")
                .build();
        storeHolds(request);

        var result = credentialWriter.reject("holderPid", "issuerPid", ISSUER_DID, null, PARTICIPANT_ID);

        assertThat(result).isSucceeded();
        Assertions.assertThat(request.stateAsEnum()).isEqualTo(HolderRequestState.ERROR);
        Assertions.assertThat(request.getErrorDetail()).contains("issuerPid");
    }

    @Test
    void reject_saveFails_expectFailure() {
        var request = HolderCredentialRequest.Builder.newInstance()
                .issuerDid(ISSUER_DID)
                .requestedCredential("test-id", TEST_CREDENTIAL_TYPE, TEST_CREDENTIAL_FORMAT)
                .state(REQUESTED.code())
                .participantContextId(PARTICIPANT_ID)
                .issuerPid("issuerPid")
                .build();
        storeHolds(request);
        when(holderCredentialRequestStore.save(any())).thenReturn(StoreResult.generalError("save failed"));

        var result = credentialWriter.reject("holderPid", "issuerPid", ISSUER_DID, "attestation not satisfied", PARTICIPANT_ID);

        assertThat(result).isFailed().detail().isEqualTo("save failed");
        verify(holderCredentialRequestStore).breakLease(request);
    }

    @Test
    @DisplayName("CS-STOR-09: a rejection from an issuer other than the one addressed is not accepted")
    void reject_fromDifferentIssuer_expectUnauthorized() {
        var request = HolderCredentialRequest.Builder.newInstance()
                .issuerDid(ISSUER_DID)
                .requestedCredential("test-id", TEST_CREDENTIAL_TYPE, TEST_CREDENTIAL_FORMAT)
                .state(REQUESTED.code())
                .participantContextId(PARTICIPANT_ID)
                .issuerPid("issuerPid")
                .build();
        storeHolds(request);

        var result = credentialWriter.reject("holderPid", "issuerPid", "did:web:someone-else", "nope", PARTICIPANT_ID);

        assertThat(result).isFailed().detail().doesNotContain(ISSUER_DID);
        Assertions.assertThat(request.stateAsEnum()).isEqualTo(REQUESTED);
        verify(holderCredentialRequestStore, never()).save(any());
        verify(holderCredentialRequestStore, never()).findByIdAndLease(anyString());
    }

    @Test
    @DisplayName("CS-STOR-13: a rejection trailing a completed issuance is acknowledged but does not undo it")
    void reject_afterCredentialsIssued_expectNoOp() {
        var request = HolderCredentialRequest.Builder.newInstance()
                .issuerDid(ISSUER_DID)
                .requestedCredential("test-id", TEST_CREDENTIAL_TYPE, TEST_CREDENTIAL_FORMAT)
                .state(ISSUED.code())
                .participantContextId(PARTICIPANT_ID)
                .issuerPid("issuerPid")
                .build();
        storeHolds(request);

        var result = credentialWriter.reject("holderPid", "issuerPid", ISSUER_DID, "too late", PARTICIPANT_ID);

        assertThat(result).isSucceeded();
        Assertions.assertThat(request.stateAsEnum()).isEqualTo(ISSUED);
        verify(holderCredentialRequestStore, never()).save(any());
        verify(holderCredentialRequestStore).breakLease(request);
    }

    @Test
    @DisplayName("CS-STOR-05: a rejection for another participant context's request is not observable")
    void reject_crossTenant_expectNotFound() {
        var request = HolderCredentialRequest.Builder.newInstance()
                .issuerDid(ISSUER_DID)
                .requestedCredential("test-id", TEST_CREDENTIAL_TYPE, TEST_CREDENTIAL_FORMAT)
                .state(REQUESTED.code())
                .participantContextId("another-participant")
                .issuerPid("issuerPid")
                .build();
        storeHolds(request);

        var result = credentialWriter.reject("holderPid", "issuerPid", ISSUER_DID, "nope", PARTICIPANT_ID);

        assertThat(result).isFailed();
        Assertions.assertThat(result.reason()).isEqualTo(ServiceFailure.Reason.NOT_FOUND);
        Assertions.assertThat(request.stateAsEnum()).isEqualTo(REQUESTED);
        verify(holderCredentialRequestStore, never()).findByIdAndLease(anyString());
    }

    @Test
    void write_vcdm20CredentialDeclaredAsVcdm11_shouldNotStore() {
        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(new CredentialWriteRequest(VCDM20_JWT, CredentialProfile.DCP_PROFILE_VC11)), PARTICIPANT_ID);

        assertThat(result).isFailed().detail().contains("does not match its format");
        Assertions.assertThat(result.reason()).isEqualTo(ServiceFailure.Reason.BAD_REQUEST);
        verifyNoInteractions(tokenValidationService, credentialStore);
        verify(holderCredentialRequestStore, never()).findByIdAndLease(anyString());
    }

    @Test
    void write_vcdm11CredentialDeclaredAsVcdm20_shouldNotStore() {
        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(new CredentialWriteRequest(VCDM11_JWT, CredentialProfile.DCP_PROFILE_VC20)), PARTICIPANT_ID);

        assertThat(result).isFailed().detail().contains("does not match its format");
        verifyNoInteractions(tokenValidationService, credentialStore);
    }

    @Test
    void write_vcdm20LdCredentialDeclaredAsVcdm11_shouldNotStore() {
        var vcdm20Ld = "{\"@context\": [\"%s\"], \"type\": [\"VerifiableCredential\"]}".formatted(VC_PREFIX_V2);

        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(new CredentialWriteRequest(vcdm20Ld, CredentialFormat.VC1_0_LD.toString())), PARTICIPANT_ID);

        assertThat(result).isFailed().detail().contains("does not match its format");
        verifyNoInteractions(credentialStore);
    }

    @ParameterizedTest
    @EnumSource(value = CredentialFormat.class, names = { "VC2_0_SD_JWT", "VC2_0_COSE" })
    void write_formatCannotBePresented_shouldNotStore(CredentialFormat format) {
        var result = credentialWriter.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(new CredentialWriteRequest(VCDM20_JWT, format.toString())), PARTICIPANT_ID);

        assertThat(result).isFailed().detail().contains("cannot be presented");
        Assertions.assertThat(result.reason()).isEqualTo(ServiceFailure.Reason.BAD_REQUEST);
        verifyNoInteractions(tokenValidationService, credentialStore);
    }

    /**
     * Delivers a credential signed with a key that the DID resolver returns for any key ID, and verifies it with the
     * actual token validation.
     */
    private ServiceResult<Void> writeSigned(JWTClaimsSet.Builder claims, String keyId) {
        var key = generateEcKey(keyId);
        when(publicKeyResolver.resolveKey(anyString())).thenAnswer(i -> Result.success(key.toPublicKey()));
        when(credentialTransformerRegistry.transform(isA(String.class), eq(VerifiableCredential.class)))
                .thenReturn(Result.success(createCredential().build()));
        when(credentialStore.create(any())).thenReturn(StoreResult.success());
        var writer = new CredentialWriterImpl(credentialStore, credentialTransformerRegistry, transactionContext, JacksonJsonLd.createObjectMapper(),
                holderCredentialRequestStore, new TokenValidationServiceImpl(), publicKeyResolver, new CredentialFormatValidator(), monitor, Clock.systemUTC(), false);

        var rawCredential = buildSignedJwt(claims.build(), key).serialize();
        return writer.write("holderPid", HOLDER_DID, "issuerPid", ISSUER_DID, Set.of(new CredentialWriteRequest(rawCredential, TEST_CREDENTIAL_FORMAT)), PARTICIPANT_ID);
    }

    private JWTClaimsSet.Builder claims() {
        return new JWTClaimsSet.Builder().subject(HOLDER_DID).claim("vc", Map.of());
    }

    /**
     * The request is looked up twice: to check the origin of a delivery before its proofs are verified, and leased to
     * store the credentials.
     */
    private void storeHolds(HolderCredentialRequest request) {
        when(holderCredentialRequestStore.findById(anyString())).thenReturn(request);
        when(holderCredentialRequestStore.findByIdAndLease(anyString())).thenReturn(StoreResult.success(request));
    }

    private VerifiableCredentialResource createResource(String id) {
        return VerifiableCredentialResource.Builder.newHolder()
                .id(id)
                .issuerId(ISSUER_DID)
                .holderId(HOLDER_DID)
                .state(VcStatus.REQUESTED)
                .participantContextId(PARTICIPANT_ID)
                .metadata(VerifiableCredentialResource.METADATA_CREDENTIAL_OBJECT_ID, "test-id")
                .credential(new VerifiableCredentialContainer("raw-cred", CredentialFormat.VC1_0_JWT, createCredential().build()))
                .build();
    }

    private VerifiableCredential.Builder createCredential() {
        return VerifiableCredential.Builder.newInstance()
                .types(List.of(TEST_CREDENTIAL_TYPE))
                .id(UUID.randomUUID().toString())
                .issuer(new Issuer(ISSUER_DID, Map.of()))
                .issuanceDate(Instant.now())
                .credentialSubject(CredentialSubject.Builder.newInstance().id(HOLDER_DID).claim("test-claim", "test-value").build());
    }

    private static String unsignedJwt(Map<String, Object> claims) {
        var encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString("{\"alg\":\"ES256\"}".getBytes(StandardCharsets.UTF_8)) + "." +
                encoder.encodeToString(JSONObjectUtils.toJSONString(claims).getBytes(StandardCharsets.UTF_8)) + ".AAAA";
    }
}

