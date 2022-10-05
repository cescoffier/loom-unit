package me.escoffier.loom.loomunit;

import jdk.jfr.EventSettings;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordingStream;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A Junit 5 Extension that allows checking if virtual threads used in tests are pinning or not the carrier thread.
 * The detection is based on JFR events.
 * <p>
 * Example of usage:
 * {@snippet class="me.escoffier.loom.loomunit.snippets.LoomUnitExampleTest" region="example"}
 */
public class LoomUnitExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    public static final String CARRIER_PINNED_EVENT_NAME = "jdk.VirtualThreadPinned";
    public static final Logger LOGGER = Logger.getLogger("Loom-Unit");
    private RecordingStream stream;
    private volatile boolean capturing;

    private final Queue<RecordedEvent> events = new ConcurrentLinkedQueue<>();
    private CountDownLatch termination;

    private static final long INTERNAL_WAIT_TIME = 93;


    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        events.clear();
        termination = new CountDownLatch(1);
        start();
    }


    @Override
    public void afterEach(ExtensionContext extensionContext) {
        stop();

        List<RecordedEvent> pinEvents = events.stream().filter(re -> re.getEventType().getName().equals(CARRIER_PINNED_EVENT_NAME)).collect(Collectors.toList());
        Method method = extensionContext.getRequiredTestMethod();

        if (method.isAnnotationPresent(ShouldPin.class)) {
            ShouldPin annotation = method.getAnnotation(ShouldPin.class);
            if (pinEvents.isEmpty()) {
                throw new AssertionError("The test " + extensionContext.getDisplayName() + " was expected to pin the carrier thread, it didn't");
            }
            if (annotation.atMost() != Integer.MAX_VALUE && pinEvents.size() > annotation.atMost()) {
                throw new AssertionError("The test " + extensionContext.getDisplayName() + " was expected to pin the carrier thread at most " + annotation.atMost()
                        + ", but we collected " + pinEvents.size() + " events\n" + dump(pinEvents));
            }
        }

        if (method.isAnnotationPresent(ShouldNotPin.class)) {
            if (!pinEvents.isEmpty()) {
                throw new AssertionError("The test " + extensionContext.getDisplayName() + " was expected to NOT pin the carrier thread"
                        + ", but we collected " + pinEvents.size() + " event(s)\n" + dump(pinEvents));
            }
        }

    }

    private static final String STACK_TRACE_TEMPLATE = "\t%s.%s(%s.java:%d)\n";

    private String dump(List<RecordedEvent> pinEvents) {
        StringBuilder builder = new StringBuilder();
        for (RecordedEvent pinEvent : pinEvents) {
            builder.append("* Pinning event captured: \n");
            for (RecordedFrame recordedFrame : pinEvent.getStackTrace().getFrames()) {
                builder.append(STACK_TRACE_TEMPLATE.formatted(recordedFrame.getMethod().getType().getName(),
                        recordedFrame.getMethod().getName(), recordedFrame.getMethod().getType().getName(), recordedFrame.getLineNumber()));
            }
        }
        return builder.toString();
    }


    void start() {
        LOGGER.log(Level.FINE, "Starting recording");
        CountDownLatch latch = new CountDownLatch(1);
        try {
            stream = new RecordingStream();
            stream.setReuse(false);
            stream.setMaxSize(100);
            EventSettings settings = stream.enable(CARRIER_PINNED_EVENT_NAME);
            settings.withStackTrace();
            stream.enable(RecordingStartedEvent.class);

            stream.onEvent(re -> {
                if (re.getEventType().getName().equals(RecordingStartedEvent.RECORDING_STARTED_EVENT_NAME)) {
                    LOGGER.log(Level.FINE, "Recording Started Event captured");
                    latch.countDown();
                } else if (re.getEventType().getName().equals(RecordingStoppedEvent.RECORDING_STOPPED_EVENT_NAME)) {
                    LOGGER.log(Level.FINE, "Recording Stopped Event captured");
                    termination.countDown();
                } else {
                    if (capturing) {
                        events.add(re);
                    }
                }
            });
            stream.startAsync();
            awaitForTheRecordingToStart(latch);
            capturing = true;
            LOGGER.fine("Event stream started");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void awaitForTheRecordingToStart(CountDownLatch latch) throws InterruptedException {
        RecordingStartedEvent event = new RecordingStartedEvent();
        event.begin();
        event.commit();
        while (latch.getCount() != 0) {
            Thread.sleep(INTERNAL_WAIT_TIME);
        }
    }

    void stop() {
        RecordingStoppedEvent event = new RecordingStoppedEvent();
        event.begin();
        event.commit();
        try {
            termination.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        this.capturing = false;
        this.stream.close();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(ThreadPinnedEvents.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return (ThreadPinnedEvents) () -> events.stream().toList();
    }
}
