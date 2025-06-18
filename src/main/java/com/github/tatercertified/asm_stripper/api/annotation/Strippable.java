package com.github.tatercertified.asm_stripper.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class containing {@link Strip} Annotations.<p>
 * Failing to mark a class will result in the Strips not being processed
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Strippable {
}
