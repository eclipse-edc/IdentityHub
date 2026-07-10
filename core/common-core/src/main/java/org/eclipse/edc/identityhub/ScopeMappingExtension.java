/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub;

import org.eclipse.edc.identityhub.defaults.ScopeMappingRegistryImpl;
import org.eclipse.edc.identityhub.spi.transformation.ScopeMappingRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.system.ServiceExtension;

import java.util.Map;

import static org.eclipse.edc.identityhub.ScopeMappingExtension.NAME;


@Extension(NAME)
public class ScopeMappingExtension implements ServiceExtension {

    public static final String NAME = "Scope Mapping Extension";

    public static final String CONFIG_PREFIX = "edc.identityhub.scope";
    @Configuration(context = CONFIG_PREFIX)
    private Map<String, ScopeMapping> scopeMappings;

    @Override
    public String name() {
        return NAME;
    }

    @Provider(isDefault = true)
    public ScopeMappingRegistry createScopeMappingRegistry() {
        var scopeMappingRegistry = new ScopeMappingRegistryImpl();
        scopeMappings.forEach((k, v) -> {
            scopeMappingRegistry.addMapping(v.pattern(), new Criterion(v.leftOperand(), v.operator(), v.rightOperand()));
        });

        return scopeMappingRegistry;
    }


    @Settings
    record ScopeMapping(

            @Setting(
                    key = "pattern",
                    description = "The regular expression the scope string is matched against."
            )
            String pattern,

            @Setting(
                    key = "leftoperand",
                    description = "The left operand of the resulting criterion, may reference regex capture groups (e.g. $1)")
            String leftOperand,

            @Setting(
                    key = "operator",
                    description = "The operator of the resulting criterion, e.g. 'contains'")
            String operator,
            @Setting(
                    key = "rightoperand",
                    description = "The right operand of the resulting criterion, may reference regex capture groups (e.g. $1)")
            String rightOperand
    ) {

    }
}
