package com.lying.grammar.content;

import java.util.Optional;

import com.google.gson.JsonObject;
import com.lying.blueprint.BlueprintRoom;
import com.lying.grammar.RoomMetadata;
import com.lying.grid.BlueprintTileGrid;
import com.lying.init.CDContentTypes;
import com.lying.worldgen.theme.Theme;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public abstract class Content
{
	public static final Codec<Content> CODEC = Identifier.CODEC.comapFlatMap(id -> 
	{
		Optional<Content> type = CDContentTypes.get(id);
		return type.isEmpty() ? DataResult.error(() -> "Content type unrecognised: "+id.toString()) : DataResult.success(type.get());
	}, Content::registryName);
	
	private final Identifier registryName;
	protected boolean allowDeadEnds = true;
	protected Optional<Identifier> doorTile = Optional.empty();
	
	protected Content(Identifier nameIn)
	{
		registryName = nameIn;
	}
	
	public final Identifier registryName() { return registryName; }
	
	/** Returns true if this entry can be applied to the given room */
	public boolean isApplicableTo(BlueprintRoom room, RoomMetadata meta, Theme theme) { return allowDeadEnds || room.hasChildren(); }
	
	/** Applied when the entry is selected, before the room goes through tile generation */
	public void prepare(BlueprintRoom room, BlueprintTileGrid tileMap, ServerWorld world, Random rand) { }
	
	/** Applied after tile generation */
	public abstract void apply(BlockPos min, BlockPos max, ServerWorld world, RoomMetadata meta, Random rand);
	
	public Identifier getDoorTile(Theme theme) { return doorTile.orElse(theme.getStandardDoor()); }
	
	public Content setDoorTile(Identifier tileId)
	{
		doorTile = tileId == null ? Optional.empty() : Optional.of(tileId);
		return this;
	}
	
	public final Optional<JsonObject> getConfig()
	{
		JsonObject obj = toJson(new JsonObject(), JsonOps.INSTANCE);
		if(!allowDeadEnds)
			obj.addProperty("AllowDeadEnds", allowDeadEnds);
		if(doorTile.isPresent())
			obj.addProperty("DoorTileID", doorTile.get().toString());
		return obj.isEmpty() ? Optional.empty() : Optional.of(obj);
	}
	
	/** Stores all configured values from this trap into the given JsonObject, specific to each descendant class of Content */
	public JsonObject toJson(JsonObject obj, JsonOps ops)
	{
		return obj;
	}
	
	/** Loads all configured values from the given element into this trap */
	public Content fromJson(JsonOps ops, JsonObject obj)
	{
		if(obj.has("AllowDeadEnds"))
			allowDeadEnds = obj.get("AllowDeadEnds").getAsBoolean();
		if(obj.has("DoorTileID"))
			doorTile = Optional.of(Identifier.of(obj.get("DoorTileID").getAsString()));
		return this;
	}
}
