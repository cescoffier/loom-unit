package me.escoffier.loom.loomunit;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Indicates that the method can pin. At most can be set to indicate  the maximum number of events.
 * If, during the execution of the test, a virtual thread does not pin the carrier thread, or pins it more than
 * the given {@code atMost} value, the test fails.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ShouldPin {

    int atMost() default Integer.MAX_VALUE;

}
