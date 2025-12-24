package com.lying.worldgen;

import java.util.List;
import java.util.function.Predicate;

import com.google.common.base.Predicates;
import com.lying.grid.BlueprintTileGrid;
import com.lying.init.CDTileTags;
import com.lying.init.CDTiles;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class TileConditions
{
	@FunctionalInterface
	public static interface Condition
	{
		public boolean test(Tile tileIn, BlockPos pos, BlueprintTileGrid set);
	}
	
	public static Predicate<Tile> isAnyOf(Identifier... idsIn)
	{
		return t -> 
		{
			Identifier id = t.registryName();
			for(Identifier test : idsIn)
				if(id.equals(test))
					return true;
			return false;
		};
	}
	
	public static Condition always() { return (t,p,s) -> true; }
	
	public static Condition never() { return (t,p,s) -> false; }
	
	public static Condition not(Condition cond) { return (t,p,s) -> !cond.test(t, p, s); }
	
	public static Condition boundary(Direction.Type type)
	{
		return boundary(type.stream().toList());
	}
	
	public static Condition boundary(List<Direction> faces)
	{
		return (t,p,s) -> faces.stream()
				.anyMatch(d -> s.isBoundary(p, d));
	}
	
	public static Condition nonBoundary()
	{
		return not(boundary(Direction.stream().toList()));
	}
	
	public static Condition onFloor()
	{
		return adjacent(List.of(Direction.DOWN), CDTileTags.SOLID_FLOORING::contains);
	}
	
	public static Condition onBottomLayer()
	{
		return boundary(List.of(Direction.DOWN));
	}
	
	public static Condition onTopLayer()
	{
		return boundary(List.of(Direction.UP));
	}
	
	public static Condition nonPassage()
	{
		return not(passage());
	}
	
	public static Condition passage()
	{
		return adjacent(t -> t.registryName().equals(CDTiles.ID_PASSAGE));
	}
	
	public static Condition adjacent(Predicate<Tile> predicate)
	{
		return adjacent(Direction.stream().toList(), predicate);
	}
	
	public static Condition adjacent(Direction.Type type, Predicate<Tile> predicate)
	{
		return adjacent(type.stream().toList(), predicate);
	}
	
	/** Adjacent to N */
	public static Condition adjacent(List<Direction> faces, Predicate<Tile> predicate)
	{
		return (tile, pos, set) -> faces.stream()
				.map(f -> pos.offset(f))
				.filter(set::contains)
				.map(p -> set.get(p).get())
				.filter(Predicates.not(Tile::isBlank))
				.anyMatch(predicate);
	}
	
	/** Within bounded range of N */
	public static Condition near(Box bounds, Predicate<Tile> predicate)
	{
		// Cap the size of the search area to reduce lag
		int lX = (int)(Math.min(10, bounds.getLengthX()) * 0.5D);
		int lY = (int)(Math.min(10, bounds.getLengthY()) * 0.5D);
		int lZ = (int)(Math.min(10, bounds.getLengthZ()) * 0.5D);
		final Box b = Box.enclosing(new BlockPos(-lX,-lY,-lZ), new BlockPos(lX,lY,lZ));
		return (tile, pos, set) -> 
		{
			final Box box = b.offset(pos);
			return !set.getMatchingTiles((p2,t2) -> box.contains(new Vec3d(p2.getX() + 0.5D, p2.getY() + 0.5D, p2.getZ() + 0.5D)) && predicate.test(t2)).isEmpty();
		};
	}
	
	/** Within Y range of N */
	public static Condition near(double distance, Predicate<Tile> predicate)
	{
		final double d = Math.min(10D, distance);
		return (t,p,s) -> !s.getMatchingTiles((p2,t2) -> p2.isWithinDistance(p, d) && predicate.test(t2)).isEmpty();
	}
	
	/** Not within bounded range of N */
	public static Condition avoid(Box bounds, Predicate<Tile> tiles)
	{
		return not(near(bounds, tiles));
	}
	
	/** Not within Y range of N */
	public static Condition avoid(double distance, Predicate<Tile> tiles)
	{
		return not(near(distance, tiles));
	}
	
	/** Adjacent to self */
	public static Condition consecutive(List<Direction> faces)
	{
		return (t,p,s) -> adjacent(faces, t::is).test(t, p, s);
	}
	
	/** Not adjacent to self */
	public static Condition nonConsecutive()
	{
		return nonConsecutive(Direction.stream().toList());
	}
	
	/** Not adjacent to self */
	public static Condition nonConsecutive(List<Direction> faces)
	{
		return (t,p,s) -> not(adjacent(faces, t::is)).test(t, p, s);
	}
	
	/** Not adjacent to N */
	public static Condition nonAdjacent(Predicate<Tile> tiles)
	{
		return nonAdjacent(Direction.stream().toList(), tiles);
	}
	
	/** Not adjacent to N */
	public static Condition nonAdjacent(List<Direction> faces, Predicate<Tile> tiles)
	{
		return not(adjacent(faces, tiles));
	}
	
	public static Condition maxAdjacent(List<Direction> faces, Predicate<Tile> tiles, int count)
	{
		return (tile,pos,set) -> 
		{
			return (int)faces.stream()
				.map(f -> pos.offset(f))
				.filter(set::contains)
				.map(p -> set.get(p).get())
				.filter(Predicates.not(Tile::isBlank))
				.filter(tiles)
				.count() < count;
		};
	}
	
	/** Not adjacent to self */
	public static Condition nonConsecutive(Box box)
	{
		return (t,p,s) -> avoid(box, t::is).test(t, p, s);
	}
	
	public static Condition maxOf(int i)
	{
		return (t,p,s) -> s.tallyOf(t) < i;
	}
	
	public static Condition maxOf(int i, Predicate<Tile> condition)
	{
		return (t,p,s) -> s.tallyMatching(condition) < i;
	}
}