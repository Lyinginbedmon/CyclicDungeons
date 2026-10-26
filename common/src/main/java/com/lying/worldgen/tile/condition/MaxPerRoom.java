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
	
	public int cost() { return 5; }
	
	public GridSensitivity searchSpace() { return GridSensitivity.REGIONAL; }
	
	public boolean affectedBy(BlockPos offset) { return true; }
	
	public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid grid, GridVolume volume)
	{
		return volume.tallyOf(tileIn) < i;
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