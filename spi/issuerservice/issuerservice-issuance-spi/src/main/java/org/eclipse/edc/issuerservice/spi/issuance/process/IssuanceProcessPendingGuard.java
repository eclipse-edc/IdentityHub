/*
 *  Copyright (c) 2025 Nederlandse Organisatie voor toegepast-natuurwetenschappelijk onderzoek (TNO)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Nederlandse Organisatie voor toegepast-natuurwetenschappelijk onderzoek (TNO) - initial API and implementation
 *
 */

package org.eclipse.edc.issuerservice.spi.issuance.process;

import org.eclipse.edc.issuerservice.spi.issuance.model.IssuanceProcess;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.entity.PendingGuard;

@FunctionalInterface
@ExtensionPoint
public interface IssuanceProcessPendingGuard extends PendingGuard<IssuanceProcess> {
}
