package io.github.phunguy65.ttbs.backend.shared.infrastructure.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(
                "/api",
                c -> c.isAnnotationPresent(RestController.class)
                        && c.getPackageName().startsWith("io.github.phunguy65.ttbs.backend"));
    }

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer.setVersionParser(new ApiVersionParser());
    }
}
