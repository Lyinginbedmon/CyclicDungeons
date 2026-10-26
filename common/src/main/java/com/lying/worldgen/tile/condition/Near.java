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
	
	public int cost() { return 8; }
	
	public GridSensitivity searchSpace() { return GridSensitivity.REGIONAL; }
	
	public boolean affectedBy(BlockPos offset) { return offset.isWithinDistance(BlockPos.ZERO, d); }
	
	public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid grid, GridVolume volume)
	{
		for(int x=-(int)d; x<d; x++)
			for(int z=-(int)d; z<d; z++)
				for(int y=-(int)d; y<d; y++)
				{
					// Ignore the zero index, since that's where we are
					if(x == y && y == z && x == 0)
						continue;
					
					BlockPos offset = pos.add(x, y, z);
					if(!volume.contains(offset) || grid.isEmpty(offset) || !offset.isWithinDistance(pos, d))
						continue;
					
					Tile tileAt = grid.get(offset).get();
					if(child.test(tileAt, offset, grid, volume))
						return true;
				}
		
		return false;
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

		public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid grid, GridVolume volume)
		{
			return !super.test(tileIn, pos, grid, volume);
		}
	}
}