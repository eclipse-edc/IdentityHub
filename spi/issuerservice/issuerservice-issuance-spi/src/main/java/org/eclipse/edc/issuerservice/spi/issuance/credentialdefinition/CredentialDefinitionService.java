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

package org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition;

import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.Collection;

/**
 * Service interface for managing credential definitions.
 */
@ExtensionPoint
public interface CredentialDefinitionService {

    /**
     * Creates a new credential definition.
     *
     * @param credentialDefinition the credential definition to create
     * @return a service result indicating success or failure
     */
    ServiceResult<Void> createCredentialDefinition(CredentialDefinition credentialDefinition);

    /**
     * Deletes a credential definition.
     *
     * @param credentialDefinitionId the ID of the credential definition to delete
     * @return a service result indicating success or failure
     */
    ServiceResult<Void> deleteCredentialDefinition(String credentialDefinitionId);

    /**
     * Updates an existing credential definition.
     *
     * @param credentialDefinition the credential definition to update
     * @return a service result indicating success or failure
     */

    ServiceResult<Void> updateCredentialDefinition(CredentialDefinition credentialDefinition);

    /**
     * Queries credential definitions.
     *
     * @param querySpec the query specification
     * @return a service result containing the matching credential definitions
     */
    ServiceResult<Collection<CredentialDefinition>> queryCredentialDefinitions(QuerySpec querySpec);

    /**
     * Finds a credential definition by ID.
     *
     * @param credentialDefinitionId the ID of the credential definition to find
     * @return a service result containing the credential definition, if found
     */
    ServiceResult<CredentialDefinition> findCredentialDefinitionById(String credentialDefinitionId);
}
