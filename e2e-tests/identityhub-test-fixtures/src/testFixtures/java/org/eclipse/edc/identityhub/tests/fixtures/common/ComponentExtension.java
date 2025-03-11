/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.tests.fixtures.common;

import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.spi.system.configuration.Config;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Base class for all component extensions. It requires a {@link RuntimePerClassExtension} which means
 * it the runtime will be created once per test class. It takes in input the modules and additional configurations that are required
 * for the runtime to be created. Implementors should provide the default configuration for the runtime.
 * It also supports injecting services mocks tagged with {@link Named} in case there are multiple
 * extensions of the same type.
 */
public abstract class ComponentExtension extends RuntimePerClassExtension {

    protected Duration interval = Duration.ofSeconds(1);
    protected Duration timeout = Duration.ofSeconds(60);
    protected String id;
    protected String name;

    protected ComponentExtension(EmbeddedRuntime runtime) {
        super(runtime);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public EmbeddedRuntime getRuntime() {
        return runtime;
    }

    public Duration getInterval() {
        return interval;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public abstract Config getConfiguration();


    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        if (isParameterSupported(parameterContext, this.getClass())) {
            return true;
        }
        return super.supportsParameter(parameterContext, extensionContext) && matchName(parameterContext);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Type type = parameterContext.getParameter().getParameterizedType();
        if (type.equals(this.getClass())) {
            return this;
        }
        return super.resolveParameter(parameterContext, extensionContext);
    }

    protected boolean matchName(ParameterContext parameterContext) {
        return parameterContext.findAnnotation(Named.class)
                .map(Named::value)
                .map(name -> name.equals(getName()))
                .orElse(true);
    }

    protected boolean isParameterSupported(ParameterContext parameterContext, Class<?> target) {
        Type type = parameterContext.getParameter().getParameterizedType();

        return type.equals(target) && matchName(parameterContext);
    }

    public abstract static class Builder<P extends ComponentExtension, B extends Builder<P, B>> {

        protected final List<Supplier<Config>> configurationProviders = new ArrayList<>();
        private final Map<Class<?>, Object> serviceMocks = new LinkedHashMap<>();
        protected String id;
        protected String name;
        protected List<String> modules = new ArrayList<>();
        protected EmbeddedRuntime runtime;
        protected Duration interval = Duration.ofSeconds(1);
        protected Duration timeout = Duration.ofSeconds(60);

        protected Builder() {
        }

        public B id(String id) {
            this.id = id;
            return self();
        }

        public B name(String name) {
            this.name = name;
            return self();
        }

        public B timeout(Duration timeout) {
            this.timeout = timeout;
            return self();
        }

        public B interval(Duration interval) {
            this.interval = interval;
            return self();
        }

        public B module(String module) {
            this.modules.add(module);
            return self();
        }

        public B modules(String... modules) {
            this.modules.addAll(Arrays.stream(modules).toList());
            return self();
        }

        public B runtime(EmbeddedRuntime runtime) {
            this.runtime = runtime;
            return self();
        }

        public B configurationProvider(Supplier<Config> configurationProvider) {
            this.configurationProviders.add(configurationProvider);
            return self();
        }

        protected abstract P internalBuild();

        public P build() {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(name, "name");

            if (runtime == null) {
                runtime = new EmbeddedRuntime(name, modules.toArray(new String[0]));
            }
            var extension = internalBuild();
            extension.id = id;
            extension.name = name;
            extension.timeout = timeout;
            extension.interval = interval;
            extension.runtime.configurationProvider(extension::getConfiguration);
            configurationProviders.forEach(extension.runtime::configurationProvider);

            return extension;
        }

        @SuppressWarnings("unchecked")
        protected B self() {
            return (B) this;
        }
    }
}