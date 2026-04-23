package com.lying.grid;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.init.CDLoggers;
import com.lying.utility.DebugLogger;
import com.lying.utility.LineSegment2f;
import com.lying.worldgen.tile.Tile;

import net.minecraft.util.Pair;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Type;

/** Utility functions for generating paths between tiles on a 2D tile grid */
public class GridPathing
{
	private static final DebugLogger LOGGER = CDLoggers.PLANAR;
	public static final int TILE_SIZE = Tile.TILE_SIZE;
	protected static final Type MOVE_SET = Direction.Type.HORIZONTAL;
	
	/** Available grid path finding algorithms in ascending order of complexity */
	private static final List<GridPathFinder> PATHERS = List.of(
			GridPathing::findImmediateRoute,
			GridPathing::findDirectRoute,
			GridPathing::findLinearRoute
			);
	
	/** Returns the pair of tiles between sets that are best suited to be connected together */
	public static BoundTilePair findBestCandidatesToJoin(List<GridTile> setA, List<GridTile> setB, Predicate<GridTile> qualifier)
	{
		// Find pair of cached tile in passage and child's doorway tile that are closest together
		BoundTilePair closestPair = null;
		for(GridTile tileB : setB)
			for(GridTile tileA : setA)
			{
				BoundTilePair pair = new BoundTilePair(tileA, tileB, qualifier);
				if(closestPair == null)
				{
					closestPair = pair;
					continue;
				}
				
				switch((int)Math.signum(closestPair.distance() - pair.distance()))
				{
					// Distance higher
					// Exclude any potential candidates that are implicitly farther away than the best we've already found
					case -1:
						continue;
					// Distance equal
					// If distances are equal, pick the candidate with the shortest resulting path length
					case 0:
						if(closestPair.length() < pair.length())
							continue;
					// Distance lower
					case 1:
						closestPair = pair;
						break;
				}
			}
		return closestPair;
	}
	
	public static List<GridTile> findRouteBetween(GridTile start, GridTile end, Predicate<GridTile> validityCheck)
	{
		LOGGER.info("Finding route between {} and {}, distance {}", start.toString(), end.toString(), start.distance(end));
		List<GridTile> tiles = Lists.newArrayList();
		
		final Predicate<GridTile> terminusCheck = (t -> start.equals(t) || end.equals(t));
		final Predicate<GridTile> qualifier = terminusCheck.or(validityCheck);
		
		// Trial the cheapest route generators
		tiles.addAll(findCheapRoute(start, end, qualifier));
		if(!tiles.isEmpty())
		{
			LOGGER.info(" + Cheap route found: {} tiles", tiles.size());
			return tiles;
		}
		
		// Generate an A* path and try to streamline it
		List<GridTile> aStarPath = findAStarRoute(start, end, qualifier);
		if(aStarPath.isEmpty())
		{
			LOGGER.warn(" + Failed to find A* route");
			return List.of();
		}
		for(GridTile position : aStarPath)
		{
			tiles.add(position);
			
			// Try finding a more direct or immediate route if available from the new position
			List<GridTile> shortcut = findCheapRoute(position, end, qualifier);
			if(!shortcut.isEmpty())
			{
				tiles.addAll(shortcut);
				LOGGER.info(" + Complex route found: {} tiles", tiles.size());
				return tiles;
			}
			
			// Otherwise, continue moving along the A* path
		}
		
		LOGGER.info(" + A* route found: {} tiles", tiles.size());
		return tiles;
	}
	
	public static List<GridTile> findCheapRoute(GridTile start, GridTile end, Predicate<GridTile> qualifier)
	{
		List<GridTile> tiles = Lists.newArrayList();
		for(GridPathFinder method : PATHERS)
			if(!(tiles = method.findPath(start, end, qualifier)).isEmpty())
				return tiles;
		return List.of();
	}
	
	/** Returns a route between two points only if those points are adjacent or identical */
	public static List<GridTile> findImmediateRoute(GridTile start, GridTile end, Predicate<GridTile> qualifier)
	{
		if(start.equals(end))
			return List.of(start);
		else if(start.isAdjacent(end))
			return List.of(start, end);
		else
			return List.of();
	}
	
	/** Returns a straight grid-aligned path between points, as long as they are parallel */
	public static List<GridTile> findDirectRoute(GridTile start, GridTile end, Predicate<GridTile> qualifier)
	{
		if(!start.isParallel(end))
			return List.of();
		
		Vector2i direction = end.toVec2i().sub(start.toVec2i());
		Vector2i offset = new Vector2i(
				(int)Math.signum(end.x - start.x),
				(int)Math.signum(end.y - start.y)
				);
		List<GridTile> set = Lists.newArrayList(start);
		GridTile tile = start;
		for(int i=1; i<direction.length(); i++)
		{
			tile = tile.add(offset);
			if(!qualifier.test(tile))
				return List.of();
			
			set.add(tile);
		}
		return set;
	}
	
	public static List<GridTile> findLinearRoute(GridTile start, GridTile end, Predicate<GridTile> qualifier)
	{
		List<GridTile> tiles = TileUtils.lineToTiles(new LineSegment2f(start, end));
		return tiles.stream().allMatch(qualifier) ? tiles : List.of();
	}
	
	public static List<GridTile> findAStarRoute(GridTile start, GridTile end, Predicate<GridTile> qualifier)
	{
		// Nodes we've already visited
		List<AStarNode> closed = Lists.newArrayList();
		List<GridTile> closedTiles = Lists.newArrayList();
		
		// Nodes we've yet to investigate
		List<AStarNode> open = Lists.newArrayList(new AStarNode(start, null, 0, end));
		
		final int maxSearch = (1 + Math.abs(start.x - end.x)) * (1 + Math.abs(start.y - end.y)); 
		while(!open.isEmpty() && closed.size() < maxSearch)
		{
			AStarNode option = open.getFirst();
			for(AStarNode c : open)
				if(c.distance() < option.distance())
					option = c;
			
			final AStarNode candidate = option;
			open.remove(candidate);
			closed.add(candidate);
			closedTiles.add(candidate.pos);
			LOGGER.info("  - Test {} {}, distance {}, {} options remaining", closed.size(), candidate.pos.toString(), candidate.distance(), open.size());
			
			if(candidate.isEnd())
				return recomposeRoute(closed);
			else
				for(AStarNode move : getAStarCandidates(candidate, qualifier, closedTiles))
				{
					if(move.isEnd())
					{
						closed.add(move);
						return recomposeRoute(closed);
					}
					
					open.removeIf(move::isBetter);
					open.add(move);
				}
		}
		
		// If we get here it's because we didn't manage to find a route
		return List.of();
	}
	
	public static List<AStarNode> getAStarCandidates(AStarNode node, Predicate<GridTile> qualifier, List<GridTile> ignore)
	{
		final Function<GridTile, AStarNode> provider = t -> new AStarNode(t, node.pos(), node.step() + 1, node.target());
		List<AStarNode> candidates = Lists.newArrayList();
		for(Direction move : MOVE_SET)
		{
			GridTile option = node.pos().offset(move);
			if(node.isEnd(option))
			{
				candidates.add(provider.apply(option));
				return candidates;
			}
			else if(!ignore.contains(option) && qualifier.test(option))
				candidates.add(provider.apply(option));
		}
		return candidates;
	}
	
	/** Builds a linked list of nodes from {@link AStarNode.isEnd} to an unparented node (ie. the start) then reverses the order */
	private static List<GridTile> recomposeRoute(List<AStarNode> nodes)
	{
		AStarNode node = nodes.stream().filter(AStarNode::isEnd).findAny().get();
		List<AStarNode> route = Lists.newArrayList(node);
		while(node.parent() != null)
		{
			for(AStarNode node2 : nodes)
				if(node2.isParent(node))
				{
					node = node2;
					break;
				}
			
			route.add(node);
		}
		
		return route.stream().sorted(AStarNode.STEP_SORT).map(AStarNode::pos).toList();
	}
	
	private static class AStarNode
	{
		public static Comparator<AStarNode> STEP_SORT = (a,b) -> b.isParent(a) ? 1 : a.isParent(b) ? -1 : 0;
		
		private final GridTile pos, parent, target;
		private final int step;
		private final double distance;
		
		public AStarNode(GridTile posIn, GridTile parentIn, int stepIn, GridTile targetIn)
		{
			pos = posIn;
			parent = parentIn;
			target = targetIn;
			step = stepIn;
			distance = pos.distance(target);
		}
		
		public GridTile pos() { return pos; }
		public GridTile parent() { return parent; }
		public GridTile target() { return target; }
		
		public int step() { return step; }
		
		public double distance() { return distance; }
		
		public boolean isEnd() { return isEnd(pos); }
		
		public boolean isEnd(GridTile tile) { return tile.equals(target); }
		
		public String toString() { return pos.toString(); }
		
		/** Returns true if we are the parent of the given node */
		public boolean isParent(AStarNode node) { return node.parent != null && node.parent.equals(pos) && node.step > step; }
		
		/** Returns true if we're the same position but faster */
		public boolean isBetter(AStarNode node)
		{
			return pos.equals(node.pos) && node.step >= step;
		}
	}
	
	public static class BoundTilePair extends Pair<GridTile,GridTile>
	{
		private final double distance;
		private final Predicate<GridTile> qualifier;
		
		// Cached route between tiles, only calculated when necessary for performance reasons
		private Optional<List<GridTile>> route = Optional.empty();
		
		public BoundTilePair(GridTile startIn, GridTile endIn, Predicate<GridTile> checkIn)
		{
			super(startIn, endIn);
			distance = getLeft().distance(getRight());
			qualifier = checkIn;
		}
		
		public double distance() { return distance; }
		
		public int length() { return route().isEmpty() ? Integer.MAX_VALUE : route().size(); }
		
		public List<GridTile> route()
		{
			if(route.isEmpty())
				route = Optional.of(findRouteBetween(getLeft(), getRight(), qualifier));
			return route.get();
		}
	}
	
	@FunctionalInterface
	private interface GridPathFinder
	{
		/**
		 * @param start - The initial position
		 * @param end - The target position
		 * @param qualifier - A predicate validating if a position is permitted
		 * @return
		 */
		public List<GridTile> findPath(GridTile start, GridTile end, Predicate<GridTile> qualifier);
	}
}
