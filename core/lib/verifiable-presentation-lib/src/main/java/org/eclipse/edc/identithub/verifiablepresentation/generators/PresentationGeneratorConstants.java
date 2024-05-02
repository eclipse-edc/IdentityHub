/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
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

package org.eclipse.edc.identithub.verifiablepresentation.generators;

/**
 * Contains common constants for {@link LdpPresentationGenerator} and {@link JwtPresentationGenerator}.
 */
public interface PresentationGeneratorConstants {

    String CONTROLLER_ADDITIONAL_DATA = "controller";

    String VP_TYPE_PROPERTY = "type";

    String VERIFIABLE_CREDENTIAL_PROPERTY = "verifiableCredential";

}
