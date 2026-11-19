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

package org.eclipse.edc.issuerservice.api.admin.credentials.v2alpha;

import org.eclipse.edc.identityhub.api.Versions;
import org.eclipse.edc.issuerservice.api.admin.credentials.v1.unstable.IssuerCredentialsAdminApiControllerTest;

class IssuerCredentialsAdminApiV2AlphaControllerTest extends IssuerCredentialsAdminApiControllerTest {

    @Override
    protected String versionPath() {
        return Versions.UNSTABLE;
    }

    @Override
    protected Object controller() {
        return new IssuerCredentialsAdminApiV2AlphaController(authorizationService, credentialStatusService, credentialOfferService);
    }
}
