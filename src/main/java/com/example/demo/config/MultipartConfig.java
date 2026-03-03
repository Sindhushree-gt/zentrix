package com.example.demo.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MultipartConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> {
            // Use both context and connector customizers to be absolutely sure
            factory.addContextCustomizers(context -> {
                // Increase the max file count (total parts) to 2000
                context.setMaxFileCount(2000L);
            });
            
            factory.addConnectorCustomizers(connector -> {
                // Ensure maxParameterCount is also high
                connector.setMaxParameterCount(5000);
            });
        };
    }
}
