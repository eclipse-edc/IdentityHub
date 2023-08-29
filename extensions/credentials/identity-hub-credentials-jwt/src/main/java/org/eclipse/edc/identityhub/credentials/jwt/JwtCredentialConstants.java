/*
 *  Copyright (c) 2023 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.credentials.jwt;

public interface JwtCredentialConstants {
    
    String VC_DATA_FORMAT = "application/vc+jwt";

    String VP_DATA_FORMAT = "application/vp+ld+jwt";
    String VERIFIABLE_CREDENTIALS_KEY = "vc";

    String VERIFIABLE_PRESENTATION_KEY = "vp";
}
