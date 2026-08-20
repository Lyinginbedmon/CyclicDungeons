package com.lying.event;

import java.util.Optional;
import java.util.UUID;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;

@FunctionalInterface
public interface PuzzleTriggerEvent
{
	public static final Event<PuzzleTriggerEvent> EVENT	= EventFactory.createLoop(PuzzleTriggerEvent.class);
	
	void onPuzzleTriggered(UUID id, Optional<String> channel);
}
