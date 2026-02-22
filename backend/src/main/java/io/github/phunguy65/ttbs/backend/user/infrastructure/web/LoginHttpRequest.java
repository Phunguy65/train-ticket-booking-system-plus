package io.github.phunguy65.ttbs.backend.user.infrastructure.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

record LoginHttpRequest(
        @Email(message = "Must be a valid email address") @NotBlank(message = "Email is required") String email,

        @NotBlank(message = "Password is required") String password) {}
