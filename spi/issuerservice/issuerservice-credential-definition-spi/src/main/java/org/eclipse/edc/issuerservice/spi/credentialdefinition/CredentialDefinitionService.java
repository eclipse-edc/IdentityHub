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

package org.eclipse.edc.issuerservice.spi.credentialdefinition;

import org.eclipse.edc.identityhub.spi.issuance.credentials.model.CredentialDefinition;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.Collection;

public interface CredentialDefinitionService {

    ServiceResult<Void> createCredentialDefinition(CredentialDefinition credentialDefinition);

    ServiceResult<Void> deleteCredentialDefinition(String credentialDefinitionId);

    ServiceResult<Void> updateCredentialDefinition(CredentialDefinition credentialDefinition);

    ServiceResult<Collection<CredentialDefinition>> queryCredentialDefinitions(QuerySpec querySpec);

    ServiceResult<CredentialDefinition> findCredentialDefinitionById(String credentialDefinitionId);
}
