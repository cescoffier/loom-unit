package me.escoffier.loom.loomunit;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Name;

@Name(RecordingStartedEvent.RECORDING_STARTED_EVENT_NAME)
@Category("loom-unit")
public class RecordingStartedEvent extends Event {
    public static final String RECORDING_STARTED_EVENT_NAME = "loom.recordingStarted";
}