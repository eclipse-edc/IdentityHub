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

import org.jetbrains.annotations.Nullable;

/**
 * Informs about the status of a credential
 *
 * @param credentialId The ID of the credential
 * @param status       A string holding status information, such as "active", "suspended", "revoked", etc.
 * @param reason       A (possibly empty) field to hold a reason for the state for example "voluntary account suspension" or similar
 */
public record CredentialStatusResponse(String credentialId, String status, @Nullable String reason) {
}
