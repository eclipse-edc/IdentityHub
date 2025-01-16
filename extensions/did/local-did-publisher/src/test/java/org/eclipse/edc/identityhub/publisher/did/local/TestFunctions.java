/*
 *  Copyright (c) 2023 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.publisher.did.local;

import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.identityhub.spi.did.model.DidResource;
import org.eclipse.edc.identityhub.spi.did.model.DidState;

public interface TestFunctions {
    static DidResource.Builder createDidResource() {
        return createDidResource("did:web:test");
    }

    static DidResource.Builder createDidResource(String did) {
        return DidResource.Builder.newInstance()
                .did(did)
                .state(DidState.GENERATED)
                .document(DidDocument.Builder.newInstance()
                        .id(did)
                        .build())
                .state(DidState.INITIAL);
    }
}
