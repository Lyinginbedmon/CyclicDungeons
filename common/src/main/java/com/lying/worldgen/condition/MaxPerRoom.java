package com.lying.worldgen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lying.grid.BlueprintTileGrid;
import com.lying.init.CDTileConditions;
import com.lying.worldgen.Tile;
import com.mojang.serialization.JsonOps;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class MaxPerRoom extends Condition
{
	private int i = 0;
	
	public MaxPerRoom(Identifier idIn)
	{
		super(idIn);
	}
	
	public static MaxPerRoom of(int val)
	{
		MaxPerRoom condition = (MaxPerRoom)CDTileConditions.MAX_TALLY.get();
		condition.i = val;
		return condition;
	}
	
	public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid set)
	{
		return set.tallyOf(tileIn) < i;
	}
	
	public JsonElement toJson(JsonOps ops)
	{
		JsonObject obj = asJsonObject(ops);
		obj.addProperty("cap", i);
		return obj;
	}
	
	public Condition fromJson(JsonObject obj, JsonOps ops)
	{
		i = obj.get("cap").getAsInt();
		return this;
	}
}