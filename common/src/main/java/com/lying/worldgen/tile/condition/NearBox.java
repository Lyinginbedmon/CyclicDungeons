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

public class NearBox extends Condition
{
	protected BlockPos min = BlockPos.ORIGIN, max = BlockPos.ORIGIN;
	protected Condition child = CDTileConditions.NEVER.get();
	
	public NearBox(Identifier idIn)
	{
		super(idIn);
	}
	
	public static NearBox of(BlockPos minIn, BlockPos maxIn, Condition childIn)
	{
		NearBox condition = (NearBox)CDTileConditions.NEAR_BOX.get();
		condition.min = new BlockPos(
				Math.min(minIn.getX(), maxIn.getX()),
				Math.min(minIn.getY(), maxIn.getY()),
				Math.min(minIn.getZ(), maxIn.getZ()));
		condition.max = new BlockPos(
				Math.max(minIn.getX(), maxIn.getX()),
				Math.max(minIn.getY(), maxIn.getY()),
				Math.max(minIn.getZ(), maxIn.getZ()));
		condition.child = childIn;
		return condition;
	}
	
	public int cost() { return 8; }
	
	public GridSensitivity searchSpace() { return GridSensitivity.REGIONAL; }
	
	public boolean affectedBy(BlockPos offset)
	{
		if(offset.getX() < min.getX() || offset.getX() > max.getX())
			return false;
		
		if(offset.getY() < min.getY() || offset.getY() > max.getY())
			return false;
		
		if(offset.getZ() < min.getZ() || offset.getZ() > max.getZ())
			return false;
		
		return true;
	}
	
	public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid grid, GridVolume volume)
	{
		for(int x=min.getX(); x<max.getX(); x++)
			for(int z=min.getZ(); z<max.getZ(); z++)
				for(int y=min.getY(); y<max.getY(); y++)
				{
					BlockPos offset = pos.add(x, y, z);
					if(volume.contains(offset) && !grid.isEmpty(offset) && child.test(grid.get(offset).get(), offset, grid, volume))
						return true;
				}
		return false;
	}
	
	public JsonElement toJson(JsonOps ops)
	{
		JsonObject obj = asJsonObject(ops);
		obj.add("min", BlockPos.CODEC.encodeStart(ops, min).getOrThrow());
		obj.add("max", BlockPos.CODEC.encodeStart(ops, max).getOrThrow());
		obj.add("condition", child.toJson(ops));
		return obj;
	}
	
	public Condition fromJson(JsonObject obj, JsonOps ops)
	{
		min = BlockPos.CODEC.parse(ops, obj.get("min")).getOrThrow();
		max = BlockPos.CODEC.parse(ops, obj.get("max")).getOrThrow();
		child = Condition.fromJson(obj.get("condition"), ops);
		return this;
	}
	
	public static class Inverse extends NearBox
	{
		public Inverse(Identifier idIn)
		{
			super(idIn);
		}
		
		public static Inverse of(BlockPos minIn, BlockPos maxIn, Condition childIn)
		{
			Inverse condition = (Inverse)CDTileConditions.AVOID_BOX.get();
			condition.min = new BlockPos(
					Math.min(minIn.getX(), maxIn.getX()),
					Math.min(minIn.getY(), maxIn.getY()),
					Math.min(minIn.getZ(), maxIn.getZ()));
			condition.max = new BlockPos(
					Math.max(minIn.getX(), maxIn.getX()),
					Math.max(minIn.getY(), maxIn.getY()),
					Math.max(minIn.getZ(), maxIn.getZ()));
			condition.child = childIn;
			return condition;
		}
		
		public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid grid, GridVolume volume)
		{
			return !super.test(tileIn, pos, grid, volume);
		}
	}
}