package io.github.phunguy65.ttbs.backend.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.accept.ApiVersionStrategy;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.util.ServletRequestPathUtils;

class WebConfigTest {

    private final WebConfig webConfig = new WebConfig();

    @Test
    void pathPrefixTargetsBackendRestControllersOnly() {
        TestPathMatchConfigurer configurer = new TestPathMatchConfigurer();

        webConfig.configurePathMatch(configurer);

        Map<String, Predicate<Class<?>>> prefixes = configurer.pathPrefixes();
        assertThat(prefixes).containsOnlyKeys("/api");
        Predicate<Class<?>> predicate = prefixes.get("/api");
        assertThat(predicate.test(SampleController.class)).isTrue();
        assertThat(predicate.test(NonController.class)).isFalse();
    }

    @Test
    void versionStrategyOnlyResolvesVersionedApiPaths() {
        TestApiVersionConfigurer configurer = new TestApiVersionConfigurer();

        webConfig.configureApiVersioning(configurer);

        ApiVersionStrategy strategy = configurer.strategy();
        MockHttpServletRequest versionedRequest =
                new MockHttpServletRequest("GET", "/api/v1/bookings");
        MockHttpServletRequest docsRequest =
                new MockHttpServletRequest("GET", "/v3/api-docs/customer");
        MockHttpServletRequest sseRequest =
                new MockHttpServletRequest("GET", "/api/v1/sse/trips/123/seats");
        ServletRequestPathUtils.parseAndCache(versionedRequest);
        ServletRequestPathUtils.parseAndCache(docsRequest);
        ServletRequestPathUtils.parseAndCache(sseRequest);

        assertThat(strategy.resolveVersion(versionedRequest)).isEqualTo("v1");
        assertThat(strategy.resolveVersion(docsRequest)).isNull();
        assertThat(strategy.resolveVersion(sseRequest)).isEqualTo("v1");
        assertThat(strategy.getDefaultVersion()).isEqualTo(strategy.parseVersion("1.0"));
    }

    @RestController
    static class SampleController {}

    static class NonController {}

    private static final class TestPathMatchConfigurer extends PathMatchConfigurer {

        Map<String, Predicate<Class<?>>> pathPrefixes() {
            return getPathPrefixes();
        }
    }

    private static final class TestApiVersionConfigurer extends ApiVersionConfigurer {

        ApiVersionStrategy strategy() {
            return getApiVersionStrategy();
        }
    }
}
