package com.lying.worldgen.condition;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lying.grid.BlueprintTileGrid;
import com.lying.init.CDTileConditions;
import com.lying.worldgen.Tile;
import com.mojang.serialization.JsonOps;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public abstract class Boolean extends Condition
{
	protected List<Condition> children = Lists.newArrayList();
	
	protected Boolean(Identifier idIn)
	{
		super(idIn);
	}
	
	public JsonElement toJson(JsonOps ops)
	{
		JsonObject obj = asJsonObject(ops);
		JsonArray set = new JsonArray();
		obj.add("sub", set);
		return obj;
	}
	
	public Condition fromJson(JsonObject obj, JsonOps ops)
	{
		JsonArray set = obj.getAsJsonArray("sub");
		set.forEach(e -> children.add(Condition.fromJson(e, ops)));
		return this;
	}
	
	public static class And extends Boolean
	{
		public And(Identifier idIn)
		{
			super(idIn);
		}
		
		public static And of(Condition... childrenIn)
		{
			And condition = (And)CDTileConditions.AND.get();
			for(Condition child : childrenIn)
				condition.children.add(child);
			return condition;
		}
		
		public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid set) { return children.stream().allMatch(c -> c.test(tileIn, pos, set)); }
	}
	
	public static class Or extends Boolean
	{
		public Or(Identifier idIn)
		{
			super(idIn);
		}
		
		public static Or of(Condition... childrenIn)
		{
			Or condition = (Or)CDTileConditions.OR.get();
			for(Condition child : childrenIn)
				condition.children.add(child);
			return condition;
		}
		
		public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid set) { return children.stream().anyMatch(c -> c.test(tileIn, pos, set)); }
	}
}