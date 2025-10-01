package com.lying.worldgen;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.google.common.collect.Lists;
import com.lying.init.CDTiles;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

public class TilePredicate
{
	private final List<TileCondition> primitives = Lists.newArrayList();
	
	public boolean canExistAt(Tile tile, BlockPos pos, TileSet set)
	{
		return primitives.isEmpty() || primitives.stream().allMatch(p -> p.test(tile, pos, set));
	}
	
	protected void add(TileCondition condition)
	{
		primitives.add(condition);
	}
	
	@FunctionalInterface
	public static interface TileCondition
	{
		public boolean test(Tile tileIn, BlockPos pos, TileSet set);
	}
	
	public static class Builder
	{
		private final List<TileCondition> conditions = Lists.newArrayList();
		
		protected Builder() { }
		
		public static Builder create() { return new Builder(); }
		
		public Builder always()
		{
			conditions.add(Conditions.always());
			return this;
		}
		
		public Builder never()
		{
			conditions.add(Conditions.never());
			return this;
		}
		
		public Builder nonBoundary()
		{
			conditions.add(Conditions.not(Conditions.boundary(Direction.stream().toList())));
			return this;
		}
		
		public Builder boundary(Direction.Type faces)
		{
			conditions.add(Conditions.boundary(faces.stream().toList()));
			return this;
		}
		
		public Builder boundary(List<Direction> faces)
		{
			conditions.add(Conditions.boundary(faces));
			return this;
		}
		
		public Builder onFloor()
		{
			return adjacent(List.of(Direction.DOWN), List.of(CDTiles.FLOOR));
		}
		
		public Builder adjacent(List<Supplier<Tile>> tiles)
		{
			return adjacent(Direction.stream().toList(), tiles);
		}
		
		public Builder adjacent(List<Direction> faces, List<Supplier<Tile>> tiles)
		{
			conditions.add(Conditions.adjacent(faces, tiles));
			return this;
		}
		
		public Builder nonAdjacent(List<Supplier<Tile>> tiles)
		{
			return nonAdjacent(Direction.stream().toList(), tiles);
		}
		
		public Builder nonAdjacent(List<Direction> faces, List<Supplier<Tile>> tiles)
		{
			conditions.add(Conditions.nonAdjacent(faces,tiles));
			return this;
		}
		
		public Builder nonConsecutive()
		{
			return nonConsecutive(Direction.stream().toList());
		}
		
		public Builder nonConsecutive(List<Direction> faces)
		{
			conditions.add(Conditions.nonConsecutive(faces));
			return this;
		}
		
		public Builder near(Box box, List<Supplier<Tile>> tiles)
		{
			conditions.add(Conditions.near(box, tiles));
			return this;
		}
		
		public Builder avoid(Box box, List<Supplier<Tile>> tiles)
		{
			conditions.add(Conditions.avoid(box, tiles));
			return this;
		}
		
		public TilePredicate build()
		{
			TilePredicate predicate = new TilePredicate();
			conditions.forEach(predicate::add);
			return predicate;
		}
		
		@SuppressWarnings("unused")
		private static class Conditions
		{
			public static TileCondition always() { return (t,p,s) -> true; }
			
			public static TileCondition never() { return (t,p,s) -> false; }
			
			public static TileCondition not(TileCondition cond)
			{
				return (t,p,s) -> !cond.test(t, p, s);
			}
			
			public static TileCondition boundary(List<Direction> faces)
			{
				return (t,p,s) -> faces.stream()
						.anyMatch(d -> s.isBoundary(p, d));
			}
			
			/** Adjacent to N */
			public static TileCondition adjacent(List<Direction> faces, List<Supplier<Tile>> tiles)
			{
				return (tile, pos, set) -> faces.stream()
						.map(face -> pos.offset(face))
						.filter(set::contains)
						.map(set::get)
						.filter(tile2 -> !tile2.isBlank())
						.anyMatch(tileAnyMatch(tiles));
			}
			
			/** Withing Y range of N */
			public static TileCondition near(Box bounds, List<Supplier<Tile>> tiles)
			{
				return (tile, pos, set) -> 
				{
					List<BlockPos> positions = Lists.newArrayList();
					BlockPos.Mutable.iterate(bounds).forEach(p -> positions.add(p.toImmutable().add(pos)));
					
					return positions.stream()
							.filter(set::contains)
							.map(set::get)
							.filter(t2 -> !t2.isBlank())
							.anyMatch(tileAnyMatch(tiles));
				};
			}
			
			/** Adjacent to self */
			public static TileCondition consecutive(List<Direction> faces)
			{
				return (t,p,s) -> adjacent(faces, List.of(() -> t)).test(t, p, s);
			}
			
			/** Not adjacent to N */
			public static TileCondition nonAdjacent(List<Direction> faces, List<Supplier<Tile>> tiles)
			{
				return (t,p,s) -> not(adjacent(faces, tiles)).test(t, p, s);
			}
			
			/** Not adjacent to self */
			public static TileCondition nonConsecutive(List<Direction> faces)
			{
				return (t,p,s) -> not(adjacent(faces, List.of(() -> t))).test(t, p, s);
			}
			
			/** Not within Y range of N */
			public static TileCondition avoid(Box bounds, List<Supplier<Tile>> tiles)
			{
				return (t,p,s) -> not(near(bounds, tiles)).test(t, p, s);
			}
			
			public static Predicate<Tile> tileAnyMatch(List<Supplier<Tile>> tiles)
			{
				return t -> tiles.stream().map(Supplier::get).anyMatch(t::is);
			}
		}
	}
}
