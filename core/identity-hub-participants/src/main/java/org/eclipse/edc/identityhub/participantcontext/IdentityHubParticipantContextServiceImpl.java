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

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.eclipse.edc.identityhub.spi.did.store.DidResourceStore;
import org.eclipse.edc.identityhub.spi.participantcontext.IdentityApiScopes;
import org.eclipse.edc.identityhub.spi.participantcontext.IdentityHubParticipantContextService;
import org.eclipse.edc.identityhub.spi.participantcontext.IssuerAdminApiScopes;
import org.eclipse.edc.identityhub.spi.participantcontext.StsAccountProvisioner;
import org.eclipse.edc.identityhub.spi.participantcontext.events.ParticipantContextObservable;
import org.eclipse.edc.identityhub.spi.participantcontext.model.CreateParticipantContextResponse;
import org.eclipse.edc.identityhub.spi.participantcontext.model.IdentityHubParticipantContext;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.eclipse.edc.participantcontext.spi.config.service.ParticipantContextConfigService;
import org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContextState;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.transaction.spi.TransactionContext;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static org.eclipse.edc.spi.result.ServiceResult.conflict;
import static org.eclipse.edc.spi.result.ServiceResult.fromFailure;
import static org.eclipse.edc.spi.result.ServiceResult.notFound;
import static org.eclipse.edc.spi.result.ServiceResult.success;

/**
 * Default implementation of the {@link IdentityHubParticipantContextService}. Uses a {@link Vault} to store API tokens and a {@link ApiTokenGenerator}
 * to generate API tokens. Please use a generator that produces Strings of a reasonable length.
 * <p>
 * This service is transactional.
 */
public class IdentityHubParticipantContextServiceImpl implements IdentityHubParticipantContextService {

    private static final String API_KEY_ALIAS_SUFFIX = "apikey";
    private final ParticipantContextStore participantContextStore;
    private final DidResourceStore didResourceStore;
    private final Vault vault;
    private final TransactionContext transactionContext;
    private final ApiTokenGenerator tokenGenerator;
    private final ParticipantContextObservable observable;
    private final StsAccountProvisioner stsAccountProvisioner;
    private final ParticipantContextConfigService configService;
    private final Monitor monitor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IdentityHubParticipantContextServiceImpl(ParticipantContextStore participantContextStore,
                                                    DidResourceStore didResourceStore,
                                                    Vault vault,
                                                    TransactionContext transactionContext,
                                                    ParticipantContextObservable observable,
                                                    StsAccountProvisioner stsAccountProvisioner,
                                                    ParticipantContextConfigService configService,
                                                    Monitor monitor) {
        this.participantContextStore = participantContextStore;
        this.didResourceStore = didResourceStore;
        this.vault = vault;
        this.transactionContext = transactionContext;
        this.observable = observable;
        this.stsAccountProvisioner = stsAccountProvisioner;
        this.configService = configService;
        this.monitor = monitor;
        this.tokenGenerator = new ApiTokenGenerator();
    }

    @WithSpan(value = "participant-context.create", kind = SpanKind.INTERNAL)
    @Override
    public ServiceResult<CreateParticipantContextResponse> createParticipantContext(ParticipantManifest manifest) {
        // the vault is not part of the transaction, so the secrets that are stored are deleted again if the creation fails
        var storedSecretAliases = new ArrayList<String>();
        try {
            return transactionContext.execute(() -> {
                if (didResourceStore.findById(manifest.getDid()) != null) {
                    return ServiceResult.conflict("Another participant with the same DID '%s' already exists.".formatted(manifest.getDid()));
                }
                var context = convert(manifest);

                var createResult = participantContextStore.create(context);
                if (createResult.failed()) {
                    // e.g. a participant context with the same ID exists, whose configuration must not be overwritten: the
                    // configuration is saved with an upsert
                    return ServiceResult.fromFailure(createResult);
                }

                // from here on, a failure rolls back the transaction, so that no partially provisioned participant context remains
                rollbackOnFailure(saveConfiguration(context));

                String apiKey = null;
                if (manifest.isProvisionApiKey()) {
                    apiKey = rollbackOnFailure(createTokenAndStoreInVault(context));
                    storedSecretAliases.add(context.getApiTokenAlias());
                }

                var response = new CreateParticipantContextResponse(apiKey, null, null);
                if (manifest.isProvisionStsAccount()) {
                    var accountInfo = rollbackOnFailure(stsAccountProvisioner.create(manifest));
                    if (accountInfo != null) {
                        storedSecretAliases.add(manifest.clientSecretAlias());
                        response = new CreateParticipantContextResponse(apiKey, accountInfo.clientId(), accountInfo.clientSecret());
                    }
                }

                // the ParticipantContextEventCoordinator creates the DID document and the key pairs, and throws a
                // ProvisioningException if that fails
                observable.invokeForEach(l -> l.created(context, manifest));
                return success(response);
            });
        } catch (ProvisioningException e) {
            deleteSecrets(manifest.getParticipantContextId(), storedSecretAliases);
            return e.failure();
        } catch (RuntimeException e) {
            deleteSecrets(manifest.getParticipantContextId(), storedSecretAliases);
            throw e;
        }
    }

    @Override
    public ServiceResult<IdentityHubParticipantContext> getParticipantContext(String participantContextId) {
        return transactionContext.execute(() -> ServiceResult.from(participantContextStore.findById(participantContextId))
                .map(this::convert));
    }

    @Override
    public ServiceResult<Void> deleteParticipantContext(String participantContextId) {
        return transactionContext.execute(() -> {
            var participantContext = findByIdInternal(participantContextId);
            if (participantContext == null) {
                return ServiceResult.notFound("A ParticipantContext with ID '%s' does not exist.");
            }
            // deactivating the PC must be the first step, because unpublishing DIDs requires the PC to be in the DEACTIVATED state.
            // Unpublishing DIDs happens in callback of the "-Deleting" Event
            return updateParticipant(participantContextId, IdentityHubParticipantContext::deactivate)
                    .compose(v -> {
                        observable.invokeForEach(l -> l.deleting(participantContext));
                        var res = participantContextStore.deleteById(participantContextId);
                        vault.deleteSecret(participantContext.getParticipantContextId(), participantContext.getApiTokenAlias());
                        if (res.failed()) {
                            return fromFailure(res);
                        }

                        observable.invokeForEach(l -> l.deleted(participantContext));
                        return ServiceResult.success();
                    });
        });
    }

    @Override
    public ServiceResult<String> regenerateApiToken(String participantContextId) {
        return transactionContext.execute(() -> {
            var participantContext = getParticipantContext(participantContextId);
            if (participantContext.failed()) {
                return participantContext.map(pc -> null);
            }
            return createTokenAndStoreInVault(participantContext.getContent());
        });
    }

    @Override
    @WithSpan(value = "participant-context.update", kind = SpanKind.INTERNAL)
    public ServiceResult<Void> updateParticipant(String participantContextId, Consumer<IdentityHubParticipantContext> modificationFunction) {
        return transactionContext.execute(() -> {
            // the participant context is locked until the transaction completes, so that concurrent updates, e.g. on other
            // replicas, do not overwrite each other
            var participant = participantContextStore.findByIdForUpdate(participantContextId)
                    .map(this::convert)
                    .orElse(f -> null);
            if (participant == null) {
                return notFound("ParticipantContext with ID '%s' not found.".formatted(participantContextId));
            }
            modificationFunction.accept(participant);
            var res = participantContextStore.update(participant)
                    .onSuccess(u -> observable.invokeForEach(l -> l.updated(participant)));
            return res.succeeded() ? success() : fromFailure(res);
        });

    }

    @Override
    public ServiceResult<Collection<IdentityHubParticipantContext>> query(QuerySpec querySpec) {
        return transactionContext.execute(() -> ServiceResult.from(participantContextStore.query(querySpec))
                .map(participantContexts -> participantContexts.stream().map(this::convert)
                        .collect(Collectors.toList())));
    }

    private ServiceResult<String> createTokenAndStoreInVault(IdentityHubParticipantContext participantContext) {
        var alias = participantContext.getApiTokenAlias();
        var newToken = tokenGenerator.generate(participantContext.getParticipantContextId());
        return vault.storeSecret(participantContext.getParticipantContextId(), alias, newToken)
                .map(unused -> success(newToken))
                .orElse(f -> conflict("Could not store new API token: %s.".formatted(f.getFailureDetail())));
    }


    private ServiceResult<Void> saveConfiguration(IdentityHubParticipantContext context) {
        var config = context.getProperties().entrySet().stream()
                .collect(toMap(Map.Entry::getKey, e -> {
                    if (e.getValue() instanceof String v) {
                        return v;
                    }
                    try {
                        return objectMapper.writeValueAsString(e.getValue());
                    } catch (JacksonException ex) {
                        throw new RuntimeException(ex);
                    }
                }));

        var cfg = ParticipantContextConfiguration.Builder.newInstance()
                .participantContextId(context.getParticipantContextId())
                .privateEntries(config)
                .build();
        return configService.save(cfg);
    }

    private <T> T rollbackOnFailure(ServiceResult<T> result) {
        if (result.failed()) {
            throw new ProvisioningException(result);
        }
        return result.getContent();
    }

    private void deleteSecrets(String participantContextId, List<String> aliases) {
        aliases.forEach(alias -> vault.deleteSecret(participantContextId, alias)
                .onFailure(f -> monitor.warning("Failed to delete the secret '%s' of participant context '%s', whose creation failed. It must be deleted manually: %s"
                        .formatted(alias, participantContextId, f.getFailureDetail()))));
    }

    private IdentityHubParticipantContext findByIdInternal(String participantContextId) {
        var resultStream = participantContextStore.findById(participantContextId)
                .map(this::convert);
        return resultStream.orElse(f -> null);
    }


    private IdentityHubParticipantContext convert(ParticipantManifest manifest) {
        var apiKeyAlias = ofNullable(manifest.getApiKeyAlias()).orElse("%s-%s".formatted(manifest.getParticipantContextId(), API_KEY_ALIAS_SUFFIX));
        var context = IdentityHubParticipantContext.Builder.newInstance()
                .id(manifest.getParticipantContextId())
                .scopes(manifest.getScopes())
                .did(manifest.getDid())
                .apiTokenAlias(apiKeyAlias)
                .state(ParticipantContextState.CREATED)
                .properties(manifest.getAdditionalProperties());
        if (manifest.getScopes().isEmpty()) {
            context.scopes(List.of(IdentityApiScopes.READ, IdentityApiScopes.WRITE, IssuerAdminApiScopes.READ, IssuerAdminApiScopes.WRITE));
        }

        return context.build();
    }

    private IdentityHubParticipantContext convert(ParticipantContext participantContext) {
        return IdentityHubParticipantContext.Builder.newInstance()
                .id(participantContext.getParticipantContextId())
                .did(participantContext.getIdentity())
                .state(participantContext.getStateAsEnum())
                .createdAt(participantContext.getCreatedAt())
                .lastModified(participantContext.getLastModified())
                .properties(participantContext.getProperties())
                .build();
    }
}
