package com.lying.grammar.content.trap;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lying.blueprint.BlueprintRoom;
import com.lying.grammar.RoomMetadata;
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

public class TileTrap extends Trap
{
	public static final Identifier ID	= Reference.ModInfo.prefix("tile");
	private Identifier hazardID = DefaultTiles.ID_LAVA_RIVER;
	private boolean allowDeadEnds = false;
	
	public TileTrap(Identifier name)
	{
		super(name);
	}
	
	public TileTrap(Identifier name, Identifier hazardIn, boolean allowDeadEndsIn)
	{
		super(name);
		hazardID = hazardIn;
		allowDeadEnds = allowDeadEndsIn;
	}
	
	public static TileTrap of(Identifier hazardIn, boolean allowDeadEndsIn)
	{
		return new TileTrap(ID, hazardIn, allowDeadEndsIn);
	}
	
	public JsonElement toJson(JsonOps ops)
	{
		JsonObject obj = asJsonObject();
		obj.addProperty("Hazard", hazardID.toString());
		obj.addProperty("AllowDeadEnds", allowDeadEnds);
		return obj;
	}
	
	public Trap fromJson(JsonOps ops, JsonElement ele)
	{
		JsonObject obj = ele.getAsJsonObject();
		hazardID = Identifier.of(obj.get("Hazard").getAsString());
		allowDeadEnds = obj.get("AllowDeadEnds").getAsBoolean();
		return this;
	}
	
	protected Optional<Tile> getHazard() { return CDTiles.instance().get(hazardID); } 
	
	public boolean isApplicableTo(BlueprintRoom room, RoomMetadata meta, Theme theme) { return getHazard().isPresent() && (allowDeadEnds || room.hasChildren()); }
	
	public void prepare(BlueprintRoom room, BlueprintTileGrid tileMap, ServerWorld world)
	{
		final Optional<Tile> hazard = getHazard();
		List<BlockPos> blanks = Lists.newArrayList();
		blanks.addAll(tileMap.getBoundaries(List.of(Direction.DOWN)).stream()
				.filter(pos -> hazard.get().canExistAt(pos, tileMap))
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
				tileMap.put(blanks.remove(world.random.nextInt(blanks.size())), hazard.get());
				blanks.removeIf(pos -> !hazard.get().canExistAt(pos, tileMap));
			}
		}
	}
	
	public void apply(BlockPos min, BlockPos max, ServerWorld world, RoomMetadata meta) { }
}