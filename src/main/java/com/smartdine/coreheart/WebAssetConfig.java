package com.smartdine.coreheart;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Professional Resource Handler configuration to serve local uploads
 * to other devices on the restaurant floor local network.
 */
@Configuration
public class WebAssetConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Exposes local storage directory C:/smartdine/images/ as web path /images/**
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:/C:/smartdine/images/");
    }
}
