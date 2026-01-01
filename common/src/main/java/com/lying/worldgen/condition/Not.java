package com.lying.worldgen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lying.grid.BlueprintTileGrid;
import com.lying.init.CDTileConditions;
import com.lying.worldgen.Tile;
import com.mojang.serialization.JsonOps;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class Not extends Condition
{
	private Condition child = CDTileConditions.NEVER.get();
	
	public Not(Identifier idIn)
	{
		super(idIn);
	}
	
	public static Condition of(Condition child)
	{
		Not not = (Not)CDTileConditions.NOT.get();
		not.child = child;
		return not;
	}
	
	public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid set) { return !child.test(tileIn, pos, set); }
	
	public JsonElement toJson(JsonOps ops)
	{
		JsonObject obj = asJsonObject(ops);
		obj.add("condition", child.toJson(ops));
		return obj;
	}
	
	public Condition fromJson(JsonObject obj, JsonOps ops)
	{
		child = Condition.fromJson(obj.get("condition"), ops);
		return this;
	}
}