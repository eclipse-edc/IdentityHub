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

package org.eclipse.edc.identityhub.spi.credentials.transformer;

import org.eclipse.edc.identityhub.spi.credentials.model.CredentialEnvelope;
import org.eclipse.edc.identityhub.spi.processor.MessageProcessor;
import org.eclipse.edc.spi.result.Result;

/**
 * The {@link MessageProcessor} is able to process messages that could contain data. If data available in the current implementation
 * we want to be sure that the data is in the correct format based on the Media type.
 */
public interface CredentialEnvelopeTransformer<T extends CredentialEnvelope> {


    /**
     * Validate the input data
     *
     * @param data Input
     * @return The result of the validation
     */
    Result<T> parse(byte[] data);


    Result<byte[]> serialize(T carrier);


    /**
     * Returns the Media type that implementor of {@link CredentialEnvelopeTransformer} is able to validate.
     */
    String dataFormat();

}
