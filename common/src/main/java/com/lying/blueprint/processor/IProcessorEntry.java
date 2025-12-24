package com.lying.blueprint.processor;

import java.util.List;

import com.google.common.collect.Lists;
import com.lying.blueprint.BlueprintRoom;
import com.lying.grammar.RoomMetadata;
import com.lying.grid.BlueprintTileGrid;
import com.lying.init.CDThemes.Theme;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public interface IProcessorEntry
{
	public Identifier registryName();
	
	/** Returns true if this entry can be applied to the given room */
	public default boolean isApplicableTo(BlueprintRoom room, RoomMetadata meta, Theme theme) { return true; }
	
	/** Applied when the entry is selected, before the room goes through tile generation */
	public default void prepare(BlueprintRoom room, BlueprintTileGrid tileMap, ServerWorld world) { }
	
	/** Applied after tile generation */
	public void apply(BlockPos min, BlockPos max, ServerWorld world, RoomMetadata meta);
	
	public static <T extends BlockEntity> List<T> getTileEntities(BlockPos min, BlockPos max, ServerWorld world, BlockEntityType<T> type)
	{
		List<T> tiles = Lists.newArrayList();
		for(int x=min.getX(); x<max.getX(); x++)
			for(int z=min.getZ(); z<max.getZ(); z++)
				for(int y=min.getY(); y<max.getY(); y++)
					world.getBlockEntity(new BlockPos(x, y, z), type).ifPresent(tiles::add);
		
		return tiles;
	}
}