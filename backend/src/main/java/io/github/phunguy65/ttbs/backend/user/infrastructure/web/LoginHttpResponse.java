package io.github.phunguy65.ttbs.backend.user.infrastructure.web;

record LoginHttpResponse(String accessToken, String refreshToken, UserHttpResponse user) {}
