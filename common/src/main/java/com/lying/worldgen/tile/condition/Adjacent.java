package com.lying.worldgen.tile.condition;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lying.grid.BlueprintTileGrid;
import com.lying.init.CDTileConditions;
import com.lying.init.CDTiles;
import com.lying.worldgen.tile.Tile;
import com.mojang.serialization.JsonOps;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class Adjacent extends Condition
{
	protected List<Direction> faces = Lists.newArrayList(Direction.values());
	protected Condition child = CDTileConditions.NEVER.get();
	
	public Adjacent(Identifier idIn)
	{
		super(idIn);
	}
	
	public static Adjacent of(Condition childIn)
	{
		Adjacent condition = (Adjacent)CDTileConditions.ADJACENT.get();
		condition.child = childIn;
		return condition;
	}
	
	public static Adjacent of(Direction.Type facesIn, Condition childIn)
	{
		return of(facesIn.stream().toList(), childIn);
	}
	
	public static Adjacent of(List<Direction> facesIn, Condition childIn)
	{
		Adjacent condition = of(childIn);
		condition.setFaces(facesIn);
		return condition;
	}
	
	protected void setFaces(List<Direction> facesIn)
	{
		faces.clear();
		faces.addAll(facesIn);
	}
	
	public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid set)
	{
		return faces.stream()
				.map(pos::offset)
				.filter(set::contains)
				.anyMatch(p -> child.test(set.get(p).get(), p, set));
	}
	
	public JsonElement toJson(JsonOps ops)
	{
		JsonObject obj = asJsonObject(ops);
		if(faces.size() < Direction.values().length)
			obj.add("sides", FACE_LIST_CODEC.encodeStart(ops, faces).getOrThrow());
		obj.add("condition", child.toJson(ops));
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
		child = Condition.fromJson(obj.get("condition"), ops);
		return this;
	}
	
	public static class Inverse extends Adjacent
	{
		public Inverse(Identifier idIn)
		{
			super(idIn);
		}
		
		public static Inverse of(Condition childIn)
		{
			Inverse condition = (Inverse)CDTileConditions.NON_ADJACENT.get();
			condition.child = childIn;
			return condition;
		}
		
		public static Inverse of(Direction.Type facesIn, Condition childIn)
		{
			return of(facesIn.stream().toList(), childIn);
		}
		
		public static Inverse of(List<Direction> facesIn, Condition childIn)
		{
			Inverse condition = of(childIn);
			condition.setFaces(facesIn);
			return condition;
		}
		
		public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid set)
		{
			return faces.stream()
					.map(pos::offset)
					.filter(set::contains)
					.noneMatch(p -> child.test(set.get(p).get(), p, set));
		}
	}
	
	public static class Capped extends Adjacent
	{
		protected int i = 1;
		
		public Capped(Identifier idIn)
		{
			super(idIn);
		}
		
		public static Capped of(Condition childIn, int val)
		{
			return of(Direction.stream().toList(), childIn, val);
		}
		
		public static Capped of(List<Direction> facesIn, Condition childIn, int val)
		{
			Capped condition = (Capped)CDTileConditions.MAX_ADJACENT.get();
			condition.setFaces(facesIn);
			condition.child = childIn;
			condition.i = val;
			return condition;
		}
		
		public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid set)
		{
			return (int)faces.stream()
					.map(f -> pos.offset(f))
					.filter(set::contains)
					.filter(p -> set.get(p).isPresent())
					.filter(p -> 
					{
						Tile tile = set.get(p).get();
						return !tile.isBlank() && child.test(tile, p, set);
					})
					.count() < i;
		}
		
		public JsonElement toJson(JsonOps ops)
		{
			JsonObject obj = super.toJson(ops).getAsJsonObject();
			obj.addProperty("cap", i);
			return obj;
		}
		
		public Condition fromJson(JsonObject obj, JsonOps ops)
		{
			super.fromJson(obj, ops);
			i = obj.get("cap").getAsInt();
			return this;
		}
	}
	
	public static class Passage extends Adjacent
	{
		public Passage(Identifier idIn)
		{
			super(idIn);
		}
		
		public static Passage of(Direction.Type facesIn)
		{
			return of(facesIn.stream().toList());
		}
		
		public static Passage of(List<Direction> facesIn)
		{
			Passage condition = (Passage)CDTileConditions.BY_PASSAGE.get();
			condition.setFaces(facesIn);
			return condition;
		}

		public JsonElement toJson(JsonOps ops)
		{
			if(faces.size() == Direction.values().length)
				return Identifier.CODEC.encodeStart(ops, id).getOrThrow();
			
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
		
		public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid set)
		{
			return faces.stream()
					.map(pos::offset)
					.filter(set::contains)
					.map(set::get)
					.map(Optional::get)
					.map(Tile::registryName)
					.anyMatch(CDTiles.ID_PASSAGE_FLAG::equals);
		}
		
		public static class Inverse extends Passage
		{
			public Inverse(Identifier idIn)
			{
				super(idIn);
			}
			
			public static Inverse of(Direction.Type facesIn)
			{
				return of(facesIn.stream().toList());
			}
			
			public static Inverse of(List<Direction> facesIn)
			{
				Inverse condition = (Inverse)CDTileConditions.NOT_BY_PASSAGE.get();
				condition.setFaces(facesIn);
				return condition;
			}
			
			public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid set)
			{
				return faces.stream()
						.map(pos::offset)
						.filter(set::contains)
						.map(set::get)
						.map(Optional::get)
						.map(Tile::registryName)
						.noneMatch(CDTiles.ID_PASSAGE_FLAG::equals);
			}
		}
	}
}