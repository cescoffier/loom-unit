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
public class LoomUnitExampleTest {

    CodeUnderTest codeUnderTest = new CodeUnderTest();

    @Test
    @ShouldNotPin
    public void testThatShouldNotPin() {
        // ...
    }

    @Test
    @ShouldPin(atMost = 1)
    public void testThatShouldPinAtMostOnce() {
        codeUnderTest.pin();
    }

    @Test
    public void testThatShouldPin(ThreadPinnedEvents events) { // Inject an object to check the pin events
        Assertions.assertTrue(events.getEvents().isEmpty());
        codeUnderTest.pin();
        await().until(() -> events.getEvents().size() > 0);
        Assertions.assertEquals(events.getEvents().size(), 1);
    }

}
// @end
