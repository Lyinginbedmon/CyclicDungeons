package com.lying.grammar.content.trap;

import java.util.List;
import java.util.Optional;

import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.blueprint.BlueprintRoom;
import com.lying.grammar.RoomMetadata;
import com.lying.grammar.content.TrapRoomContent.TrapEntry;
import com.lying.grid.BlueprintTileGrid;
import com.lying.init.CDTileConditions;
import com.lying.init.CDTileTags;
import com.lying.init.CDTiles;
import com.lying.worldgen.theme.Theme;
import com.lying.worldgen.tile.DefaultTiles;
import com.lying.worldgen.tile.Tile;
import com.lying.worldgen.tile.TilePredicate;
import com.lying.worldgen.tile.condition.Condition;
import com.lying.worldgen.tile.condition.IsAnyOf;
import com.lying.worldgen.tile.condition.NearBox;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

public class JumpingTrapEntry extends TrapEntry
{
	private static final Optional<Tile> FLOOR = CDTiles.instance().get(DefaultTiles.ID_PRISTINE_FLOOR);
	private final Identifier fillTile;
	
	private static final Condition IS_FLOOR = IsAnyOf.HasTag.of(CDTileTags.ID_SOLID_FLOORING);
	
	public JumpingTrapEntry(Identifier idIn, Identifier fillTileID)
	{
		super(idIn);
		fillTile = fillTileID;
	}
	
	public boolean isApplicableTo(BlueprintRoom room, RoomMetadata meta, Theme theme) { return CDTiles.instance().get(fillTile).isPresent() && FLOOR.isPresent(); }
	
	public void prepare(BlueprintRoom room, BlueprintTileGrid tileMap, ServerWorld world)
	{
		final Tile flooring = FLOOR.get();
		
		// Place initial solid flooring underneath doorway exclusions
		tileMap.getMatchingTiles((p,t) -> t.registryName().equals(CDTiles.ID_PASSAGE_FLAG)).stream()
			.map(BlockPos::down)
			.filter(tileMap::contains)
			.forEach(p -> tileMap.put(p, flooring));
		
		// Randomly place solid flooring at distance from other solid flooring
		final Random rand = world.getRandom();
		final Vector2i roomSize = room.metadata().tileSize();
		final int minSepX = roomSize.x < 4 ? 1 : rand.nextBetween(1, 2);
		final int minSepY = roomSize.y < 4 ? 1 : rand.nextBetween(1, 2);
		final TilePredicate spacingPredicate = buildSpacingPredicate(minSepX, minSepY);
		
		// TODO Implement platform height offsets
		
		List<BlockPos> platforms = Lists.newArrayList();
		List<BlockPos> options;
		while(!(options = tileMap.getMatchingTiles((p,t) -> spacingPredicate.test(t, p, tileMap))).isEmpty())
		{
			BlockPos pos = options.size() > 1 ? options.get(rand.nextInt(options.size())) : options.get(0);
			tileMap.put(pos, flooring);
			platforms.add(pos);
		}
		
		// Fill in remaining floor space with filler tile
		final Tile filler = CDTiles.instance().get(fillTile).get();
		tileMap.getMatchingTiles((p,t) -> t.isBlank() && flooring.canExistAt(p, tileMap)).forEach(p -> tileMap.put(p, filler));
		
		if(!room.hasChildren())
		{
			final BlockPos entry = tileMap.getMatchingTiles((p,t) -> t.registryName().equals(CDTiles.ID_PASSAGE_FLAG)).getFirst();
			platforms.stream()
					.filter(p -> !tileMap.isBoundary(p, Direction.UP) && tileMap.isEmpty(p.up()))
					.sorted((a,b) -> 
					{
						double distA = a.getSquaredDistance(entry);
						double distB = b.getSquaredDistance(entry);
						return distA > distB ? -1 : distA < distB ? 1 : 0;
					})
					.findFirst()
					.ifPresent(p -> tileMap.put(p, DefaultTiles.TREASURE.get()));
		}
	}
	
	protected TilePredicate buildSpacingPredicate(int minSepX, int minSepY)
	{
		final BlockPos 
			hitMin = new BlockPos(-minSepX, 0, -minSepY),
			hitMax = new BlockPos(minSepX, 0, minSepY);
		
		return TilePredicate.Builder.create()
				.condition(CDTileConditions.ON_BOTTOM.get())
				.condition(NearBox.Inverse.of(Box.enclosing(hitMin, hitMax), IS_FLOOR))
				.condition(NearBox.of(Box.enclosing(hitMin.add(-1, 0, -1), hitMax.add(1, 0, 1)), IS_FLOOR))
				.build();
	}
	
	public void apply(BlockPos min, BlockPos max, ServerWorld world, RoomMetadata meta) { }
}
