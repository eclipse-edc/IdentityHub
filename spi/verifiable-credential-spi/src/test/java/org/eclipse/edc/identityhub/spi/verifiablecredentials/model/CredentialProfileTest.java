/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.spi.verifiablecredentials.model;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC1_0_JWT;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat.VC2_0_JOSE;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialProfile.DCP_PROFILE_VC11;
import static org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialProfile.DCP_PROFILE_VC20;

/**
 * Test class for {@link CredentialProfile}.
 * This class validates the behavior of the {@code formatForProfile} method, ensuring that different profiles
 * are correctly translated into corresponding {@link CredentialFormat} or appropriate error messages.
 */
class CredentialProfileTest {

    @ParameterizedTest
    @ValueSource(strings = { DCP_PROFILE_VC11, DCP_PROFILE_VC20 })
    void formatForProfile_validProfile(String profile) {
        assertThat(CredentialProfile.formatForProfile(profile).succeeded()).isTrue();
        assertThat(CredentialProfile.formatForProfile(profile.toUpperCase()).succeeded()).isTrue();
        assertThat(CredentialProfile.formatForProfile(profile.toLowerCase()).succeeded()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "invalid-profile", "vc20-sl2021/COSE", "VC2-0-LD/JWT" })
    void formatForProfile_unrecognizedProfile(String profile) {
        assertThat(CredentialProfile.formatForProfile(profile).failed()).isTrue();
        assertThat(CredentialProfile.formatForProfile(profile.toUpperCase()).failed()).isTrue();
        assertThat(CredentialProfile.formatForProfile(profile.toLowerCase()).failed()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(CredentialFormat.class)
    void formatForProfile_profileIsFormatString(CredentialFormat format) {
        var r = CredentialProfile.formatForProfile(format.name());
        assertThat(r.succeeded()).isTrue();
        assertThat(r.getContent()).isEqualTo(format);
    }

    @Test
    void profileForFormat_withSupportedCredentialFormat() {
        var r = CredentialProfile.profileForFormat(VC1_0_JWT);
        assertThat(r.succeeded()).isTrue();
        assertThat(r.getContent()).isEqualTo(DCP_PROFILE_VC11);

        var r2 = CredentialProfile.profileForFormat(VC2_0_JOSE);
        assertThat(r2.succeeded()).isTrue();
        assertThat(r2.getContent()).isEqualTo(DCP_PROFILE_VC20);
    }

    @ParameterizedTest
    @EnumSource(mode = EnumSource.Mode.EXCLUDE, value = CredentialFormat.class, names = { "VC1_0_JWT", "VC2_0_JOSE" })
    void profileForFormat_withUnsupportedProfileString(CredentialFormat unsupportedFormat) {
        var r = CredentialProfile.profileForFormat(unsupportedFormat);
        assertThat(r.failed()).isTrue();
    }
}