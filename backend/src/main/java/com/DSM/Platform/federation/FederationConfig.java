package com.DSM.Platform.federation;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FederationConfig {

    /**
     * Short timeouts so one slow peer can't stall a sync run. Applied as a
     * customizer (not inside FederationClient) so tests can still swap the
     * request factory with MockRestServiceServer.
     */
    @Bean
    public RestClientCustomizer federationTimeoutCustomizer(FederationProperties properties) {
        return builder -> builder.requestFactory(ClientHttpRequestFactories.get(
                ClientHttpRequestFactorySettings.DEFAULTS
                        .withConnectTimeout(properties.getConnectTimeout())
                        .withReadTimeout(properties.getReadTimeout())
        ));
    }
}
