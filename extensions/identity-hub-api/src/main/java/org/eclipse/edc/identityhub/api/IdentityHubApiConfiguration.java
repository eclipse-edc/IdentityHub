package org.eclipse.edc.identityhub.api;

import org.eclipse.edc.web.spi.configuration.WebServiceConfiguration;

public class IdentityHubApiConfiguration extends WebServiceConfiguration {
    public IdentityHubApiConfiguration(WebServiceConfiguration webServiceConfig) {
        contextAlias = webServiceConfig.getContextAlias();
        path = webServiceConfig.getPath();
        port = webServiceConfig.getPort();
    }
}
