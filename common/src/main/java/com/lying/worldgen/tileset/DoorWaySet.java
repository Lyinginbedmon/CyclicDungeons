package com.lying.worldgen.tileset;

import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lying.init.CDTiles;
import com.lying.worldgen.tile.DefaultTiles;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;

import net.minecraft.util.Identifier;

public record DoorWaySet(
		Optional<Identifier> doorTile,
		Optional<Identifier> lintelTile,
		Optional<Identifier> flooringTile
		)
{
	public static final Codec<DoorWaySet> CODEC	= Codec.of(DoorWaySet::encode, DoorWaySet::decode);
	public static final DoorWaySet DEFAULTS	= of(CDTiles.ID_DOORWAY, CDTiles.ID_DOORWAY_LINTEL, DefaultTiles.ID_PRISTINE_FLOOR);
	public static final DoorWaySet BLANK	= new DoorWaySet(Optional.empty(), Optional.empty(), Optional.empty());
	
	@SuppressWarnings("unchecked")
	private static <T> DataResult<T> encode(final DoorWaySet func, final DynamicOps<T> ops, final T prefix)
	{
		if(ops != JsonOps.INSTANCE)
			return DataResult.error(() -> "DoorWaySet not permitted for non-JSON data storage");
		
		return (DataResult<T>)DataResult.success(func.toJson());
	}
	
	private static <T> DataResult<Pair<DoorWaySet, T>> decode(final DynamicOps<T> ops, final T input)
	{
		if(ops != JsonOps.INSTANCE)
			return DataResult.error(() -> "DoorWaySet not permitted for non-JSON data retrieval");
		
		return DataResult.success(Pair.of(fromJson((JsonElement)input), input));
	}
	
	public static DoorWaySet of(Identifier doorway)
	{
		return new DoorWaySet(Optional.of(doorway), Optional.empty(), Optional.empty());
	}
	
	public static DoorWaySet of(Identifier doorway, Identifier lintel)
	{
		return new DoorWaySet(Optional.of(doorway), Optional.of(lintel), Optional.empty());
	}
	
	public static DoorWaySet of(Identifier doorway, Identifier lintel, Identifier flooring)
	{
		return new DoorWaySet(Optional.of(doorway), Optional.of(lintel), Optional.of(flooring));
	}
	
	public JsonElement toJson()
	{
		JsonObject obj = new JsonObject();
		doorTile.ifPresent(id -> obj.addProperty("Door", id.toString()));
		lintelTile.ifPresent(id -> obj.addProperty("Lintel", id.toString()));
		flooringTile.ifPresent(id -> obj.addProperty("Flooring", id.toString()));
		return obj;
	}
	
	public static DoorWaySet fromJson(JsonElement ele)
	{
		if(!ele.isJsonObject())
			return new DoorWaySet(Optional.empty(), Optional.empty(), Optional.empty());
		
		JsonObject obj = ele.getAsJsonObject();
		return new DoorWaySet(
				retrieveIfPresent(obj, "Door"), 
				retrieveIfPresent(obj, "Lintel"), 
				retrieveIfPresent(obj, "Flooring"));
	}
	
	protected static Optional<Identifier> retrieveIfPresent(JsonObject obj, String name)
	{
		return obj.has(name) ? Optional.of(Identifier.of(obj.get(name).getAsString())) : Optional.empty();
	}
}
