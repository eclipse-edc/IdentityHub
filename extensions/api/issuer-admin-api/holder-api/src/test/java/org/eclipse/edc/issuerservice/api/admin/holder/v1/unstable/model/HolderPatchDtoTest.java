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

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class HolderPatchDtoTest {

    @Test
    void propertiesAndRemoved_whenNull_shouldBeEmpty() {
        var patch = new HolderPatchDto("did:web:holder", null, null, null);

        assertThat(patch.properties()).isEmpty();
        assertThat(patch.removed()).isEmpty();
    }

    @Test
    void validate() {
        assertThat(new HolderPatchDto("did:web:holder", null, null, null).validate()).isSucceeded();
        assertThat(new HolderPatchDto("did:web:holder", "name", Map.of("foo", "bar"), Set.of("baz")).validate()).isSucceeded();
    }

    @Test
    void validate_whenDidMissing() {
        assertThat(new HolderPatchDto(" ", null, null, null).validate()).isFailed()
                .detail().contains("did");
    }

    @Test
    void validate_whenPropertySetAndRemoved() {
        assertThat(new HolderPatchDto("did:web:holder", null, Map.of("foo", "bar"), Set.of("foo")).validate()).isFailed()
                .detail().contains("foo");
    }

    @Test
    void validate_whenPropertyNull() {
        var properties = new HashMap<String, Object>();
        properties.put("foo", null);

        assertThat(new HolderPatchDto("did:web:holder", null, properties, null).validate()).isFailed()
                .detail().contains("'removed'");
    }
}
