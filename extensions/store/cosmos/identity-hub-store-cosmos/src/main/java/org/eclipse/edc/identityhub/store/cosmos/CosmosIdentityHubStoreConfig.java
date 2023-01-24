/*
 *  Copyright (c) 2023 Amadeus
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

package org.eclipse.edc.identityhub.store.cosmos;

import org.eclipse.edc.azure.cosmos.AbstractCosmosConfig;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

public class CosmosIdentityHubStoreConfig extends AbstractCosmosConfig {

    @Setting(required = true, value = "CosmosDB account name for IdentityHub Store")
    public static final String COSMOS_ACCOUNTNAME_SETTING = "edc.identityhubstore.cosmos.account-name";
    @Setting(required = true, value = "CosmosDB database name for IdentityHub Store")
    public static final String COSMOS_DBNAME_SETTING = "edc.identityhubstore.cosmos.database-name";
    @Setting(value = "CosmosDB preferred region for IdentityHub Store")
    public static final String COSMOS_PREFERRED_REGION_SETTING = "edc.identityhubstore.cosmos.preferred-region";
    @Setting(value = "CosmosDB container name for IdentityHub Store")
    public static final String COSMOS_CONTAINER_NAME_SETTING = "edc.identityhubstore.cosmos.container-name";

    /**
     * Create a config object to interact with a Cosmos database.
     *
     * @param context Service extension context
     */
    protected CosmosIdentityHubStoreConfig(ServiceExtensionContext context) {
        super(context);
    }

    @Override
    protected String getAccountNameSetting() {
        return COSMOS_ACCOUNTNAME_SETTING;
    }

    @Override
    protected String getDbNameSetting() {
        return COSMOS_DBNAME_SETTING;
    }

    @Override
    protected String getCosmosPreferredRegionSetting() {
        return COSMOS_PREFERRED_REGION_SETTING;
    }

    @Override
    protected String getContainerNameSetting() {
        return COSMOS_CONTAINER_NAME_SETTING;
    }
}
