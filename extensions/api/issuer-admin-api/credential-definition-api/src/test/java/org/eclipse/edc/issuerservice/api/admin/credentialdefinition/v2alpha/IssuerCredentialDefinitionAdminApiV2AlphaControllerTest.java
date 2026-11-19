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

import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.issuerservice.api.admin.credentialdefinition.v1.unstable.IssuerCredentialDefinitionAdminApiControllerTest;
import org.eclipse.edc.junit.annotations.ComponentTest;

@ComponentTest
class IssuerCredentialDefinitionAdminApiV2AlphaControllerTest extends IssuerCredentialDefinitionAdminApiControllerTest {

    @Override
    protected String versionPath() {
        return Versions.UNSTABLE;
    }

    @Override
    protected Object controller() {
        return new IssuerCredentialDefinitionAdminApiV2AlphaController(authorizationService, credentialDefinitionService, issuerProfile);
    }
}
