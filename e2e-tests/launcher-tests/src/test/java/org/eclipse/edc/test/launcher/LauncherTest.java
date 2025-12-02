/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.test.launcher;

import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.File;
import java.io.IOException;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.eclipse.edc.junit.testfixtures.TestUtils.findBuildRoot;
import static org.testcontainers.containers.wait.strategy.Wait.forHealthcheck;

@EndToEndTest
public class LauncherTest {

    @ParameterizedTest
    @ValueSource(strings = {"identityhub", "identityhub-oauth2", "issuer-service"})
    void shouldBuildAndRun(String launcherName) throws IOException, InterruptedException {
        var file = new File(TestUtils.findBuildRoot(), TestUtils.GRADLE_WRAPPER);
        var command = new String[]{file.getCanonicalPath(), ":launcher:" + launcherName + ":shadowJar"};

        var exec = Runtime.getRuntime().exec(command).waitFor(1, MINUTES);
        assertThat(exec).isTrue();

        var dockerfile = findBuildRoot().toPath().resolve("launcher").resolve(launcherName).resolve("Dockerfile");
        var runtime = new GenericContainer<>(new ImageFromDockerfile().withDockerfile(dockerfile))
                .withEnv("edc.issuer.statuslist.signing.key.alias", "any")
                .withEnv("edc.iam.oauth2.jwks.url", "https://example.com/jwks.jsons") // oauth2 launcher uses this
                .waitingFor(forHealthcheck())
                .withLogConsumer(f -> System.out.println(f.getUtf8StringWithoutLineEnding()));

        assertThatNoException().isThrownBy(() -> {
            runtime.start();

            runtime.stop();
        });

    }
}
