package io.github.phunguy65.ttbs.backend.user.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.user.domain.model.UserRole;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
class UserEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "phone")
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserEntity() {}

    UUID getId() {
        return id;
    }

    void setId(UUID id) {
        this.id = id;
    }

    String getEmail() {
        return email;
    }

    void setEmail(String email) {
        this.email = email;
    }

    String getPasswordHash() {
        return passwordHash;
    }

    void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    String getFullName() {
        return fullName;
    }

    void setFullName(String fullName) {
        this.fullName = fullName;
    }

    String getPhone() {
        return phone;
    }

    void setPhone(String phone) {
        this.phone = phone;
    }

    UserRole getRole() {
        return role;
    }

    void setRole(UserRole role) {
        this.role = role;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }

    void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
