/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.identityhub.api.featuredetection;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.eclipse.dataspaceconnector.dtos.FeatureDetection;

/**
 * API controller to provide Feature Detection object.
 */
@Tag(name = "FeatureDetection")
@Produces({"application/json"})
@Consumes({"application/json"})
@Path("/featuredetection")
public class FeatureDetectionController {

    @GET
    public FeatureDetection featureDetection() {
        return new FeatureDetection.Builder().build();
    }
}
