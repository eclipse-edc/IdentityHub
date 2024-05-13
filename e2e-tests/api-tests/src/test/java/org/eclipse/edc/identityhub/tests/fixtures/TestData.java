/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.tests.fixtures;

import org.eclipse.edc.junit.testfixtures.TestUtils;

public interface TestData {
    // taken from https://www.w3.org/TR/vc-data-model/#example-a-simple-example-of-a-verifiable-credential
    String VC_EXAMPLE = TestUtils.getResourceFileContentAsString("vc_example_1.json");

    // this VC is
    String VC_EXAMPLE_2 = TestUtils.getResourceFileContentAsString("vc_example_2.json");
}
