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

package org.eclipse.edc.issuerservice.issuance.issuance;

import org.eclipse.edc.issuerservice.issuance.IssuanceServicesExtension;
import org.eclipse.edc.issuerservice.issuance.attestation.AttestationDefinitionServiceImpl;
import org.eclipse.edc.issuerservice.issuance.credentialdefinition.CredentialDefinitionServiceImpl;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(DependencyInjectionExtension.class)

public class IssuanceServicesExtensionTest {

    @Test
    void verifyDefaultServices(IssuanceServicesExtension extension) {
        assertThat(extension.createParticipantService()).isInstanceOf(CredentialDefinitionServiceImpl.class);
        assertThat(extension.createAttestationService()).isInstanceOf(AttestationDefinitionServiceImpl.class);
    }

}
