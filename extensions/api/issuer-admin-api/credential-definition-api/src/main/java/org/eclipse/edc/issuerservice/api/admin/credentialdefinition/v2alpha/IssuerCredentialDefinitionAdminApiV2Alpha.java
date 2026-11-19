/*
 *  Copyright (c) 2026 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.issuerservice.api.admin.credentialdefinition.v2alpha;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.eclipse.edc.issuerservice.api.admin.credentialdefinition.v1.unstable.IssuerCredentialDefinitionAdminApi;

@OpenAPIDefinition(info = @Info(description = "This API is used to manipulate credential definitions in an Issuer Service", title = "Issuer Service Credential Definitions Admin API", version = "2alpha"))
@Tag(name = "Issuer Service Credential Definition Admin API v2alpha")
public interface IssuerCredentialDefinitionAdminApiV2Alpha extends IssuerCredentialDefinitionAdminApi {
}
