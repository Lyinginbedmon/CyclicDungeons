package com.lying.grid;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.joml.Vector2i;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.lying.init.CDLoggers;
import com.lying.utility.DebugLogger;
import com.lying.utility.LineSegment2f;
import com.lying.worldgen.tile.Tile;

import net.minecraft.util.Pair;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Type;
import net.minecraft.util.math.Vec2f;

/** Utility functions for generating paths between tiles on a 2D tile grid */
public class GridPathing
{
	private static final DebugLogger LOGGER = CDLoggers.PLANAR;
	public static final int TILE_SIZE = Tile.TILE_SIZE;
	protected static final Type MOVE_SET = Direction.Type.HORIZONTAL;
	
	/** Available grid path finding algorithms in ascending order of complexity */
	private static final List<GridPathFinder> PATHERS = List.of
			(
				// Direct pathing for tiles overlapping or adjacent
				(start, end, qualifier) -> 
				{
					if(start.equals(end))
						return PathingResult.success(List.of(start));
					else if(start.isAdjacent(end))
						return PathingResult.success(List.of(start, end));
					else
						return PathingResult.failure();
				}
				,
				// Straight-line linear pathing for parallel tiles
				(start, end, qualifier) ->
				{
					if(!start.isParallel(end))
						return PathingResult.failure();
					
					Vector2i direction = end.toVec2i().sub(start.toVec2i());
					Vector2i offset = new Vector2i(
							(int)Math.signum(direction.x),
							(int)Math.signum(direction.y)
							);
					List<GridTile> set = Lists.newArrayList(start);
					GridTile tile = start;
					for(int i=0; i<direction.length(); i++)
						if(!qualifier.test(tile = tile.add(offset)))
							return PathingResult.failure();
						else
							set.add(tile);
					return PathingResult.success(set);
				}
				,
				// Straight-line pathing for non-parallel tiles
				(start, end, qualifier) -> 
				{
					List<GridTile> tiles = lineToTiles(start, end);
					return tiles.stream().allMatch(qualifier) ? 
							PathingResult.success(tiles) : 
							PathingResult.failure();
				}
			);
	
	/** Returns the pair of tiles between sets that are best suited to be connected together */
	public static BoundTilePair findBestCandidatesToJoin(List<GridTile> setA, List<GridTile> setB, Predicate<GridTile> qualifier)
	{
		if(setA.isEmpty())
			throw new NullPointerException("Set A of tiles provided is blank");
		if(setB.isEmpty())
			throw new NullPointerException("Set B of tiles provided were blank");
		
		// Find pair of cached tile in passage and child's doorway tile that are closest together
		BoundTilePair closestPair = new BoundTilePair(setA.getFirst(), setB.getFirst(), qualifier);
		for(GridTile tileA : setA)
			for(GridTile tileB : setB)
			{
				BoundTilePair pair = new BoundTilePair(tileA, tileB, qualifier);
				// Exclude any potential candidates that are implicitly farther away than the best we've already found
				if(pair.distance() > closestPair.distance())
					continue;
				// If distances are lower, pick the new candidate
				// If distances are equal, pick the candidate with the shortest resulting path length
				if(
						pair.distance() < closestPair.distance() || 
						pair.length() < closestPair.length())
					closestPair = pair;
			}
		return closestPair;
	}
	
	public static PathingResult findRouteBetween(GridTile start, GridTile end, Predicate<GridTile> validityCheck)
	{
		LOGGER.info("Finding route between {} and {}, distance {}", start.toString(), end.toString(), start.distance(end));
		
		final Predicate<GridTile> terminusCheck = (t -> start.equals(t) || end.equals(t));
		final Predicate<GridTile> qualifier = terminusCheck.or(validityCheck);
		
		// Trial the cheapest route generators
		PathingResult cheap = findCheapRoute(start, end, qualifier);
		if(cheap.isSuccess())
		{
			LOGGER.info(" + Cheap route found: {} tiles", cheap.size());
			return cheap;
		}
		
		// Generate an A* path and try to streamline it
		PathingResult aStarPath = findAStarRoute(start, end, qualifier);
		if(aStarPath.isFailure())
			return PathingResult.failure();
		
		// Try to streamline the A* path by shortcutting where possible
		// TODO Replace with weighting to make the A* path less prone to zig-zagging
		List<GridTile> tiles = Lists.newArrayList();
		for(GridTile position : aStarPath.result())
		{
			tiles.add(position);
			
			// Try finding a more direct or immediate route if available from the new position
			PathingResult shortcut = findCheapRoute(position, end, qualifier);
			if(shortcut.isSuccess())
			{
				tiles.addAll(shortcut.result());
				LOGGER.info(" + Complex route found: {} tiles", tiles.size());
				return PathingResult.success(tiles);
			}
			
			// Otherwise, continue moving along the A* path
		}
		
		LOGGER.info(" + A* route found: {} tiles", tiles.size());
		return aStarPath;
	}
	
	public static PathingResult findCheapRoute(GridTile start, GridTile end, Predicate<GridTile> qualifier)
	{
		PathingResult tiles;
		for(GridPathFinder method : PATHERS)
			if((tiles = method.findPath(start, end, qualifier)).isSuccess())
				return tiles;
		return PathingResult.failure();
	}
	
	/** Generates a route between the given tiles by following a 2D line between them */
	public static List<GridTile> lineToTiles(GridTile startTile, GridTile endTile)
	{
		final LineSegment2f line = new LineSegment2f(startTile, endTile);
		
		// Generate points along the length of the interceding line
		List<GridTile> initialSet = Lists.newArrayList(startTile);
		final Vec2f dir = line.direction().normalize();
		for(int i=0; i<Math.floor(line.length()); i++)
		{
			GridTile prev = initialSet.getLast();
			GridTile next = startTile.add(GridTile.fromVec(dir.multiply(i)));
			
			// Add points to produce a cohesive route as necessary
			List<GridTile> append = Lists.newArrayList();
			append.addAll(adjoinTiles(prev, next));
			append.add(next);
			
			append.forEach(t -> 
			{
				if(!initialSet.contains(t))
					initialSet.add(t);
			});
		}
		
		if(!initialSet.contains(endTile))
		{
			initialSet.addAll(adjoinTiles(initialSet.getLast(), endTile));
			initialSet.add(endTile);
		}
		
		return initialSet;
	}
	
	/** Returns a list of tiles adjoining the start and end points, excluding those points */
	public static List<GridTile> adjoinTiles(GridTile start, GridTile end)
	{
		if(start.isAdjacentOrSame(end))
			return List.of();
		
		PathingResult result = GridPathing.findAStarRoute(start, end, Predicates.alwaysTrue());
		return result.isSuccess() ? result.result().stream().filter(t -> !start.equals(t) && !end.equals(t)).toList() : List.of();
	}
	
	/** Returns an A* path between points, avoiding unqualified geometry */
	public static PathingResult findAStarRoute(GridTile start, GridTile end, Predicate<GridTile> qualifier)
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
				return PathingResult.success(recomposeRoute(closed));
			else
				for(AStarNode move : getAStarCandidates(candidate, qualifier, closedTiles))
				{
					if(move.isEnd())
					{
						closed.add(move);
						return PathingResult.success(recomposeRoute(closed));
					}
					
					open.removeIf(move::isBetter);
					open.add(move);
				}
		}
		
		// If we get here it's because we didn't manage to find a route
		return PathingResult.failure();
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
		if(nodes.stream().noneMatch(AStarNode::isEnd))
			return List.of();
		
		AStarNode node = nodes.stream().filter(AStarNode::isEnd).findAny().get();
		List<AStarNode> route = Lists.newArrayList(node);
		while(!node.isStart() && !nodes.isEmpty())
		{
			nodes.remove(node);
			for(AStarNode node2 : nodes)
				if(node2.isParent(node))
				{
					node = node2;
					break;
				}
			
			route.add(node);
		}
		return route.reversed().stream().map(AStarNode::pos).toList();
	}
	
	private static class AStarNode
	{
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
		
		public GridTile target() { return target; }
		
		public int step() { return step; }
		
		public double distance() { return distance; }
		
		public boolean isStart() { return parent == null; }
		
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
		private Optional<PathingResult> route = Optional.empty();
		
		public BoundTilePair(GridTile startIn, GridTile endIn, Predicate<GridTile> checkIn)
		{
			super(startIn, endIn);
			distance = getLeft().manhattanDistance(getRight());
			qualifier = checkIn;
		}
		
		public double distance() { return distance; }
		
		/** Returns the number of tiles this path occupies */
		public int length() { return route().isFailure() ? Integer.MAX_VALUE : route().result().size(); }
		
		public PathingResult route()
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
		public PathingResult findPath(GridTile start, GridTile end, Predicate<GridTile> qualifier);
	}
	
	/** A result handler class, so we can readily distinguish between an empty path and a failed path */
	public static class PathingResult
	{
		protected final List<GridTile> contents;
		protected final boolean isSuccess;
		
		protected PathingResult(boolean success, List<GridTile> tiles)
		{
			contents = tiles;
			isSuccess = success;
		}
		
		public static PathingResult failure()
		{
			return new PathingResult(false, List.of());
		}
		
		public static PathingResult success(List<GridTile> tiles)
		{
			return new PathingResult(true, tiles);
		}
		
		public boolean isSuccess() { return isSuccess; }
		
		public boolean isFailure() { return !isSuccess; }
		
		public List<GridTile> result() { return contents; }
		
		public int size() { return contents.size(); }
	}
}
