@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
            "booking::application",
            "booking::domain",
            "booking::usecase",
            "booking::command",
            "booking::event",
            "booking::port",
            "shared"
        })
package io.github.phunguy65.ttbs.backend.payment;
