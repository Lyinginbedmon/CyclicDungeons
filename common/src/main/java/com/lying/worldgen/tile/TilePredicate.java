package com.lying.worldgen.tile;

import java.util.ArrayList;
import java.util.List;

import com.lying.grid.BlueprintTileGrid;
import com.lying.grid.GridVolume;
import com.lying.worldgen.tile.Tile.GridSensitivity;
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
	private final GridSensitivity searchSpace;
	
	protected TilePredicate(List<Condition> conditionsIn)
	{
		/** 
		 * Identify which search spaces this predicate monitors.
		 * This helps us to identify which tiles are evergreen.
		 */
		GridSensitivity space = null;
		for(Condition c : conditionsIn)
		{
			if(space == null || c.searchSpace().value() > space.value())
				space = c.searchSpace();
			primitives.add(c);
		}
		searchSpace = space;
		
		// Sort incoming conditions by cost, so we can check the cheapest ones first
		primitives.sort(Condition.COST_SORT);
	}
	
	public List<Condition> contents() { return primitives; }
	
	public GridSensitivity sensitivity() { return searchSpace; }
	
	public boolean isPlacementAffectedBy(BlockPos offset) { return primitives.stream().anyMatch(c -> c.affectedBy(offset)); }
	
	public static TilePredicate fromCondition(Condition condition)
	{
		return Builder.create().condition(condition).build();
	}
	
	public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid grid, GridVolume volume)
	{
		return primitives.isEmpty() || primitives.stream().allMatch(p -> p.test(tileIn, pos, grid, volume));
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
