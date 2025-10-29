package com.lying.worldgen;

import java.util.List;

import com.google.common.collect.Lists;
import com.lying.grid.BlueprintTileGrid;

import net.minecraft.util.math.BlockPos;

public class TilePredicate
{
	private final List<TileConditions.Condition> primitives = Lists.newArrayList();
	
	public static TilePredicate fromCondition(TileConditions.Condition condition)
	{
		return Builder.create().condition(condition).build();
	}
	
	public boolean test(Tile tile, BlockPos pos, BlueprintTileGrid set)
	{
		return primitives.isEmpty() || primitives.stream().allMatch(p -> p.test(tile, pos, set));
	}
	
	public static class Builder
	{
		private final List<TileConditions.Condition> conditions = Lists.newArrayList();
		
		protected Builder() { }
		
		public static Builder create() { return new Builder(); }
		
		public Builder condition(TileConditions.Condition condition)
		{
			conditions.add(condition);
			return this;
		}
		
		public TilePredicate build()
		{
			TilePredicate predicate = new TilePredicate();
			conditions.forEach(predicate.primitives::add);
			return predicate;
		}
	}
}
