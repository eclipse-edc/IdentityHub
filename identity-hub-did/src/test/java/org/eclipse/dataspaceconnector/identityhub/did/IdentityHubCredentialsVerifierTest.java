package org.eclipse.dataspaceconnector.identityhub.did;
/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.identityhub.client.IdentityHubClient;
import org.eclipse.dataspaceconnector.identityhub.client.IdentityHubClientImpl;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;

public class IdentityHubCredentialsVerifierTest {

    @Test
    public void getVerifiedClaims_getValidClaims() {

    }

    @Test
    public void getVerifiedClaims_filtersSignedByWrongIssuer() {

    }

    @Test
    public void getVerifiedClaims_filtersClaimsWithWrongFormat() {

    }

}
