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

package org.eclipse.edc.issuerservice.spi.issuance.process;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Returns true if an external approval is required to issue the credential batch.
 */
public interface ExternalApprovalPredicate extends Predicate<ExternalApprovalPredicate.ProcessRecord> {
    record ProcessRecord(String issuanceId, Set<String> credentialTypes) {
    }

}
