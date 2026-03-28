package io.github.phunguy65.ttbs.backend.user.application.command;

public record RegisterUserCommand(String email, String password, String fullName) {}
