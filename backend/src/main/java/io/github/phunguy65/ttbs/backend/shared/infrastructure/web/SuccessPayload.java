package io.github.phunguy65.ttbs.backend.shared.infrastructure.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SuccessPayload {
    Class<?> value() default Void.class;

    SuccessResponseKind kind() default SuccessResponseKind.OBJECT;

    String responseCode() default "";
}
