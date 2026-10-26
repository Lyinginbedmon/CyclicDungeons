package com.lying.worldgen.tile.condition;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lying.grid.BlueprintTileGrid;
import com.lying.grid.GridVolume;
import com.lying.init.CDTileConditions;
import com.lying.worldgen.tile.Tile;
import com.lying.worldgen.tile.Tile.GridSensitivity;
import com.mojang.serialization.JsonOps;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public abstract class Boolean extends Condition
{
	protected List<Condition> children = new ArrayList<>();
	protected int totalCost = 0;
	
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
		for(JsonElement e : set)
		{
			Condition c = Condition.fromJson(e, ops);
			totalCost += c.cost();
			children.add(c);
		}
		return this;
	}
	
	public int cost() { return totalCost; }
	
	public GridSensitivity searchSpace()
	{
		GridSensitivity score = GridSensitivity.SPATIAL;
		for(Condition child : children)
		{
			GridSensitivity s = child.searchSpace();
			if(s.value() > score.value())
				score = s;
		}
		return score;
	}
	
	public boolean affectedBy(BlockPos offset) { return children.stream().anyMatch(c -> c.affectedBy(offset)); }
	
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
		
		public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid grid, GridVolume volume) { return children.stream().allMatch(c -> c.test(tileIn, pos, grid, volume)); }
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
		
		public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid grid, GridVolume volume) { return children.stream().anyMatch(c -> c.test(tileIn, pos, grid, volume)); }
	}
}