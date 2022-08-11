/*
 *  Copyright (c) 2022 Amadeus
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

package org.eclipse.dataspaceconnector.identityhub.selfdescription;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class SelfDescriptionLoaderTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final SelfDescriptionLoader loader = new SelfDescriptionLoader(OBJECT_MAPPER);

    @Test
    void fromFile() throws IOException {
        var path = "src/test/resources/self-description.json";
        var expected = loadJsonFile(path);

        var result = loader.fromFile(path);

        assertThat(result).usingRecursiveFieldByFieldElementComparator().isEqualTo(expected);
    }

    @Test
    void fromFile_fileNotFound() {
        assertThatExceptionOfType(EdcException.class).isThrownBy(() -> loader.fromFile("src/test/resources/invalid-self-description.json"))
                .withRootCauseExactlyInstanceOf(NoSuchFileException.class);
    }

    @Test
    void fromFile_invalidJson() {
        assertThatExceptionOfType(EdcException.class).isThrownBy(() -> loader.fromFile("src/test/resources/invalid-self-description.txt"))
                .withRootCauseExactlyInstanceOf(JsonParseException.class);
    }

    @Test
    void fromClasspath() throws IOException {
        var expected = loadJsonFile("src/main/resources/default-self-description.json");

        var result = loader.fromClasspath("default-self-description.json");

        assertThat(result).usingRecursiveFieldByFieldElementComparator().isEqualTo(expected);
    }

    @Test
    void fromClasspath_fileNotFound() {
        assertThatExceptionOfType(EdcException.class).isThrownBy(() -> loader.fromClasspath("default-self-descriptionxx.json"))
                .withRootCauseExactlyInstanceOf(NoSuchFileException.class);
    }

    private static JsonNode loadJsonFile(String path) throws IOException {
        var content = Files.readString(Path.of(path));
        return OBJECT_MAPPER.readTree(content);
    }
}