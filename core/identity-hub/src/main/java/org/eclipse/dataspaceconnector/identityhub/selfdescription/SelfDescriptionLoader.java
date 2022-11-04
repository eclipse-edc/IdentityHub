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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.spi.EdcException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static java.lang.String.format;

public class SelfDescriptionLoader {

    private final ObjectMapper mapper;

    public SelfDescriptionLoader(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Load Self-Description from a provided file path.
     *
     * @param path Path to the Self-Description to be loaded.
     * @return JSON representation of the Self-Description.
     */
    public JsonNode fromFile(String path) {
        try (var is = Files.newInputStream(Path.of(path))) {
            return mapper.readTree(is);
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    /**
     * Load Self-Description from the classpath.
     *
     * @param fileName Name of the classpath file from which Self-Description will be loaded.
     * @return JSON representation of the Self-Description.
     */
    public JsonNode fromClasspath(String fileName) {
        try (var is = this.getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (is == null) {
                throw new NoSuchFileException(format("Cannot find file `%s` from classpath", fileName));
            }
            return mapper.readTree(is);
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }
}
