package com.lying.worldgen.tile.condition;

import java.util.List;

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

public class Boundary extends Condition
{
	protected List<Direction> faces = List.of(Direction.values());
	
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
		condition.faces = facesIn;
		return condition;
	}
	
	public int cost() { return 1; }
	
	public GridSensitivity searchSpace() { return GridSensitivity.SPATIAL; }
	
	public boolean affectedBy(BlockPos offset) { return false; }
	
	public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid grid, GridVolume volume)
	{
		for(Direction face : faces)
			if(volume.isBoundary(pos, face))
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
		faces = FACE_LIST_CODEC.parse(ops, obj.get("sides")).getOrThrow();
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
			condition.faces = facesIn;
			return condition;
		}
		
		public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid set, GridVolume volume)
		{
			return faces.stream().noneMatch(d -> volume.isBoundary(pos, d));
		}
	}
}