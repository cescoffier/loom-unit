package me.escoffier.loom.loomunit;

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstantiationException;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static me.escoffier.loom.loomunit.Collector.CARRIER_PINNED_EVENT_NAME;

/**
 * A Junit 5 Extension that allows checking if virtual threads used in tests are pinning or not the carrier thread.
 * The detection is based on JFR events.
 * <p>
 * Example of usage:
 * {@snippet class = "me.escoffier.loom.loomunit.snippets.LoomUnitExampleTest" region = "example"}
 * <p>
 * {@snippet class = "me.escoffier.loom.loomunit.snippets.LoomUnitExampleOnClassTest" region = "example"}
 */
public class LoomUnitExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback, ParameterResolver {


	private ExtensionContext.Namespace namespace;


	@Override
	public void beforeAll(ExtensionContext extensionContext) throws Exception {
		Collector collector = new Collector();
		namespace = ExtensionContext.Namespace.create("loom-unit");
		var store = extensionContext.getStore(namespace);
		store.put("collector", collector);
		collector.init();
	}

	@Override
	public void afterAll(ExtensionContext extensionContext) throws Exception {
		var store = extensionContext.getStore(namespace);
		store.get("collector", Collector.class).shutdown();
	}

	@Override
	public void beforeEach(ExtensionContext extensionContext) throws InterruptedException {
		if (requiresRecording(extensionContext.getRequiredTestClass(), extensionContext.getRequiredTestMethod())) {
			var store = extensionContext.getStore(namespace);
			store.get("collector", Collector.class).start(extensionContext);

			if (getShouldPin(extensionContext.getRequiredTestClass(), extensionContext.getRequiredTestMethod()) != null
					&& getShouldNotPin(extensionContext.getRequiredTestClass(), extensionContext.getRequiredTestMethod()) != null) {
				throw new TestInstantiationException("Cannot execute test " + extensionContext.getDisplayName() + ": @ShouldPin and @ShouldNotPin are used on the method or class");
			}
		}
	}

	private boolean requiresRecording(Class<?> clazz, Method method) {
		if (clazz.isAnnotationPresent(ShouldNotPin.class) || clazz.isAnnotationPresent(ShouldPin.class)
				|| method.isAnnotationPresent(ShouldNotPin.class) || method.isAnnotationPresent(ShouldPin.class)) {
			return true;
		}
		return Arrays.asList(method.getParameterTypes()).contains(ThreadPinnedEvents.class);
	}

	private ShouldPin getShouldPin(Class<?> clazz, Method method) {
		if (method.isAnnotationPresent(ShouldPin.class)) {
			return method.getAnnotation(ShouldPin.class);
		}

		if (method.isAnnotationPresent(ShouldNotPin.class)) {
			// If the method overrides the class annotation.
			return null;
		}

		if (clazz.isAnnotationPresent(ShouldPin.class)) {
			return clazz.getAnnotation(ShouldPin.class);
		}

		return null;
	}

	private ShouldNotPin getShouldNotPin(Class<?> clazz, Method method) {
		if (method.isAnnotationPresent(ShouldNotPin.class)) {
			return method.getAnnotation(ShouldNotPin.class);
		}

		if (method.isAnnotationPresent(ShouldPin.class)) {
			// If the method overrides the class annotation.
			return null;
		}

		if (clazz.isAnnotationPresent(ShouldNotPin.class)) {
			return clazz.getAnnotation(ShouldNotPin.class);
		}

		return null;
	}

	@Override
	public void afterEach(ExtensionContext extensionContext) throws InterruptedException {
		Method method = extensionContext.getRequiredTestMethod();
		Class<?> clazz = extensionContext.getRequiredTestClass();
		if (!requiresRecording(clazz, method)) {
			return;
		}
		var store = extensionContext.getStore(namespace);
		List<RecordedEvent> captured = store.get("collector", Collector.class).stop(extensionContext);
		List<RecordedEvent> pinEvents = captured.stream().filter(re -> re.getEventType().getName().equals(CARRIER_PINNED_EVENT_NAME)).collect(Collectors.toList());

		ShouldPin pin = getShouldPin(clazz, method);
		ShouldNotPin notpin = getShouldNotPin(clazz, method);

		if (pin != null) {
			if (pinEvents.isEmpty()) {
				throw new AssertionError("The test " + extensionContext.getDisplayName() + " was expected to pin the carrier thread, it didn't");
			}
			if (pin.atMost() != Integer.MAX_VALUE && pinEvents.size() > pin.atMost()) {
				throw new AssertionError("The test " + extensionContext.getDisplayName() + " was expected to pin the carrier thread at most " + pin.atMost()
						+ ", but we collected " + pinEvents.size() + " events\n" + dump(pinEvents));
			}
		}

		if (notpin != null) {
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


	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
		return parameterContext.getParameter().getType().equals(ThreadPinnedEvents.class);
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
		return (ThreadPinnedEvents) () -> {
			var store = extensionContext.getStore(namespace);
			return store.get("collector", Collector.class).getEvents();
		};
	}
}
