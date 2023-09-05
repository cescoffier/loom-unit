package me.escoffier.loom.loomunit;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.SettingDefinition;
import jdk.jfr.StackTrace;


public interface InternalEvents {

	String INITIALIZATION_EVENT_NAME = "me.escoffier.loom.loomunit.InternalEvents.InitializationEvent";
	String SHUTDOWN_EVENT_NAME = "me.escoffier.loom.loomunit.InternalEvents.ShutdownEvent";

	String CAPTURING_STARTED_EVENT_NAME = "me.escoffier.loom.loomunit.InternalEvents.CapturingStartedEvent";
	String CAPTURING_STOPPED_EVENT_NAME = "me.escoffier.loom.loomunit.InternalEvents.CapturingStoppedEvent";



	@Name(INITIALIZATION_EVENT_NAME)
	@Category("loom-unit")
	@StackTrace(value = false)
	class InitializationEvent extends Event {
		// Marker event
	}

	@Name(SHUTDOWN_EVENT_NAME)
	@Category("loom-unit")
	@StackTrace(value = false)
	class ShutdownEvent extends Event {
		// Marker event
	}

	@Name(CAPTURING_STARTED_EVENT_NAME)
	@Category("loom-unit")
	@StackTrace(value = false)
	class CapturingStartedEvent extends Event {

		@Name("id")
		@Label("id")
		public final String id;

		public CapturingStartedEvent(String id) {
			this.id = id;
		}
	}

	@Name(CAPTURING_STOPPED_EVENT_NAME)
	@Category("loom-unit")
	@StackTrace(value = false)
	class CapturingStoppedEvent extends Event {


		@Name("id")
		@Label("id")
		public final String id;

		public CapturingStoppedEvent(String id) {
			this.id = id;
		}
	}
}
