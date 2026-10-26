package com.lying.worldgen.tile.condition;

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
	
	public int cost() { return child.cost(); }
	
	public GridSensitivity searchSpace() { return child.searchSpace(); }
	
	public boolean affectedBy(BlockPos offset) { return child.affectedBy(offset); }
	
	public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid grid, GridVolume volume) { return !child.test(tileIn, pos, grid, volume); }
	
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