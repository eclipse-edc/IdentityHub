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

package org.eclipse.edc.issuerservice.issuance.rules;

import org.eclipse.edc.issuerservice.issuance.rules.expression.ExpressionCredentialRuleDefinitionValidator;
import org.eclipse.edc.issuerservice.issuance.rules.expression.ExpressionCredentialRuleFactory;
import org.eclipse.edc.issuerservice.spi.issuance.rule.CredentialRuleDefinitionValidatorRegistry;
import org.eclipse.edc.issuerservice.spi.issuance.rule.CredentialRuleFactoryRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static org.eclipse.edc.issuerservice.issuance.rules.IssuanceRulesExtension.NAME;


@Extension(NAME)
public class IssuanceRulesExtension implements ServiceExtension {

    public static final String NAME = "Issuance Rules Extension";

    @Inject
    private CredentialRuleFactoryRegistry registry;

    @Inject
    private CredentialRuleDefinitionValidatorRegistry validatorRegistry;

    @Override
    public void initialize(ServiceExtensionContext context) {
        registry.registerFactory("expression", new ExpressionCredentialRuleFactory());
        validatorRegistry.registerValidator("expression", new ExpressionCredentialRuleDefinitionValidator());
    }
}
