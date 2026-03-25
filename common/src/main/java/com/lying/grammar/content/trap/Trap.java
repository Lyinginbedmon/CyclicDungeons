package com.lying.grammar.content.trap;

import java.util.Optional;

import com.google.gson.JsonObject;
import com.lying.blueprint.BlueprintRoom;
import com.lying.grammar.RoomMetadata;
import com.lying.grid.BlueprintTileGrid;
import com.lying.init.CDTrapTypes;
import com.lying.worldgen.theme.Theme;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public abstract class Trap
{
	public static final Codec<Trap> CODEC = Identifier.CODEC.comapFlatMap(id -> 
	{
		Optional<Trap> type = CDTrapTypes.get(id);
		return type.isEmpty() ? DataResult.error(() -> "Trap type unrecognised: "+id.toString()) : DataResult.success(type.get());
	}, Trap::registryName);
	
	private final Identifier registryName;
	protected boolean allowDeadEnds = true;
	
	protected Trap(Identifier nameIn)
	{
		registryName = nameIn;
	}
	
	public final Identifier registryName() { return registryName; }
	
	/** Returns true if this entry can be applied to the given room */
	public boolean isApplicableTo(BlueprintRoom room, RoomMetadata meta, Theme theme) { return allowDeadEnds || room.hasChildren(); }
	
	/** Applied when the entry is selected, before the room goes through tile generation */
	public void prepare(BlueprintRoom room, BlueprintTileGrid tileMap, ServerWorld world) { }
	
	/** Applied after tile generation */
	public abstract void apply(BlockPos min, BlockPos max, ServerWorld world, RoomMetadata meta);
	
	public final Optional<JsonObject> getConfig()
	{
		JsonObject obj = toJson(new JsonObject(), JsonOps.INSTANCE);
		if(!allowDeadEnds)
			obj.addProperty("AllowDeadEnds", allowDeadEnds);
		return obj.isEmpty() ? Optional.empty() : Optional.of(obj);
	}
	
	/** Stores all configured values from this trap into the given JsonObject */
	public JsonObject toJson(JsonObject obj, JsonOps ops) { return obj; }
	
	/** Loads all configured values from the given element into this trap */
	public Trap fromJson(JsonOps ops, JsonObject obj) { return this; }
}
