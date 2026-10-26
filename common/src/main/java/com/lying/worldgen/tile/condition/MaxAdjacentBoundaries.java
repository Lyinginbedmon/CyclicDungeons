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

public class MaxAdjacentBoundaries extends Condition
{
	protected List<Direction> faces = Lists.newArrayList(Direction.stream().iterator());
	protected int i = 0;
	
	public MaxAdjacentBoundaries(Identifier idIn)
	{
		super(idIn);
	}
	
	public static MaxAdjacentBoundaries of(int val)
	{
		return of(Direction.stream().toList(), val);
	}
	
	public int cost() { return 2; }
	
	public GridSensitivity searchSpace() { return GridSensitivity.SPATIAL; }
	
	public boolean affectedBy(BlockPos offset) { return false; }
	
	public static MaxAdjacentBoundaries of(List<Direction> facesIn, int val)
	{
		MaxAdjacentBoundaries condition = (MaxAdjacentBoundaries)CDTileConditions.MAX_ADJ_BOUNDS.get();
		condition.setFaces(facesIn);
		condition.i = val;
		return condition;
	}
	
	protected void setFaces(List<Direction> facesIn)
	{
		faces.clear();
		faces.addAll(facesIn);
	}
	
	public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid grid, GridVolume volume)
	{
		int tally = 0;
		BlockPos offset;
		for(Direction face : faces)
			if(volume.contains(offset = pos.offset(face)) && volume.isBoundary(offset, face))
				if(++tally >= i)
					return false;
		return true;
	}
	
	public JsonElement toJson(JsonOps ops)
	{
		JsonObject obj = asJsonObject(ops);
		obj.add("sides", FACE_LIST_CODEC.encodeStart(ops, faces).getOrThrow());
		obj.addProperty("cap", i);
		return obj;
	}
	
	public Condition fromJson(JsonObject obj, JsonOps ops)
	{
		faces.clear();
		faces.addAll(FACE_LIST_CODEC.parse(ops, obj.get("sides")).getOrThrow());
		i = obj.get("cap").getAsInt();
		return this;
	}
	
	public static class Inverse extends MaxAdjacentBoundaries
	{
		public Inverse(Identifier idIn)
		{
			super(idIn);
		}
		
		public static Inverse of(int val)
		{
			return of(Direction.stream().toList(), val);
		}
		
		public static Inverse of(List<Direction> facesIn, int val)
		{
			Inverse condition = (Inverse)CDTileConditions.MIN_ADJ_BOUNDS.get();
			condition.setFaces(facesIn);
			condition.i = val;
			return condition;
		}
		
		public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid set, GridVolume volume)
		{
			int tally = 0;
			BlockPos offset;
			for(Direction face : faces)
				if(volume.contains(offset = pos.offset(face)) && volume.isBoundary(offset, face))
					if(++tally >= i)
						return true;
			return false;
		}
	}
}