package com.lying.blueprint.processor;

import java.util.function.Supplier;

import com.lying.blueprint.BlueprintRoom;
import com.lying.grammar.RoomMetadata;
import com.lying.grid.BlueprintTileGrid;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public interface IRoomProcessor
{
	public static final Supplier<IRoomProcessor> NOOP = () -> new IRoomProcessor() 
	{
		public void applyPreProcessing(BlueprintRoom room, RoomMetadata meta, BlueprintTileGrid tileMap, ServerWorld world) { }
		
		public void applyPostProcessing(BlockPos min, BlockPos max, ServerWorld world, BlueprintRoom room, RoomMetadata meta) { }
	};
	
	/** Applied before room tile generation */
	public void applyPreProcessing(BlueprintRoom room, RoomMetadata meta, BlueprintTileGrid tileMap, ServerWorld world);
	
	/** Applied after all other room generation */
	public void applyPostProcessing(BlockPos min, BlockPos max, ServerWorld world, BlueprintRoom room, RoomMetadata meta);
}
