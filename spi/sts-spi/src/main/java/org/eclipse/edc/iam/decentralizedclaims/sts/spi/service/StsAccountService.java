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

package org.eclipse.edc.iam.decentralizedclaims.sts.spi.service;

import org.eclipse.edc.iam.decentralizedclaims.sts.spi.model.StsAccount;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Mediates access to, modification and authentication of {@link StsAccount}s.
 */
@ExtensionPoint
public interface StsAccountService {
    ServiceResult<Void> createAccount(ParticipantManifest manifest, String secretAlias);

    /**
     * Deletes an {@link StsAccount} by its ID.
     *
     * @param participantContextId The (database) ID
     * @return A successful result, or a failure indicating what went wrong.
     */
    ServiceResult<Void> deleteAccount(String participantContextId);

    /**
     * Updates an existing {@link StsAccount}, overwriting all values with the given new object.
     *
     * @param updatedAccount the new account
     * @return A successful result, or a failure indicating what went wrong.
     */
    ServiceResult<Void> updateAccount(StsAccount updatedAccount);

    /**
     * Finds a particular {@link StsAccount} by its (database-)ID
     *
     * @param id the (database-)ID
     * @return a {@link ServiceResult} containing the account, a failure otherwise.
     */
    ServiceResult<StsAccount> findById(String id);

    /**
     * Queries the storage for a collection of {@link StsAccount} objects that conform to the given {@link QuerySpec}
     *
     * @param querySpec the query
     * @return A collection of accounts, potentially empty. Never null.
     */
    Collection<StsAccount> queryAccounts(QuerySpec querySpec);

    /**
     * Authenticate an {@link StsAccount} given the input secret
     *
     * @param client The client to authenticate
     * @param secret The client secret in input to check
     * @return the successful if authenticated, failure otherwise
     */
    ServiceResult<StsAccount> authenticate(StsAccount client, String secret);


    /**
     * Updates the client secret associated with this {@link StsAccount}. The new secret is stored in the {@link org.eclipse.edc.spi.security.Vault}
     * using the given alias before the account is switched to that alias, and the old secret is removed afterwards. If the alias
     * does not change, the stored secret is replaced. If the new secret is {@code null}, one is generated at random.
     *
     * @param participantContextId The ID of the participant context that owns the account.
     * @param id                   the ID of the {@link StsAccount} to update
     * @param secretAlias          The alias under which the new secret is stored in the {@link org.eclipse.edc.spi.security.Vault}
     * @param newSecret            The new client secret. If null, a new one is generated.
     * @return A result containing the new client secret, or a failure indicating what went wrong. If the account does not
     *         belong to the participant context, the result is "not found".
     */
    ServiceResult<String> updateSecret(String participantContextId, String id, String secretAlias, @Nullable String newSecret);

    /**
     * Updates the client secret associated with this {@link StsAccount} with a secret that is generated at random. See
     * {@link #updateSecret(String, String, String, String)}.
     *
     * @param participantContextId The ID of the participant context that owns the account.
     * @param id                   the ID of the {@link StsAccount} to update
     * @param newAlias             The alias under which the new secret is stored in the {@link org.eclipse.edc.spi.security.Vault}
     * @return A result containing the new client secret, or a failure indicating what went wrong.
     */
    default ServiceResult<String> updateSecret(String participantContextId, String id, String newAlias) {
        return updateSecret(participantContextId, id, newAlias, null);
    }

    /**
     * Returns an {@link StsAccount} by its id
     *
     * @param id id of the client
     * @return the client successful if found, failure otherwise
     */
    ServiceResult<StsAccount> findByClientId(String id);

}
