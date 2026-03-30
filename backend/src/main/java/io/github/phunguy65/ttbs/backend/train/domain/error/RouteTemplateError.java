package io.github.phunguy65.ttbs.backend.train.domain.error;

public sealed interface RouteTemplateError {

    record RouteTemplateNotFound() implements RouteTemplateError {
        @Override
        public String message() {
            return "Route template not found";
        }
    }

    String message();
}
