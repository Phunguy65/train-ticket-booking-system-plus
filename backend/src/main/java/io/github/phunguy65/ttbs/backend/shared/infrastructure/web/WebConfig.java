package io.github.phunguy65.ttbs.backend.shared.infrastructure.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.accept.PathApiVersionResolver;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final PathApiVersionResolver API_VERSION_RESOLVER =
            new PathApiVersionResolver(1);

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(
                "/api",
                c -> c.isAnnotationPresent(RestController.class)
                        && c.getPackageName().startsWith("io.github.phunguy65.ttbs.backend"));
    }

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer
                .useVersionResolver(request -> isVersionedApiPath(request.getRequestURI())
                        ? API_VERSION_RESOLVER.resolveVersion(request)
                        : null)
                .addSupportedVersions("1.0")
                .setDefaultVersion("1.0")
                .setVersionParser(new ApiVersionParser());
    }

    private static boolean isVersionedApiPath(String path) {
        return path.startsWith("/api/v");
    }
}
