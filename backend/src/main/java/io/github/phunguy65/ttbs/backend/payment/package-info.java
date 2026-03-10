@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
            "booking::event",
            "booking::model",
            "booking::repository",
            "train::port",
            "train::model",
            "shared",
            "user::model"
        })
package io.github.phunguy65.ttbs.backend.payment;
