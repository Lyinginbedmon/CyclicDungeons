package com.lying.worldgen.condition;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lying.grid.BlueprintTileGrid;
import com.lying.init.CDTileConditions;
import com.lying.init.CDTileTags;
import com.lying.worldgen.Tile;
import com.mojang.serialization.JsonOps;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class IsAnyOf extends Condition
{
	protected final List<Identifier> ids = Lists.newArrayList();
	
	public IsAnyOf(Identifier idIn)
	{
		super(idIn);
	}
	
	public static IsAnyOf of(Identifier... idsIn)
	{
		IsAnyOf condition = (IsAnyOf)CDTileConditions.IS_ANY_OF.get();
		for(Identifier id : idsIn)
			if(!condition.ids.contains(id))
				condition.ids.add(id);
		return condition;
	}
	
	public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid set) { return ids.stream().anyMatch(tileIn.registryName()::equals); }
	
	public JsonElement toJson(JsonOps ops)
	{
		JsonObject obj = asJsonObject(ops);
		JsonArray set = new JsonArray();
		ids.forEach(id -> set.add(Identifier.CODEC.encodeStart(ops, id).getOrThrow()));
		obj.add("set", set);
		return obj;
	}
	
	public Condition fromJson(JsonObject obj, JsonOps ops)
	{
		ids.clear();
		JsonArray set = obj.getAsJsonArray("set");
		set.forEach(e -> ids.add(Identifier.CODEC.parse(ops, e).getOrThrow()));
		return this;
	}
	
	public static class HasTag extends IsAnyOf
	{
		public HasTag(Identifier idIn)
		{
			super(idIn);
		}
		
		public static HasTag of(Identifier... idsIn)
		{
			HasTag condition = (HasTag)CDTileConditions.HAS_TAG.get();
			for(Identifier id : idsIn)
				if(!condition.ids.contains(id))
					condition.ids.add(id);
			return condition;
		}
		
		public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid set)
		{
			return ids.stream()
				.map(CDTileTags::get)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.anyMatch(tag -> tag.contains(tileIn));
		}
	}
}