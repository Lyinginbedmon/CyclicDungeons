package com.lying.worldgen.tile;

import java.util.ArrayList;
import java.util.List;

import com.lying.grid.BlueprintTileGrid;
import com.lying.worldgen.tile.condition.Condition;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.util.math.BlockPos;

public class TilePredicate
{
	public static final Codec<TilePredicate> CODEC	= Condition.CODEC.listOf().comapFlatMap(
			conditions -> DataResult.success(new TilePredicate(conditions)),
			TilePredicate::contents);
	
	private final List<Condition> primitives = new ArrayList<>();
	
	protected TilePredicate(List<Condition> conditionsIn)
	{
		primitives.addAll(conditionsIn);
	}
	
	public List<Condition> contents() { return primitives; }
	
	public static TilePredicate fromCondition(Condition condition)
	{
		return Builder.create().condition(condition).build();
	}
	
	public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid grid)
	{
		return primitives.isEmpty() || primitives.stream().allMatch(p -> p.test(tileIn, pos, grid));
	}
	
	public static class Builder
	{
		private final List<Condition> conditions = new ArrayList<>();
		
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
