package com.lying.worldgen.tile.condition;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lying.grid.BlueprintTileGrid;
import com.lying.init.CDTileConditions;
import com.lying.worldgen.tile.Tile;
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
	
	public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid set)
	{
		return faces.stream()
				.map(pos::offset)
				.filter(set::contains)
				.map(set::get)
				.map(Optional::get)
				.filter(t -> !t.isBlank())
				.anyMatch(tileIn::is);
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
		
		public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid set)
		{
			return faces.stream()
					.map(pos::offset)
					.filter(set::contains)
					.map(set::get)
					.map(Optional::get)
					.filter(t -> !t.isBlank())
					.noneMatch(tileIn::is);
		}
	}
}