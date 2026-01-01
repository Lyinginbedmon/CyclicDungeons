package com.lying.init;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import com.lying.CyclicDungeons;
import com.lying.grid.BlueprintTileGrid;
import com.lying.worldgen.Tile;
import com.lying.worldgen.condition.Adjacent;
import com.lying.worldgen.condition.Boolean;
import com.lying.worldgen.condition.Boundary;
import com.lying.worldgen.condition.Condition;
import com.lying.worldgen.condition.Consecutive;
import com.lying.worldgen.condition.IsAnyOf;
import com.lying.worldgen.condition.MaxAdjacentBoundaries;
import com.lying.worldgen.condition.MaxConsecutive;
import com.lying.worldgen.condition.MaxPerRoom;
import com.lying.worldgen.condition.Near;
import com.lying.worldgen.condition.NearBox;
import com.lying.worldgen.condition.Not;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class CDTileConditions
{
	private static final Map<Identifier, Supplier<Condition>> REGISTRY = new HashMap<>();
	
	public static final Supplier<Condition> NEVER			= register("never", id -> new Condition(id)
	{
		public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid set) { return false; }
	});
	public static final Supplier<Condition> ALWAYS			= register("always", id -> new Condition(id)
	{
		public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid set) { return true; }
	});
	
	public static final Supplier<Condition> NOT				= register("not", Not::new);
	public static final Supplier<Condition> AND				= register("and", Boolean.And::new);
	public static final Supplier<Condition> OR				= register("or", Boolean.Or::new);
	
	public static final Supplier<Condition> IS_ANY_OF		= register("is_any_of", IsAnyOf::new);
	public static final Supplier<Condition> HAS_TAG			= register("has_tag", IsAnyOf.HasTag::new);
	public static final Supplier<Condition> ON_FLOOR		= register("on_solid_floor", id -> new Condition(id)
	{
		public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid set)
		{
			BlockPos down = pos.down();
			Optional<Tile> tile;
			return 
				set.contains(down) && 
				(tile = set.get(down)).isPresent() &&
				CDTileTags.SOLID_FLOORING.contains(tile.get());
		}
	});
	public static final Supplier<Condition> ON_BOTTOM		= register("on_bottom", id -> new Condition(id)
	{
		public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid set) { return set.isBoundary(pos, Direction.DOWN); }
	});
	public static final Supplier<Condition> ON_TOP			= register("on_top", id -> new Condition(id)
	{
		public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid set) { return set.isBoundary(pos, Direction.UP); }
	});
	public static final Supplier<Condition> BOUNDARY		= register("boundary", Boundary::new);
	public static final Supplier<Condition> NON_BOUNDARY	= register("non_boundary", Boundary.Inverse::new);
	public static final Supplier<Condition> MAX_TALLY		= register("max_per_room", MaxPerRoom::new);
	public static final Supplier<Condition> MAX_SELF		= register("max_consecutive", MaxConsecutive::new);
	public static final Supplier<Condition> CONSECUTIVE		= register("consecutive", Consecutive::new);
	public static final Supplier<Condition> NON_CONSECUTIVE	= register("nonconsecutive", Consecutive.Inverse::new);
	public static final Supplier<Condition> ADJACENT		= register("adjacent", Adjacent::new);
	public static final Supplier<Condition> NON_ADJACENT	= register("non_adjacent", Adjacent.Inverse::new);
	public static final Supplier<Condition> MAX_ADJACENT	= register("max_adjacent", Adjacent.Capped::new);
	public static final Supplier<Condition> BY_PASSAGE		= register("by_passage", Adjacent.Passage::new);
	public static final Supplier<Condition> NOT_BY_PASSAGE	= register("not_by_passage", Adjacent.Passage.Inverse::new);
	public static final Supplier<Condition> NEAR			= register("near", Near::new);
	public static final Supplier<Condition> AVOID			= register("avoid", Near.Inverse::new);
	public static final Supplier<Condition> NEAR_BOX		= register("near_box", NearBox::new);
	public static final Supplier<Condition> AVOID_BOX		= register("avoid_box", NearBox.Inverse::new);
	public static final Supplier<Condition> MAX_ADJ_BOUNDS	= register("max_adjacent_boundaries", MaxAdjacentBoundaries::new);
	public static final Supplier<Condition> MIN_ADJ_BOUNDS	= register("min_adjacent_boundaries", MaxAdjacentBoundaries.Inverse::new);
	
	private static Supplier<Condition> register(String name, Function<Identifier, Condition> funcIn)
	{
		return register(prefix(name), funcIn);
	}
	
	public static Supplier<Condition> register(Identifier id, Function<Identifier, Condition> funcIn)
	{
		Supplier<Condition> supplier = () -> funcIn.apply(id);
		REGISTRY.put(id, supplier);
		return supplier;
	}
	
	public static void init()
	{
		CyclicDungeons.LOGGER.info("# Initialised {} tile conditions", REGISTRY.size());
	}
	
	@NotNull
	public static Condition get(Identifier id) { return REGISTRY.getOrDefault(id, NEVER).get(); }
}