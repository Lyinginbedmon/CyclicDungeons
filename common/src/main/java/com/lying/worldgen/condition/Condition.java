package com.lying.worldgen.condition;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lying.grid.BlueprintTileGrid;
import com.lying.init.CDTileConditions;
import com.lying.worldgen.Tile;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public abstract class Condition
{
	public static final Codec<Condition> CODEC = Codec.of(Condition::encode, Condition::decode);
	protected static final Codec<List<Direction>> FACE_LIST_CODEC	= Direction.CODEC.listOf();
	
	protected final Identifier id;
	
	protected Condition(Identifier idIn)
	{
		id = idIn;
	}
	
	public final Identifier registryName() { return id; }
	
	/** Evaluates if the given tile at the given position evaluates correctly with this condition */
	public abstract boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid set);
	
	public JsonElement toJson(JsonOps ops)
	{
		// Conditions are stored as just their registry ID unless they necessitate more information
		return Identifier.CODEC.encodeStart(ops, id).getOrThrow();
	}
	
	/** Returns a JsonObject with the registry ID of this condition under "id" */
	protected final JsonObject asJsonObject(JsonOps ops)
	{
		JsonObject obj = new JsonObject();
		obj.add("id", ops.createString(id.toString()));
		return obj;
	}
	
	public Condition fromJson(JsonObject json, JsonOps ops) { return this; }
	
	@Nullable
	public static Condition fromJson(JsonElement json, JsonOps ops)
	{
		JsonObject obj;
		if(json.isJsonPrimitive())
			return CDTileConditions.get(Identifier.CODEC.parse(ops, json).getOrThrow());
		else if(json.isJsonObject() && (obj = json.getAsJsonObject()).has("id"))
		{
			Condition condition = CDTileConditions.get(Identifier.CODEC.parse(ops, obj.get("id")).getOrThrow());
			return obj.size() > 1 ? condition.fromJson(obj, ops) : condition;
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private static <T> DataResult<T> encode(final Condition func, final DynamicOps<T> ops, final T prefix)
	{
		return ops == JsonOps.INSTANCE ? (DataResult<T>)DataResult.success(func.toJson((JsonOps)ops)) : DataResult.error(() -> "Storing tile condition as NBT is not supported");
	}
	
	private static <T> DataResult<Pair<Condition, T>> decode(final DynamicOps<T> ops, final T input)
	{
		return ops == JsonOps.INSTANCE ? DataResult.success(Pair.of(fromJson((JsonElement)input, (JsonOps)ops), input)) : DataResult.error(() -> "Loading tile condition from NBT is not supported");
	}
}