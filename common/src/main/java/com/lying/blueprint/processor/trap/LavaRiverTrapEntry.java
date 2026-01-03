package com.lying.blueprint.processor.trap;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;
import com.lying.blueprint.BlueprintRoom;
import com.lying.blueprint.processor.TrapRoomProcessor.TrapEntry;
import com.lying.grammar.RoomMetadata;
import com.lying.grid.BlueprintTileGrid;
import com.lying.init.CDThemes.Theme;
import com.lying.init.CDTiles;
import com.lying.worldgen.tile.DefaultTiles;
import com.lying.worldgen.tile.Tile;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class LavaRiverTrapEntry extends TrapEntry
{
	private static final Optional<Tile> LAVA_TILE = CDTiles.instance().get(DefaultTiles.ID_LAVA_RIVER);
	
	public LavaRiverTrapEntry(Identifier name)
	{
		super(name);
	}
	
	public boolean isApplicableTo(BlueprintRoom room, RoomMetadata meta, Theme theme) { return LAVA_TILE.isPresent() && room.hasChildren(); }
	
	public void prepare(BlueprintRoom room, BlueprintTileGrid tileMap, ServerWorld world)
	{
		List<BlockPos> blanks = Lists.newArrayList();
		blanks.addAll(tileMap.getBoundaries(List.of(Direction.DOWN)).stream()
				.filter(pos -> LAVA_TILE.get().canExistAt(pos, tileMap))
				.filter(pos -> 
				{
					Optional<Tile> tileAt = tileMap.get(pos);
					return tileAt.isPresent() && tileAt.get().isBlank();
					})
				.toList());
		
		if(blanks.isEmpty())
			return;
		
		final int count = (int)((float)blanks.size() * 0.70F);
		for(int i=0; i<count; i++)
		{
			if(blanks.isEmpty())
				break;
			else
			{
				tileMap.put(blanks.remove(world.random.nextInt(blanks.size())), LAVA_TILE.get());
				blanks.removeIf(pos -> !LAVA_TILE.get().canExistAt(pos, tileMap));
			}
		}
	}
	
	public void apply(BlockPos min, BlockPos max, ServerWorld world, RoomMetadata meta) { }
}