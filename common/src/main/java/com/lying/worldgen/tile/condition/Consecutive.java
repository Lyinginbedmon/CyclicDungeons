package com.lying.worldgen.tile.condition;

import java.util.List;

import com.google.common.collect.Lists;
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
import net.minecraft.util.math.Direction;

public class Consecutive extends Condition
{
	protected List<Direction> faces = Lists.newArrayList(Direction.values());
	
	public Consecutive(Identifier idIn)
	{
		super(idIn);
	}
	
	public static Consecutive of(List<Direction> facesIn)
	{
		Consecutive condition = (Consecutive)CDTileConditions.CONSECUTIVE.get();
		condition.faces.clear();
		condition.faces.addAll(facesIn);
		return condition;
	}
	
	public int cost() { return 1; }
	
	public GridSensitivity searchSpace() { return GridSensitivity.ADJACENT; }
	
	public boolean affectedBy(BlockPos offset)
	{
		return faces.contains(Direction.fromVector(offset, Direction.NORTH));
	}
	
	public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid grid, GridVolume volume)
	{
		BlockPos offset;
		for(Direction face : faces)
			if(!volume.contains(offset = pos.offset(face)) || grid.isEmpty(offset))
				continue;
			else if(tileIn.is(grid.get(offset).get()))
				return true;
		return false;
	}
	
	public JsonElement toJson(JsonOps ops)
	{
		if(faces.size() == Direction.values().length)
			return super.toJson(ops);
		
		JsonObject obj = asJsonObject(ops);
		obj.add("sides", FACE_LIST_CODEC.encodeStart(ops, faces).getOrThrow());
		return obj;
	}
	
	public Condition fromJson(JsonObject obj, JsonOps ops)
	{
		if(obj.has("sides"))
		{
			faces.clear();
			faces.addAll(FACE_LIST_CODEC.parse(ops, obj.get("sides")).getOrThrow());
		}
		else
			faces = Lists.newArrayList(Direction.values());
		return this;
	}
	
	public static class Inverse extends Consecutive
	{
		public Inverse(Identifier idIn)
		{
			super(idIn);
		}
		
		public static Inverse of(List<Direction> facesIn)
		{
			Inverse condition = (Inverse)CDTileConditions.NON_CONSECUTIVE.get();
			condition.faces.clear();
			condition.faces.addAll(facesIn);
			return condition;
		}
		
		public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid grid, GridVolume volume)
		{
			return !super.test(tileIn, pos, grid, volume);
		}
	}
}