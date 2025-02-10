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

package org.eclipse.edc.issuerservice.spi.issuance.rule;

import org.eclipse.edc.issuerservice.spi.issuance.IssuanceContext;
import org.eclipse.edc.spi.result.Result;

/**
 * Evaluates the context according to a rule.
 */
@FunctionalInterface
public interface CredentialRule {

    /**
     * Performs the evaluation.
     */
    Result<Void> evaluate(IssuanceContext context);

}
