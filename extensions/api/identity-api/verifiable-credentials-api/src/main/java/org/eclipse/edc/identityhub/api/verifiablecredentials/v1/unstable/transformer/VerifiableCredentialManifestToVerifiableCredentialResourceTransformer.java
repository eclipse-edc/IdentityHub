/*
 *  Copyright (c) 2024 Amadeus IT Group.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus IT Group - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.api.verifiablecredentials.v1.unstable.transformer;

import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VcStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialManifest;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.edc.transform.spi.TypeTransformer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VerifiableCredentialManifestToVerifiableCredentialResourceTransformer implements TypeTransformer<VerifiableCredentialManifest, VerifiableCredentialResource> {

    @Override
    public Class<VerifiableCredentialManifest> getInputType() {
        return VerifiableCredentialManifest.class;
    }

    @Override
    public Class<VerifiableCredentialResource> getOutputType() {
        return VerifiableCredentialResource.class;
    }

    @Override
    public @Nullable VerifiableCredentialResource transform(@NotNull VerifiableCredentialManifest manifest, @NotNull TransformerContext transformerContext) {
        var container = manifest.getVerifiableCredentialContainer();
        return VerifiableCredentialResource.Builder.newInstance()
                .id(manifest.getId())
                .participantContextId(manifest.getParticipantContextId())
                .issuerId(container.credential().getIssuer().id())
                .holderId(container.credential().getCredentialSubject().stream().findFirst().get().getId())
                .state(VcStatus.ISSUED)
                .issuancePolicy(manifest.getIssuancePolicy())
                .reissuancePolicy(manifest.getReissuancePolicy())
                .credential(container)
                .build();
    }
}
