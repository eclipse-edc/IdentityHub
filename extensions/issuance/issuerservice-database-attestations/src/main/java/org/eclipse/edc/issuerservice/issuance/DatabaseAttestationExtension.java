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

package org.eclipse.edc.issuerservice.issuance;

import org.eclipse.edc.issuerservice.issuance.database.DatabaseAttestationSourceFactory;
import org.eclipse.edc.issuerservice.issuance.database.DatabaseAttestationSourceValidator;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionValidatorRegistry;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSourceFactoryRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import static org.eclipse.edc.issuerservice.issuance.DatabaseAttestationExtension.NAME;

@Extension(NAME)
public class DatabaseAttestationExtension implements ServiceExtension {

    public static final String NAME = "Database Attestations Extension";
    public static final String DATABASE_ATTESTATION_TYPE = "database";

    @Inject
    private AttestationSourceFactoryRegistry registry;

    @Inject
    private AttestationDefinitionValidatorRegistry validatorRegistry;
    @Inject
    private Vault vault;
    @Inject
    private TransactionContext transactionContext;
    @Inject
    private QueryExecutor queryExecutor;
    @Inject
    private DataSourceRegistry dataSourceRegistry;

    @Override
    public void initialize(ServiceExtensionContext context) {
        registry.registerFactory(DATABASE_ATTESTATION_TYPE, new DatabaseAttestationSourceFactory(transactionContext, queryExecutor, dataSourceRegistry));
        validatorRegistry.registerValidator(DATABASE_ATTESTATION_TYPE, new DatabaseAttestationSourceValidator());
    }
}
