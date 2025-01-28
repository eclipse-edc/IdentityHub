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

package org.eclipse.edc.issuerservice.api.administration.v1.unstable.model;

import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;

/**
 * a key rotation request to be processed by the issuer
 *
 * @param timeToDisable time before the old key gets disabled (= public key removed from the DID Document) in days.
 *                      Set to <=0 for immediate removal.
 * @param timeToLive    lifetime of the new key in days. Accepted range is [7..730] (1 week - 2 yrs)
 * @param newKey        Description of the new key
 */
public record RotationRequest(long timeToDisable, long timeToLive, KeyDescriptor newKey) {
}
