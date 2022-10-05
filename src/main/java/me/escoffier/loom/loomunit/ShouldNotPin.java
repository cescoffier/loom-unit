package me.escoffier.loom.loomunit;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marker indicating that the test method should not pin the carrier thread.
 * If, during the execution of the test, a virtual thread pins the carrier thread, the test fails.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ShouldNotPin {

}
