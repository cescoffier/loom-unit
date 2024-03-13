package me.escoffier.loom.loomunit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker indicating that the test method should not pin the carrier thread.
 * If, during the execution of the test, a virtual thread pins the carrier thread, the test fails.
 * However occasional pin can still be allowed by setting {@code atMost} value
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface ShouldNotPin {

    int atMost() default 0;

}
