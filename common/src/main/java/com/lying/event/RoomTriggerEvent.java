package com.lying.event;

import java.util.Optional;
import java.util.UUID;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;

@FunctionalInterface
public interface RoomTriggerEvent
{
	public static final Event<RoomTriggerEvent> EVENT	= EventFactory.createLoop(RoomTriggerEvent.class);
	public static final Optional<String> START	= Optional.of("start");
	public static final Optional<String> FINISH	= Optional.of("completed");
	
	void onTriggered(UUID id, Optional<String> channel);
}
