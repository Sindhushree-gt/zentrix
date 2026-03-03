package com.example.demo.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MultipartConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> customizer() {
        return factory -> {
            factory.addContextCustomizers(context -> {
                // This is the critical fix for FileCountLimitExceededException in Tomcat 10/11
                // It sets the maximum number of permitted parts (files + form fields) in a multipart request
                context.setMaxFileCount(100L);
            });
        };
    }
}
