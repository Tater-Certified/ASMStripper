package com.github.tatercertified.asm_stripper.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to easily register a Stripper for a field, method, or class.<p>
 * Annotate a field, method, or class to remove it during runtime.<p>
 *
 * This has special functionality when paired with Mixin. If a field or method is
 * as annotated with @Shadow and @Strip, then the original class's field/method will be removed.
 * If a Mixin class is annotated with @Strip, then the original class will be removed.<p>
 *
 * An alternative classpath can be set if not using Mixin. Just specify it in the Strip annotation.
 * Then create a replica of the field/method you want to remove
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
public @interface Strip {
    String altClassPath() default "";
}
