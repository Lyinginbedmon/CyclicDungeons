package com.lying.grammar.content.trap;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;
import com.lying.blueprint.BlueprintRoom;
import com.lying.grammar.RoomMetadata;
import com.lying.grammar.content.TrapRoomContent.TrapEntry;
import com.lying.grid.BlueprintTileGrid;
import com.lying.init.CDTiles;
import com.lying.worldgen.theme.Theme;
import com.lying.worldgen.tile.DefaultTiles;
import com.lying.worldgen.tile.Tile;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class PitfallTrapEntry extends TrapEntry
{
	private static final Optional<Tile> PIT = CDTiles.instance().get(DefaultTiles.ID_PITFALL);
	
	public PitfallTrapEntry(Identifier name)
	{
		super(name);
	}
	
	public boolean isApplicableTo(BlueprintRoom room, RoomMetadata meta, Theme theme) { return PIT.isPresent(); }
	
	public void prepare(BlueprintRoom room, BlueprintTileGrid tileMap, ServerWorld world)
	{
		List<BlockPos> blanks = Lists.newArrayList();
		blanks.addAll(tileMap.getBoundaries(List.of(Direction.DOWN)).stream()
				.filter(pos -> PIT.get().canExistAt(pos, tileMap))
				.filter(pos -> 
				{
					Optional<Tile> tileAt = tileMap.get(pos);
					return tileAt.isPresent() && tileAt.get().isBlank();
					})
				.toList());
		
		if(blanks.isEmpty())
			return;
		
		BlockPos start = blanks.remove(world.random.nextInt(blanks.size()));
		recursivePlacePit(start, tileMap);
		
		// TODO Place treasure at position away from entryway
	}
	
	protected void recursivePlacePit(BlockPos pos, BlueprintTileGrid tileMap)
	{
		tileMap.put(pos, PIT.get());
		Direction.Type.HORIZONTAL.stream()
			.map(pos::offset)
			.filter(tileMap::contains)
			.filter(p -> tileMap.get(p).get().isBlank())
			.filter(p -> PIT.get().canExistAt(p, tileMap))
			.forEach(p -> recursivePlacePit(p, tileMap));
	}
	
	public void apply(BlockPos min, BlockPos max, ServerWorld world, RoomMetadata meta) { }
}