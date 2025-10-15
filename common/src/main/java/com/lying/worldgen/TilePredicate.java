package com.lying.worldgen;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.google.common.collect.Lists;
import com.lying.init.CDTileTags;
import com.lying.init.CDTileTags.TileTag;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class TilePredicate
{
	private final List<TileCondition> primitives = Lists.newArrayList();
	
	public boolean test(Tile tile, BlockPos pos, TileSet set)
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
	
	/** Returns a predicate that matches an input Tile against the supplied list of tiles */
	public static Predicate<Tile> tileAnyMatch(List<Supplier<Tile>> tiles)
	{
		return t -> tiles.stream().map(Supplier::get).filter(t2 -> !t2.isBlank()).anyMatch(t::is);
	}
	
	/** Returns a predicate that matches an input Tile against the supplied list of tags */
	public static Predicate<Tile> tagAnyMatch(List<TileTag> tags)
	{
		return t -> tags.stream().anyMatch(t::isIn);
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
			return adjacent(List.of(Direction.DOWN), CDTileTags.SOLID_FLOORING::contains);
		}
		
		public Builder adjacent(Predicate<Tile> predicate)
		{
			return adjacent(Direction.stream().toList(), predicate);
		}
		
		public Builder adjacent(List<Direction> faces, Predicate<Tile> predicate)
		{
			conditions.add(Conditions.adjacent(faces, predicate));
			return this;
		}
		
		public Builder nonAdjacent(Predicate<Tile> predicate)
		{
			return nonAdjacent(Direction.stream().toList(), predicate);
		}
		
		public Builder nonAdjacent(List<Direction> faces, Predicate<Tile> predicate)
		{
			conditions.add(Conditions.nonAdjacent(faces, predicate));
			return this;
		}
		
		public Builder nonConsecutive()
		{
			return nonConsecutive(Direction.stream().toList());
		}
		
		public Builder nonConsecutive(Box box)
		{
			conditions.add(Conditions.nonConsecutive(box));
			return this;
		}
		
		public Builder nonConsecutive(List<Direction> faces)
		{
			conditions.add(Conditions.nonConsecutive(faces));
			return this;
		}
		
		public Builder near(Box box, Predicate<Tile> predicate)
		{
			conditions.add(Conditions.near(box, predicate));
			return this;
		}
		
		public Builder avoid(Box box, Predicate<Tile> predicate)
		{
			conditions.add(Conditions.avoid(box, predicate));
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
			public static TileCondition adjacent(List<Direction> faces, Predicate<Tile> predicate)
			{
				return (tile, pos, set) -> faces.stream()
						.map(face -> pos.offset(face))
						.filter(set::contains)
						.map(set::get)
						.map(Optional::get)
						.filter(tile2 -> !tile2.isBlank())
						.anyMatch(predicate);
			}
			
			/** Within bounded range of N */
			public static TileCondition near(Box bounds, Predicate<Tile> predicate)
			{
				// Cap the size of the search area to reduce lag
				int lX = (int)(Math.min(10, bounds.getLengthX()) * 0.5D);
				int lY = (int)(Math.min(10, bounds.getLengthY()) * 0.5D);
				int lZ = (int)(Math.min(10, bounds.getLengthZ()) * 0.5D);
				final Box b = Box.enclosing(new BlockPos(-lX,-lY,-lZ), new BlockPos(lX,lY,lZ));
				return (tile, pos, set) -> 
				{
					final Box box = bounds.offset(pos);
					return !set.getTiles((p2,t2) -> box.contains(new Vec3d(p2.getX() + 0.5D, p2.getY() + 0.5D, p2.getZ() + 0.5D)) && predicate.test(t2)).isEmpty();
				};
			}
			
			/** Within Y range of N */
			public static TileCondition near(double distance, Predicate<Tile> predicate)
			{
				final double d = Math.min(10D, distance);
				return (t,p,s) -> !s.getTiles((p2,t2) -> p2.isWithinDistance(p, d) && predicate.test(t2)).isEmpty();
			}
			
			/** Not within bounded range of N */
			public static TileCondition avoid(Box bounds, Predicate<Tile> tiles)
			{
				return (t,p,s) -> not(near(bounds, tiles)).test(t, p, s);
			}
			
			/** Not within Y range of N */
			public static TileCondition avoid(double distance, Predicate<Tile> tiles)
			{
				return (t,p,s) -> not(near(distance, tiles)).test(t,p,s);
			}
			
			/** Adjacent to self */
			public static TileCondition consecutive(List<Direction> faces)
			{
				return (t,p,s) -> adjacent(faces, t::is).test(t, p, s);
			}
			
			/** Not adjacent to N */
			public static TileCondition nonAdjacent(List<Direction> faces, Predicate<Tile> tiles)
			{
				return (t,p,s) -> not(adjacent(faces, tiles)).test(t, p, s);
			}
			
			/** Not adjacent to self */
			public static TileCondition nonConsecutive(List<Direction> faces)
			{
				return (t,p,s) -> not(adjacent(faces, t::is)).test(t, p, s);
			}
			
			/** Not adjacent to self */
			public static TileCondition nonConsecutive(Box box)
			{
				return (t,p,s) -> avoid(box, t::is).test(t, p, s);
			}
		}
	}
}
