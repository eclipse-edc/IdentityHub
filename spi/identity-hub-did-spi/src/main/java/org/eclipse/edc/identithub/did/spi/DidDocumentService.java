/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.identithub.did.spi;

import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.identithub.did.spi.model.DidState;
import org.eclipse.edc.spi.result.ServiceResult;

/**
 * The {@code DidDocumentService} gives access to a {@link DidDocument} that are held in storage.
 */
public interface DidDocumentService {

    /**
     * Retrieves the {@link DidDocument} associated with the given DID, if it exists.
     *
     * @param did The DID for which to retrieve the DidDocument.
     * @return A {@link ServiceResult} containing the DidDocument if it exists, or an error if it does not exist or cannot be retrieved.
     */
    ServiceResult<DidDocument> getDidDocument(String did);

    /**
     * Retrieves the state of a DID resource.
     *
     * @param did The identifier of the DID resource.
     * @return A {@link ServiceResult} containing the state of the DID resource if it exists.
     */
    ServiceResult<DidState> getState(String did);
}
