package com.lying.worldgen.condition;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lying.grid.BlueprintTileGrid;
import com.lying.init.CDTileConditions;
import com.lying.worldgen.Tile;
import com.mojang.serialization.JsonOps;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class Boundary extends Condition
{
	protected final List<Direction> faces = Lists.newArrayList(Direction.stream().iterator());
	
	public Boundary(Identifier idIn)
	{
		super(idIn);
	}
	
	public static Boundary of(Direction.Type type)
	{
		return of(type.stream().toList());
	}
	
	public static Boundary of(List<Direction> facesIn)
	{
		Boundary condition = (Boundary)CDTileConditions.BOUNDARY.get();
		condition.faces.clear();
		condition.faces.addAll(facesIn);
		return condition;
	}
	
	public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid set)
	{
		return faces.stream().anyMatch(d -> set.isBoundary(pos, d));
	}
	
	public JsonElement toJson(JsonOps ops)
	{
		JsonObject obj = asJsonObject(ops);
		obj.add("sides", FACE_LIST_CODEC.encodeStart(ops, faces).getOrThrow());
		return obj;
	}
	
	public Condition fromJson(JsonObject obj, JsonOps ops)
	{
		faces.clear();
		faces.addAll(FACE_LIST_CODEC.parse(ops, obj.get("sides")).getOrThrow());
		return this;
	}
	
	public static class Inverse extends Boundary
	{
		public Inverse(Identifier idIn)
		{
			super(idIn);
		}
		
		public static Inverse of(Direction.Type type)
		{
			return of(type.stream().toList());
		}
		
		public static Inverse of(List<Direction> facesIn)
		{
			Inverse condition = (Inverse)CDTileConditions.NON_BOUNDARY.get();
			condition.faces.clear();
			condition.faces.addAll(facesIn);
			return condition;
		}
		
		public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid set)
		{
			return faces.stream().noneMatch(d -> set.isBoundary(pos, d));
		}
	}
}