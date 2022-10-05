package me.escoffier.loom.loomunit;

import jdk.jfr.consumer.RecordedEvent;

import java.util.List;

/**
 * Object that can be injected in a test method.
 * It gives controlled on the captured events, and so let you do manual checks.
 * <p>
 * The returned list is a copy of the list of captured events.
 */
public interface ThreadPinnedEvents {

    List<RecordedEvent> getEvents();

}
