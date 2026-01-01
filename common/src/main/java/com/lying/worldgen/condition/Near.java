package com.lying.worldgen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lying.grid.BlueprintTileGrid;
import com.lying.init.CDTileConditions;
import com.lying.worldgen.Tile;
import com.mojang.serialization.JsonOps;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class Near extends Condition
{
	protected double d = 0D;
	protected Condition child = CDTileConditions.NEVER.get();
	
	public Near(Identifier idIn)
	{
		super(idIn);
	}
	
	public static Near of(double distance, Condition childIn)
	{
		Near condition = (Near)CDTileConditions.NEAR.get();
		condition.d = Math.min(10D, distance);
		condition.child = childIn;
		return condition;
	}

	public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid set)
	{
		return !set.getMatchingTiles((p2,t2) -> 
			p2.isWithinDistance(pos, d) && 
			child.test(t2, p2, set)).isEmpty();
	}
	
	public JsonElement toJson(JsonOps ops)
	{
		JsonObject obj = asJsonObject(ops);
		obj.addProperty("range", d);
		obj.add("condition", child.toJson(ops));
		return obj;
	}
	
	public Condition fromJson(JsonObject obj, JsonOps ops)
	{
		d = obj.get("range").getAsDouble();
		child = Condition.fromJson(obj.get("child"), ops);
		return this;
	}
	
	public static class Inverse extends Near
	{
		public Inverse(Identifier idIn)
		{
			super(idIn);
		}
		
		public static Inverse of(double distance, Condition childIn)
		{
			Inverse condition = (Inverse)CDTileConditions.AVOID.get();
			condition.d = Math.min(10D, distance);
			condition.child = childIn;
			return condition;
		}

		public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid set)
		{
			return !super.test(tileIn, pos, set);
		}
	}
}