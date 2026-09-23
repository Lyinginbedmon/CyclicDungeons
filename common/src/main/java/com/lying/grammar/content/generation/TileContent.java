package com.lying.grammar.content.generation;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.lying.blueprint.BlueprintRoom;
import com.lying.grammar.RoomMetadata;
import com.lying.grammar.content.Content;
import com.lying.grammar.content.RoomNumberProvider;
import com.lying.grid.BlueprintTileGrid;
import com.lying.init.CDTiles;
import com.lying.reference.Reference;
import com.lying.worldgen.theme.Theme;
import com.lying.worldgen.tile.DefaultTiles;
import com.lying.worldgen.tile.Tile;
import com.mojang.serialization.JsonOps;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

public class TileContent extends Content
{
	public static final Identifier ID	= Reference.ModInfo.prefix("tile");
	private Identifier hazardID = DefaultTiles.ID_LAVA_RIVER;
	private RoomNumberProvider counter = new RoomNumberProvider.SizeRatio(1D, 1D, 0.5D);
	
	public TileContent(Identifier name)
	{
		super(name);
	}
	
	public TileContent(Identifier name, Identifier hazardIn, RoomNumberProvider countIn)
	{
		super(name);
		hazardID = hazardIn;
		counter = countIn;
	}
	
	public static TileContent of(Identifier hazardIn, RoomNumberProvider hazardCountIn)
	{
		return new TileContent(ID, hazardIn, hazardCountIn);
	}
	
	public JsonObject toJson(JsonObject obj, JsonOps ops)
	{
		super.toJson(obj, ops);
		obj.addProperty("Tile", hazardID.toString());
		return obj;
	}
	
	public Content fromJson(JsonOps ops, JsonObject obj)
	{
		super.fromJson(ops, obj);
		hazardID = Identifier.of(obj.get("Tile").getAsString());
		return this;
	}
	
	protected Optional<Tile> getTile() { return CDTiles.instance().get(hazardID); }
	
	public boolean isApplicableTo(BlueprintRoom room, RoomMetadata meta, Theme theme) { return super.isApplicableTo(room, meta, theme) && getTile().isPresent(); }
	
	public void prepare(BlueprintRoom room, BlueprintTileGrid tileMap, ServerWorld world, Random rand)
	{
		final Tile tile = getTile().get();
		final Predicate<BlockPos> canExistAt = p -> tile.canExistAt(p, tileMap);
		List<BlockPos> blanks = Lists.newArrayList();
		blanks.addAll(tileMap.getBoundaries(List.of(Direction.DOWN)).stream()
				.filter(canExistAt)
				.filter(pos -> 
				{
					Optional<Tile> tileAt = tileMap.get(pos);
					return tileAt.isPresent() && tileAt.get().isBlank();
					})
				.toList());
		int count = counter.getCount(world.getRandom(), room.metadata().tileSize());
		while(!blanks.isEmpty() && (counter.isUnlimited() || count-- > 0))
		{
			BlockPos pos = blanks.remove(blanks.size() == 1 ? 0 : world.random.nextInt(blanks.size()));
			tileMap.put(pos, tile);
			blanks.removeIf(canExistAt.negate());
		}
	}
	
	public void apply(BlockPos min, BlockPos max, ServerWorld world, RoomMetadata meta, Random rand) { }
}