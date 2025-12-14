package com.lying.blueprint.processor;

import com.lying.blueprint.BlueprintRoom;
import com.lying.grid.BlueprintTileGrid;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

@FunctionalInterface
public interface IProcessorEntry
{
	public default void prepare(BlueprintRoom room, BlueprintTileGrid tileMap, ServerWorld world) { }
	
	public void apply(BlockPos min, BlockPos max, ServerWorld world);
}