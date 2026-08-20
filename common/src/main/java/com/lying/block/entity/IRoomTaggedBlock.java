package com.lying.block.entity;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

public interface IRoomTaggedBlock
{
	public void setRoom(@Nullable UUID id);
	
	public default boolean hasRoom() { return getRoom() != null; }
	
	@Nullable
	public UUID getRoom();
}
