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

package org.eclipse.edc.identityhub.participantcontext;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKParameterNames;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.VerificationMethod;
import org.eclipse.edc.identithub.did.spi.DidDocumentService;
import org.eclipse.edc.identityhub.security.KeyPairGenerator;
import org.eclipse.edc.identityhub.spi.ParticipantContextService;
import org.eclipse.edc.identityhub.spi.model.participant.KeyDescriptor;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantContext;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantContextState;
import org.eclipse.edc.identityhub.spi.model.participant.ParticipantManifest;
import org.eclipse.edc.identityhub.spi.store.ParticipantContextStore;
import org.eclipse.edc.security.token.jwt.CryptoConverter;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.KeyParserRegistry;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.security.KeyPair;
import java.security.PublicKey;
import java.text.ParseException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.eclipse.edc.spi.result.ServiceResult.badRequest;
import static org.eclipse.edc.spi.result.ServiceResult.conflict;
import static org.eclipse.edc.spi.result.ServiceResult.fromFailure;
import static org.eclipse.edc.spi.result.ServiceResult.notFound;
import static org.eclipse.edc.spi.result.ServiceResult.success;

/**
 * Default implementation of the {@link ParticipantContextService}. Uses a {@link Vault} to store API tokens and a {@link ApiTokenGenerator}
 * to generate API tokens. Please use a generator that produces Strings of a reasonable length.
 * <p>
 * This service is transactional.
 */
public class ParticipantContextServiceImpl implements ParticipantContextService {

    private static final String API_KEY_ALIAS_SUFFIX = "apikey";
    private final ParticipantContextStore participantContextStore;
    private final Vault vault;
    private final TransactionContext transactionContext;
    private final ApiTokenGenerator tokenGenerator;
    private final KeyParserRegistry keyParserRegistry;
    private final DidDocumentService didDocumentService;

    public ParticipantContextServiceImpl(ParticipantContextStore participantContextStore, Vault vault, TransactionContext transactionContext,
                                         KeyParserRegistry registry, DidDocumentService didDocumentService) {
        this.participantContextStore = participantContextStore;
        this.vault = vault;
        this.transactionContext = transactionContext;
        this.tokenGenerator = new ApiTokenGenerator();
        this.keyParserRegistry = registry;
        this.didDocumentService = didDocumentService;
    }

    @Override
    public ServiceResult<String> createParticipantContext(ParticipantManifest manifest) {
        return transactionContext.execute(() -> {
            var apiKey = new AtomicReference<String>();
            var context = convert(manifest);
            var res = createParticipantContext(context)
                    .compose(u -> generateAndStoreToken(context)).onSuccess(apiKey::set)
                    .compose(ak -> createOrUpdateKey(manifest.getKey()))
                    .compose(jwk -> generateDidDocument(manifest, jwk));
            return res.map(u -> apiKey.get());
        });
    }

    @Override
    public ServiceResult<ParticipantContext> getParticipantContext(String participantId) {
        return transactionContext.execute(() -> {
            var res = participantContextStore.query(QuerySpec.Builder.newInstance().filter(new Criterion("participantId", "=", participantId)).build());
            if (res.succeeded()) {
                return res.getContent().stream().findFirst()
                        .map(ServiceResult::success)
                        .orElse(notFound("No ParticipantContext with ID '%s' was found.".formatted(participantId)));
            }
            return fromFailure(res);
        });
    }

    @Override
    public ServiceResult<Void> deleteParticipantContext(String participantId) {
        return transactionContext.execute(() -> {
            var did = Optional.ofNullable(findByIdInternal(participantId)).map(ParticipantContext::getDid);
            var res = participantContextStore.deleteById(participantId);
            if (res.failed()) {
                return fromFailure(res);
            }

            return did.map(d -> didDocumentService.unpublish(d).compose(u -> didDocumentService.deleteById(d)))
                    .orElseGet(ServiceResult::success);
        });
    }

    @Override
    public ServiceResult<String> regenerateApiToken(String participantId) {
        return transactionContext.execute(() -> {
            var participantContext = getParticipantContext(participantId);
            if (participantContext.failed()) {
                return participantContext.map(pc -> null);
            }
            return generateAndStoreToken(participantContext.getContent());
        });
    }

    @Override
    public ServiceResult<Void> updateParticipant(String participantId, Consumer<ParticipantContext> modificationFunction) {
        return transactionContext.execute(() -> {
            var participant = findByIdInternal(participantId);
            if (participant == null) {
                return notFound("ParticipantContext with ID '%s' not found.".formatted(participantId));
            }
            modificationFunction.accept(participant);
            var res = participantContextStore.update(participant);
            return res.succeeded() ? success() : fromFailure(res);
        });

    }

    private ServiceResult<String> generateAndStoreToken(ParticipantContext participantContext) {
        var alias = participantContext.getApiTokenAlias();
        var newToken = tokenGenerator.generate(participantContext.getParticipantId());
        return vault.storeSecret(alias, newToken)
                .map(unused -> success(newToken))
                .orElse(f -> conflict("Could not store new API token: %s.".formatted(f.getFailureDetail())));
    }

    private ServiceResult<Void> generateDidDocument(ParticipantManifest manifest, JWK publicKey) {
        var doc = DidDocument.Builder.newInstance()
                .id(manifest.getDid())
                .service(manifest.getServiceEndpoints().stream().toList())
                .verificationMethod(List.of(VerificationMethod.Builder.newInstance()
                        .publicKeyJwk(publicKey.toJSONObject())
                        .build()))
                .build();
        return didDocumentService.store(doc)
                .compose(u -> manifest.isActive() ? didDocumentService.publish(doc.getId()) : success());
    }

    private ServiceResult<JWK> createOrUpdateKey(KeyDescriptor key) {
        // do we need to generate the key?
        var keyGeneratorParams = key.getKeyGeneratorParams();
        JWK publicKeyJwk;
        if (keyGeneratorParams != null) {
            var kp = KeyPairGenerator.generateKeyPair(keyGeneratorParams);
            if (kp.failed()) {
                return badRequest("Could not generate KeyPair from generator params: %s".formatted(kp.getFailureDetail()));
            }
            var alias = key.getPrivateKeyAlias();
            var storeResult = vault.storeSecret(alias, CryptoConverter.createJwk(kp.getContent()).toJSONString());
            if (storeResult.failed()) {
                return badRequest(storeResult.getFailureDetail());
            }
            publicKeyJwk = CryptoConverter.createJwk(kp.getContent()).toPublicJWK();
        } else if (key.getPublicKeyJwk() != null) {
            publicKeyJwk = CryptoConverter.create(key.getPublicKeyJwk());
        } else if (key.getPublicKeyPem() != null) {
            var pubKey = keyParserRegistry.parse(key.getPublicKeyPem());
            if (pubKey.failed()) {
                return badRequest("Cannot parse public key from PEM: %s".formatted(pubKey.getFailureDetail()));
            }
            publicKeyJwk = CryptoConverter.createJwk(new KeyPair((PublicKey) pubKey.getContent(), null));
        } else {
            return badRequest("No public key information found in KeyDescriptor.");
        }
        // insert the "kid" parameter
        var json = publicKeyJwk.toJSONObject();
        json.put(JWKParameterNames.KEY_ID, key.getKeyId());
        try {
            publicKeyJwk = JWK.parse(json);
            return success(publicKeyJwk);
        } catch (ParseException e) {
            return badRequest("Could not create JWK: %s".formatted(e.getMessage()));
        }
    }

    private ServiceResult<Void> createParticipantContext(ParticipantContext context) {
        var storeRes = participantContextStore.create(context);
        return storeRes.succeeded() ?
                success() :
                fromFailure(storeRes);
    }

    private ParticipantContext findByIdInternal(String participantId) {
        var resultStream = participantContextStore.query(QuerySpec.Builder.newInstance().filter(new Criterion("participantContext", "=", participantId)).build());
        if (resultStream.failed()) return null;
        return resultStream.getContent().stream().findFirst().orElse(null);
    }


    private ParticipantContext convert(ParticipantManifest manifest) {
        return ParticipantContext.Builder.newInstance()
                .participantId(manifest.getParticipantId())
                .apiTokenAlias("%s-%s".formatted(manifest.getParticipantId(), API_KEY_ALIAS_SUFFIX))
                .state(manifest.isActive() ? ParticipantContextState.ACTIVATED : ParticipantContextState.CREATED)
                .build();
    }
}
