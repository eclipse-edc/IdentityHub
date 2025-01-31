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

    String JWT_VC_EXAMPLE = """
            eyJraWQiOiJFeEhrQk1XOWZtYmt2VjI2Nm1ScHVQMnNVWV9OX0VXSU4xbGFwVXpPOHJvIiwiYWxnIjoiRVMyNTYifQ.eyJAY29
            udGV4dCI6WyJodHRwczovL3d3dy53My5vcmcvbnMvY3JlZGVudGlhbHMvdjIiLCJodHRwczovL3d3dy53My5vcmcvbnMvY3JlZ
            GVudGlhbHMvZXhhbXBsZXMvdjIiXSwiaWQiOiJodHRwOi8vdW5pdmVyc2l0eS5leGFtcGxlL2NyZWRlbnRpYWxzLzM3MzIiLCJ
            0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiRXhhbXBsZURlZ3JlZUNyZWRlbnRpYWwiLCJFeGFtcGxlUGVyc29uQ3JlZ
            GVudGlhbCJdLCJpc3N1ZXIiOiJodHRwczovL3VuaXZlcnNpdHkuZXhhbXBsZS9pc3N1ZXJzLzE0IiwidmFsaWRGcm9tIjoiMjA
            xMC0wMS0wMVQxOToyMzoyNFoiLCJjcmVkZW50aWFsU3ViamVjdCI6eyJpZCI6ImRpZDpleGFtcGxlOmViZmViMWY3MTJlYmM2Z
            jFjMjc2ZTEyZWMyMSIsImRlZ3JlZSI6eyJ0eXBlIjoiRXhhbXBsZUJhY2hlbG9yRGVncmVlIiwibmFtZSI6IkJhY2hlbG9yIG9
            mIFNjaWVuY2UgYW5kIEFydHMifSwiYWx1bW5pT2YiOnsibmFtZSI6IkV4YW1wbGUgVW5pdmVyc2l0eSJ9fSwiY3JlZGVudGlhb
            FNjaGVtYSI6W3siaWQiOiJodHRwczovL2V4YW1wbGUub3JnL2V4YW1wbGVzL2RlZ3JlZS5qc29uIiwidHlwZSI6Ikpzb25TY2h
            lbWEifSx7ImlkIjoiaHR0cHM6Ly9leGFtcGxlLm9yZy9leGFtcGxlcy9hbHVtbmkuanNvbiIsInR5cGUiOiJKc29uU2NoZW1hI
            n1dfQ.ZO5PfcjJ7aq-FFgFnvKF4irU-4Cv6_zLTGt7t7rVhb_K-veUd3XKTNbfiS_RtedrNYgay8PksZfTnkk2gnNFSw
            """;
}
