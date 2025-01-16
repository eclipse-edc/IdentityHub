/*
 *  Copyright (c) 2023 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.publisher.did.local;

import org.eclipse.edc.identityhub.spi.did.DidConstants;
import org.eclipse.edc.identityhub.spi.did.DidDocumentPublisherRegistry;
import org.eclipse.edc.identityhub.spi.did.DidWebParser;
import org.eclipse.edc.identityhub.spi.did.events.DidDocumentObservable;
import org.eclipse.edc.identityhub.spi.did.store.DidResourceStore;
import org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.PortMapping;
import org.eclipse.edc.web.spi.configuration.PortMappingRegistry;

import java.time.Clock;

import static org.eclipse.edc.identityhub.publisher.did.local.LocalDidPublisherExtension.NAME;
import static org.eclipse.edc.identityhub.spi.webcontext.IdentityHubApiContext.IH_DID;

@Extension(value = NAME)
public class LocalDidPublisherExtension implements ServiceExtension {

    public static final String NAME = "Local DID publisher extension";
    private static final String DEFAULT_DID_PATH = "/";
    private static final int DEFAULT_DID_PORT = 10100;

    @Configuration
    private DidApiConfiguration apiConfiguration;

    @Inject
    private DidDocumentPublisherRegistry registry;
    @Inject
    private DidResourceStore didResourceStore;
    @Inject
    private WebService webService;
    @Inject
    private PortMappingRegistry portMappingRegistry;
    /**
     * Allow extensions to contribute their own DID WEB parser, in case some special url modification is needed.
     */
    @Inject(required = false)
    private DidWebParser didWebParser;
    @Inject
    private Clock clock;
    @Inject
    private EventRouter eventRouter;

    private DidDocumentObservableImpl observable;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        portMappingRegistry.register(new PortMapping(IH_DID, apiConfiguration.port(), apiConfiguration.path()));
        var localPublisher = new LocalDidPublisher(didDocumentObservable(), didResourceStore, context.getMonitor());
        registry.addPublisher(DidConstants.DID_WEB_METHOD, localPublisher);
        webService.registerResource(IdentityHubApiContext.IH_DID, new DidWebController(context.getMonitor(), didResourceStore, getDidParser()));
    }

    @Provider
    public DidDocumentObservable didDocumentObservable() {
        if (observable == null) {
            observable = new DidDocumentObservableImpl();
            observable.registerListener(new DidDocumentListenerImpl(clock, eventRouter));
        }
        return observable;
    }

    private DidWebParser getDidParser() {
        return didWebParser != null ? didWebParser : new DidWebParser();
    }

    @Settings
    record DidApiConfiguration(
            @Setting(key = "web.http." + IH_DID + ".port", description = "Port for " + IH_DID + " api context", defaultValue = DEFAULT_DID_PORT + "")
            int port,
            @Setting(key = "web.http." + IH_DID + ".path", description = "Path for " + IH_DID + " api context", defaultValue = DEFAULT_DID_PATH)
            String path
    ) {

    }
}
