@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
            "user::model",
            "user::port",
            "shared",
            "train::port",
            "train::model",
            "train::validation"
        })
@org.springframework.modulith.NamedInterface("api")
package io.github.phunguy65.ttbs.backend.booking;
