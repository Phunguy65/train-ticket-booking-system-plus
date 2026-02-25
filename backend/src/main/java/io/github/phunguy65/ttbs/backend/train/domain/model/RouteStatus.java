package io.github.phunguy65.ttbs.backend.train.domain.model;

/**
 * Status lifecycle for a {@link Route}.
 *
 * <p>Only {@code SCHEDULED} is used in this change. Additional statuses (CANCELLED, COMPLETED)
 * may be added in future changes.
 */
public enum RouteStatus {
    SCHEDULED
}
