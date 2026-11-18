/*
 *  Copyright (c) 2026 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.issuerservice.api.admin.holder.v1.unstable.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.edc.issuerservice.spi.holder.model.Holder;
import org.eclipse.edc.spi.result.Result;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * A partial update of a {@link Holder}, that changes the holder's properties only. The DID and the name must be equal to
 * those of the stored holder.
 *
 * @param did        the DID of the holder. Cannot be changed.
 * @param name       the name of the holder. Cannot be changed.
 * @param properties entries to add to the holder's properties. The value of an existing key is replaced as a whole.
 * @param removed    keys to remove from the holder's properties. Keys that the holder does not have are ignored.
 */
public record HolderPatchDto(@JsonProperty(value = "did", required = true) String did,
                             @JsonProperty("name") String name,
                             @JsonProperty("properties") Map<String, Object> properties,
                             @JsonProperty("removed") Set<String> removed) {

    public HolderPatchDto {
        properties = properties != null ? properties : Map.of();
        removed = removed != null ? removed : Set.of();
    }

    public Result<Void> validate() {
        var errors = new ArrayList<String>();
        if (did == null || did.isBlank()) {
            errors.add("'did' is mandatory.");
        }
        properties.forEach((key, value) -> {
            if (value == null) {
                errors.add("Property '%s' must not be null. Use 'removed' to remove a property.".formatted(key));
            }
            if (removed.contains(key)) {
                errors.add("Property '%s' cannot be both set and removed.".formatted(key));
            }
        });
        return errors.isEmpty() ? Result.success() : Result.failure(errors);
    }
}
