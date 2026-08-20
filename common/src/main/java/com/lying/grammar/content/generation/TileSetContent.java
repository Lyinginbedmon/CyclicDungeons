package com.lying.grammar.content.generation;

import java.util.Optional;

import com.google.gson.JsonObject;
import com.lying.blueprint.BlueprintRoom;
import com.lying.grammar.RoomMetadata;
import com.lying.grammar.content.Content;
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

public class TileSetContent extends Content
{
	public static final Identifier ID	= Reference.ModInfo.prefix("tileset");
	private Identifier tileSetID = null;
	
	public TileSetContent(Identifier nameIn)
	{
		super(nameIn);
	}
	
	protected TileSetContent(Identifier nameIn, Identifier tileSetIDIn)
	{
		this(nameIn);
		tileSetID = tileSetIDIn;
	}
	
	public static TileSetContent of(TileSet set)
	{
		return of(set.registryName());
	}
	
	public static TileSetContent of(Identifier set)
	{
		return new TileSetContent(ID, set);
	}
	
	public JsonObject toJson(JsonObject obj, JsonOps ops)
	{
		super.toJson(obj, ops);
		obj.addProperty("TileSet", tileSetID.toString());
		return obj;
	}
	
	public Content fromJson(JsonOps ops, JsonObject obj)
	{
		super.fromJson(ops, obj);
		tileSetID = Identifier.of(obj.get("TileSet").getAsString());
		return this;
	}
	
	public boolean isApplicableTo(BlueprintRoom room, RoomMetadata meta, Theme theme) { return getTileSet().isPresent(); }
	
	public void apply(BlockPos min, BlockPos max, ServerWorld world, RoomMetadata meta, Random rand) { }
	
	public void prepare(BlueprintRoom room, BlueprintTileGrid tileMap, ServerWorld world, Random rand)
	{
		Random random = Random.create(room.position().x() ^ room.position().x + room.position().y() ^ room.position().y);
		TileSet tileSet = getTileSet().get();
		TileGenerator.generate(tileMap, tileSet, () -> null, random);
	}
	
	protected Optional<TileSet> getTileSet() { return tileSetID == null ? Optional.empty() : CDTileSets.instance().get(tileSetID); }
}
