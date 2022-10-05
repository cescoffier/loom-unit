package me.escoffier.loom.loomunit;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Name;

@Name(RecordingStoppedEvent.RECORDING_STOPPED_EVENT_NAME)
@Category("loom-unit")
public class RecordingStoppedEvent extends Event {
    public static final String RECORDING_STOPPED_EVENT_NAME = "loom.recordingStopped";
}