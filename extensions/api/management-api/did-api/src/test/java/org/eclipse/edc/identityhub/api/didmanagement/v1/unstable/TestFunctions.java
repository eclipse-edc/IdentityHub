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

package org.eclipse.edc.identityhub.api.didmanagement.v1.unstable;

import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.Service;
import org.eclipse.edc.iam.did.spi.document.VerificationMethod;

import java.util.List;

public class TestFunctions {
    public static DidDocument.Builder createDidDocument() {
        return DidDocument.Builder.newInstance()
                .id("did:web:testdid")
                .service(List.of(new Service("test-service", "test-service", "https://test.service.com/")))
                .verificationMethod(List.of(VerificationMethod.Builder.newInstance()
                        .id("did:web:testdid#key-1")
                        .publicKeyMultibase("saflasjdflaskjdflasdkfj")
                        .build()));
    }
}
