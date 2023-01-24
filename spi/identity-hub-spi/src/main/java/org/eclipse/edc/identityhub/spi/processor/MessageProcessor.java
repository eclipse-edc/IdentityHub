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

package org.eclipse.edc.identityhub.spi.processor;


import org.eclipse.edc.identityhub.spi.model.MessageRequestObject;
import org.eclipse.edc.identityhub.spi.model.MessageResponseObject;
import org.eclipse.edc.identityhub.spi.model.WebNodeInterfaces;

/**
 * <p>
 * Each implementor of the MessageProcessor interface handles a message of a different Decentralized Web Node Interface method.
 * Refer to <a href="https://identity.foundation/decentralized-web-node/spec/#interfaces">the spec</a> for a list of interfaces available.
 * <p>
 * Messages may or may not contain additional data associated with it (when data is desired or required to be present for a given method invocation).
 * The MessageProcessor gets handed over this data in case it is available, or null otherwise.
 * <p>
 * See {@link WebNodeInterfaces} for a list of currently supported interfaces. Currently the only supported interface that accepts data is "CollectionsWrite".
 */
@FunctionalInterface
public interface MessageProcessor {

    /**
     * Processes a message
     *
     * @param requestObject Request object to be processed.
     * @return MessageResponseObject
     */
    MessageResponseObject process(MessageRequestObject requestObject);
}
