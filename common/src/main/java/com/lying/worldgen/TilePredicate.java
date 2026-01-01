package com.lying.worldgen;

import java.util.List;

import com.google.common.collect.Lists;
import com.lying.grid.BlueprintTileGrid;
import com.lying.worldgen.condition.Condition;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.util.math.BlockPos;

public class TilePredicate
{
	public static final Codec<TilePredicate> CODEC	= Condition.CODEC.listOf().comapFlatMap(
			conditions -> DataResult.success(new TilePredicate(conditions)),
			TilePredicate::contents);
	
	private final List<Condition> primitives = Lists.newArrayList();
	
	protected TilePredicate(List<Condition> conditionsIn)
	{
		primitives.addAll(conditionsIn);
	}
	
	public List<Condition> contents() { return primitives; }
	
	public static TilePredicate fromCondition(Condition condition)
	{
		return Builder.create().condition(condition).build();
	}
	
	public boolean test(Tile tile, BlockPos pos, BlueprintTileGrid set)
	{
		return primitives.isEmpty() || primitives.stream().allMatch(p -> p.test(tile, pos, set));
	}
	
	public static class Builder
	{
		private final List<Condition> conditions = Lists.newArrayList();
		
		protected Builder() { }
		
		public static Builder create() { return new Builder(); }
		
		public Builder condition(Condition condition)
		{
			conditions.add(condition);
			return this;
		}
		
		public TilePredicate build()
		{
			return new TilePredicate(conditions);
		}
	}
}
