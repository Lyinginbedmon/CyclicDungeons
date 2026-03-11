package com.lying.grammar.content.trap;

import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lying.blueprint.BlueprintRoom;
import com.lying.grammar.RoomMetadata;
import com.lying.grid.BlueprintTileGrid;
import com.lying.init.CDTileSets;
import com.lying.reference.Reference;
import com.lying.worldgen.TileGenerator;
import com.lying.worldgen.theme.Theme;
import com.lying.worldgen.tileset.TileSet;
import com.mojang.serialization.JsonOps;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public class TileSetTrap extends Trap
{
	public static final Identifier ID	= Reference.ModInfo.prefix("tileset");
	private Identifier tileSetID = null;
	
	public TileSetTrap(Identifier nameIn)
	{
		super(nameIn);
	}
	
	protected TileSetTrap(Identifier nameIn, Identifier tileSetIDIn)
	{
		this(nameIn);
		tileSetID = tileSetIDIn;
	}
	
	public static TileSetTrap of(TileSet set)
	{
		return of(set.registryName());
	}
	
	public static TileSetTrap of(Identifier set)
	{
		return new TileSetTrap(ID, set);
	}
	
	public JsonElement toJson(JsonOps ops)
	{
		JsonObject obj = asJsonObject();
		obj.addProperty("TileSet", tileSetID.toString());
		return obj;
	}
	
	public Trap fromJson(JsonOps ops, JsonElement ele)
	{
		JsonObject obj = ele.getAsJsonObject();
		tileSetID = Identifier.of(obj.get("TileSet").getAsString());
		return this;
	}
	
	public boolean isApplicableTo(BlueprintRoom room, RoomMetadata meta, Theme theme) { return getTileSet().isPresent(); }
	
	public void apply(BlockPos min, BlockPos max, ServerWorld world, RoomMetadata meta) { }
	
	public void prepare(BlueprintRoom room, BlueprintTileGrid tileMap, ServerWorld world)
	{
		Random random = Random.create(room.position().x() ^ room.position().x + room.position().y() ^ room.position().y);
		TileSet tileSet = getTileSet().get();
		TileGenerator.generate(tileMap, tileSet, () -> null, random);
	}
	
	protected Optional<TileSet> getTileSet() { return tileSetID == null ? Optional.empty() : CDTileSets.instance().get(tileSetID); }
}
