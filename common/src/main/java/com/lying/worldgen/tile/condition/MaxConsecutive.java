package com.lying.worldgen.tile.condition;

import java.util.List;

import com.google.common.base.Predicates;
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

public class MaxConsecutive extends Condition
{
	private List<Direction> faces = Lists.newArrayList(Direction.stream().iterator());
	private int i = 0;
	
	public MaxConsecutive(Identifier idIn)
	{
		super(idIn);
	}
	
	public MaxConsecutive(Identifier idIn, List<Direction> facesIn, int val)
	{
		super(idIn);
		faces = facesIn;
		i = val;
	}
	
	public static MaxConsecutive of(List<Direction> facesIn, int val)
	{
		MaxConsecutive condition = (MaxConsecutive)CDTileConditions.MAX_SELF.get();
		condition.faces.clear();
		condition.faces.addAll(facesIn);
		condition.i = val;
		return condition;
	}
	
	public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid set)
	{
		return (int)faces.stream()
				.map(f -> pos.offset(f))
				.filter(set::contains)
				.map(p -> set.get(p).get())
				.filter(Predicates.not(Tile::isBlank))
				.filter(tileIn::is)
				.count() < i;
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
}