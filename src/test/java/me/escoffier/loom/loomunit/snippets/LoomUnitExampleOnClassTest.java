package me.escoffier.loom.loomunit.snippets;
// @start region="example"
import me.escoffier.loom.loomunit.LoomUnitExtension;
import me.escoffier.loom.loomunit.ThreadPinnedEvents;
import me.escoffier.loom.loomunit.ShouldNotPin;
import me.escoffier.loom.loomunit.ShouldPin;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.awaitility.Awaitility.await;


@ExtendWith(LoomUnitExtension.class) // Use the extension
@ShouldNotPin // You can use @ShouldNotPin or @ShouldPin on the class itself, it's applied to each method.
public class LoomUnitExampleOnClassTest {

	CodeUnderTest codeUnderTest = new CodeUnderTest();

	@Test
	public void testThatShouldNotPin() {
		// ...
	}

	@Test
	@ShouldPin(atMost = 1) // Method annotation overrides the class annotation
	public void testThatShouldPinAtMostOnce() {
		codeUnderTest.pin();
	}

}
// @end