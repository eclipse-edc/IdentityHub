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

package org.eclipse.edc.issuerservice.api.admin.credentials.v1.unstable.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;

/**
 * The credentials an Issuer offers to a Holder.
 *
 * @param offerReason why the credential is offered, e.g. {@code reissue} or {@code proof-key-revocation}. Optional;
 *                    defaults to {@code reissue}.
 */
public record CredentialOfferDto(@JsonProperty(required = true) String holderId,
                                 @JsonProperty(required = true) Collection<String> credentials,
                                 String offerReason) {
}
