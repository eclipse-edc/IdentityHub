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
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.Arrays;
import java.util.List;

import static org.eclipse.edc.spi.result.ServiceResult.success;

/**
 * The {@code CredentialProfile} class provides functionality to map DCP credential profiles
 * to their corresponding credential formats and vice versa. It acts as a utility class
 * to help validate and translate profile/format pairings.
 * <p>
 * It defines constant values for supported DCP credential profiles and includes methods
 * for format/profile conversions and validation logic for expected inputs.
 * <p>
 * All currently defined DCP profiles are listed below:
 * <ul>
 *     <li>{@code vc11-sl2021/jwt}</li>
 *     <li>{@code vc20-bssl/jwt}</li>
 * </ul>
 * </p>
 * <p>
 * For ore information refer to the <a href="https://eclipse-dataspace-dcp.github.io/decentralized-claims-protocol/v1.0.1/#dcp-profile-definitions">DCP Specification</a>
 */
public class CredentialProfile {
    public static final String DCP_PROFILE_VC11 = "vc11-sl2021/jwt";
    public static final String DCP_PROFILE_VC20 = "vc20-bssl/jwt";
    private static final List<String> VALID_CREDENTIAL_FORMATS = Arrays.stream(CredentialFormat.values()).map(Object::toString).toList();

    /**
     * Converts a DCP credential profile to its corresponding credential format. If the profile is not recognized, an attempt
     * is made to convert it to a {@link CredentialFormat} enum value. If the conversion is unsuccessful, an error is returned.
     *
     * @param profile A DCP credential profile string.
     * @return A {@link ServiceResult} containing the corresponding {@link CredentialFormat} or an error if the conversion fails.
     */
    public static ServiceResult<CredentialFormat> formatForProfile(String profile) {
        switch (profile.toLowerCase()) {
            case DCP_PROFILE_VC11:
                return success(CredentialFormat.VC1_0_JWT);
            case DCP_PROFILE_VC20:
                return success(CredentialFormat.VC2_0_JOSE);
            default:
                try {
                    return success(CredentialFormat.valueOf(profile));
                } catch (IllegalArgumentException e) {
                    return ServiceResult.badRequest(String.format("Invalid format: '%s', expected one of '%s, '%s' or  %s".formatted(profile, DCP_PROFILE_VC11, DCP_PROFILE_VC20, VALID_CREDENTIAL_FORMATS)));
                }
        }
    }

    /**
     * Returns the DCP profile for a given credential format. If the format falls outside supported DCP profiles, then an error is returned.
     *
     * @param format The credential format.
     * @return A {@link ServiceResult} containing the corresponding DCP profile string or an error if the format is unsupported.
     */
    public static ServiceResult<String> profileForFormat(CredentialFormat format) {
        return switch (format) {
            case VC1_0_JWT -> success(DCP_PROFILE_VC11);
            case VC2_0_JOSE -> success(DCP_PROFILE_VC20);
            default ->
                    ServiceResult.badRequest(String.format("Unsupported format: '%s', expected one of '%s, '%s' or  %s".formatted(format, DCP_PROFILE_VC11, DCP_PROFILE_VC20, VALID_CREDENTIAL_FORMATS)));
        };
    }
}
