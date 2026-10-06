package com.lying.worldgen.tileset;

import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lying.init.CDTiles;
import com.lying.worldgen.tile.DefaultTiles;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Identifier;

public record DoorWaySet(
		Optional<Identifier> doorTile,
		Optional<Identifier> lintelTile,
		Optional<Identifier> flooringTile
		)
{
	public static final Codec<DoorWaySet> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			Identifier.CODEC.optionalFieldOf("Door").forGetter(DoorWaySet::doorTile),
			Identifier.CODEC.optionalFieldOf("Lintel").forGetter(DoorWaySet::lintelTile),
			Identifier.CODEC.optionalFieldOf("Flooring").forGetter(DoorWaySet::flooringTile)
			).apply(instance, DoorWaySet::new));
	public static final DoorWaySet DEFAULTS	= of(CDTiles.ID_DOORWAY, CDTiles.ID_DOORWAY_LINTEL, DefaultTiles.ID_PRISTINE_FLOOR);
	public static final DoorWaySet BLANK	= new DoorWaySet(Optional.empty(), Optional.empty(), Optional.empty());
	
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
	
	protected static Optional<Identifier> retrieveIfPresent(JsonObject obj, String name)
	{
		return obj.has(name) ? Optional.of(Identifier.of(obj.get(name).getAsString())) : Optional.empty();
	}
	
	public static DoorWaySet fromJson(JsonElement obj) { return CODEC.parse(JsonOps.INSTANCE, obj).getOrThrow(); }
	
	public JsonElement toJson() { return CODEC.encodeStart(JsonOps.INSTANCE, this).getOrThrow(); }
}
