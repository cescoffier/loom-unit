# Loom-Unit

A Junit 5 extension capturing weather a virtual threads _pins_ the carrier thread during the execution of the test.


## Usage

1. Add the following dependency to your project:

```xml
<dependency>    
  <groupId>me.escoffier.loom</groupId>
  <artifactId>loom-unit</artifactId>
  <version>VERSION</version>
  <scope>test</scope>  
</dependency>
```

**IMPORTANT**: You need to use Java 19+. 

2. Extends your test class with the `me.escoffier.loom.loomunit.LoomUnitExtension` extension:

```java
@ExtendWith(LoomUnitExtension.class) 
public class LoomUnitExampleTest {
    // ...
}
```

3. Use the `me.escoffier.loom.loomunit.ShouldNotPin` or `me.escoffier.loom.loomunit.ShouldPin` annotation on your test.

## Complete example

```java
package me.escoffier.loom.loomunit.snippets;
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
    public void testThatShouldNotPin(ThreadPinnedEvents events) { // Inject an object to check the pin events
        Assertions.assertTrue(events.getEvents().isEmpty());
        codeUnderTest.pin();
        await().until(() -> events.getEvents().size() > 0);
        Assertions.assertEquals(events.getEvents().size(), 1);
    }

}
```

