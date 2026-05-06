package com.lying.grid;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.lying.init.CDLoggers;
import com.lying.utility.DebugLogger;
import com.lying.utility.LineSegment2f;
import com.lying.worldgen.tile.Tile;

import net.minecraft.util.Pair;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;

/** Utility functions for generating paths between tiles on a 2D tile grid */
public class GridPathing
{
	private static final DebugLogger LOGGER = CDLoggers.PLANAR;
	public static final int TILE_SIZE = Tile.TILE_SIZE;
	protected static final List<AStarMove> MOVE_SET = Direction.Type.HORIZONTAL.stream().map(d -> new AStarMove(new Vector2i(d.getOffsetX(), d.getOffsetZ()))).toList();
	
	/** Available grid path finding algorithms in ascending order of complexity */
	private static final List<GridPathFinder> PATHERS = List.of
			(
				// Direct pathing for tiles overlapping or adjacent
				(start, end, walkable) -> 
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
				(start, end, walkable) ->
				{
					// If the tiles aren't parallel, we don't have a straight line to walk
					if(!start.isParallel(end))
						return PathingResult.failure();
					
					Vector2i direction = end.toVec2i().sub(start.toVec2i());
					Vector2i offset = new Vector2i(
							(int)Math.signum(direction.x),
							(int)Math.signum(direction.y)
							);
					// If the offset length does not equal 1, we know it's not grid-aligned
					if(offset.length() != 1)
						return PathingResult.failure();
					
					List<GridTile> set = Lists.newArrayList(start);
					GridTile tile = start;
					for(int i=0; i<direction.length(); i++)
						if(!walkable.test(tile = tile.add(offset)))
							return PathingResult.failure();
						else
							set.add(tile);
					return PathingResult.success(set);
				}
			);
	
	/** The predicate determining navigable grid tile positions */
	private Predicate<GridTile> walkable;
	
	public GridPathing(Predicate<GridTile> walkableIn)
	{
		walkable = walkableIn;
	}
	
	public GridPathing()
	{
		this(Predicates.alwaysTrue());
	}
	
	protected GridPathing setWalkable(Predicate<GridTile> walkableIn)
	{
		this.walkable = walkableIn;
		return this;
	}
	
	/** Returns the pair of tiles between sets that are best suited to be connected together */
	public static BoundTilePair findBestCandidatesToJoin(List<GridTile> setA, List<GridTile> setB, Predicate<GridTile> walkable)
	{
		if(setA.isEmpty())
			throw new NullPointerException("Set A of tiles provided is blank");
		if(setB.isEmpty())
			throw new NullPointerException("Set B of tiles provided were blank");
		
		// Find pair of cached tile in passage and child's doorway tile that are closest together
		BoundTilePair closestPair = new BoundTilePair(setA.getFirst(), setB.getFirst(), walkable);
		for(GridTile tileA : setA)
			for(GridTile tileB : setB)
			{
				BoundTilePair pair = new BoundTilePair(tileA, tileB, walkable);
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
	
	public PathingResult findRouteBetween(GridTile start, GridTile end)
	{
		LOGGER.info("Finding route between {} and {}, distance {}", start.toString(), end.toString(), start.distance(end));
		
		final Predicate<GridTile> walkableLog = walkable;
		final Predicate<GridTile> terminusCheck = (t -> start.equals(t) || end.equals(t));
		setWalkable(terminusCheck.or(walkable));
		
		// Trial the cheapest route generators
		PathingResult cheap = findCheapRoute(start, end);
		if(cheap.isSuccess())
		{
			LOGGER.info(" + Cheap route found: {} tiles", cheap.size());
			setWalkable(walkableLog);
			return cheap;
		}
		
		// Generate a streamlined A* path
		PathingResult aStarPath = findAStarRoute(start, end);
		if(aStarPath.isFailure())
		{
			setWalkable(walkableLog);
			return PathingResult.failure();
		}
		
		setWalkable(walkableLog);
		return aStarPath;
	}
	
	public PathingResult findCheapRoute(GridTile start, GridTile end)
	{
		PathingResult tiles;
		for(GridPathFinder method : PATHERS)
			if((tiles = method.findPath(start, end, walkable)).isSuccess())
				return tiles;
		return PathingResult.failure();
	}
	
	/** Generates a route between the given tiles by following a 2D line between them */
	public List<GridTile> lineToTiles(GridTile startTile, GridTile endTile)
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
	public List<GridTile> adjoinTiles(GridTile start, GridTile end)
	{
		if(start.isAdjacentOrSame(end))
			return List.of();
		
		PathingResult result = findAStarRoute(start, end);
		return result.isSuccess() ? result.result().stream().filter(t -> !start.equals(t) && !end.equals(t)).toList() : List.of();
	}
	
	/** Returns an A* path between points, avoiding unqualified geometry */
	public PathingResult findAStarRoute(GridTile start, GridTile end)
	{
		// Nodes we've already visited
		List<AStarNode> closed = Lists.newArrayList();
		List<GridTile> closedTiles = Lists.newArrayList();
		
		// Nodes we've yet to investigate
		List<AStarNode> open = Lists.newArrayList(new AStarNode(start, end));
		
		final int maxSearch = (1 + Math.abs(start.x - end.x)) * (1 + Math.abs(start.y - end.y));
		while(!open.isEmpty() && closed.size() < maxSearch)
		{
			AStarNode option = open.getFirst();
			for(AStarNode c : open)
				if(c.value() < option.value())
					option = c;
			
			final AStarNode candidate = option;
			open.remove(candidate);
			closed.add(candidate);
			closedTiles.add(candidate.pos);
			LOGGER.info("  - Test {} {}, distance {}, {} options remaining", closed.size(), candidate.pos.toString(), candidate.distance(), open.size());
			
			if(candidate.isEnd())
				return PathingResult.success(candidate.route());
			else
				for(AStarNode move : getAStarCandidates(candidate, closedTiles))
				{
					if(move.isEnd())
						return PathingResult.success(move.route());
					
					open.removeIf(move::isBetter);
					open.add(move);
				}
		}
		
		// If we get here it's because we didn't manage to find a route
		return PathingResult.failure();
	}
	
	public List<AStarNode> getAStarCandidates(AStarNode node, List<GridTile> ignore)
	{
		List<AStarNode> candidates = Lists.newArrayList();
		for(AStarMove move : MOVE_SET)
		{
			// Always prohibit moving back against the current direction of travel
			if(move.isOpposite(node.moveTaken.orElse(null)))
				continue;
			
			AStarNode option = node.apply(move);
			if(option.isEnd())
				return List.of(option);
			else if(!ignore.contains(option.pos()) && walkable.test(option.pos()))
				candidates.add(option);
		}
		return candidates;
	}
	
	private static class AStarNode
	{
		private final List<AStarNode> history = Lists.newArrayList();
		private final Optional<AStarMove> moveTaken;
		private final GridTile pos, destination;
		private final float totalCost;
		private final double distance;
		
		public AStarNode(GridTile posIn, GridTile targetIn)
		{
			this(posIn, Optional.empty(), 0F, targetIn);
		}
		
		public AStarNode(GridTile posIn, Optional<AStarMove> lastMove, float cost, GridTile targetIn)
		{
			pos = posIn;
			destination = targetIn;
			moveTaken = lastMove;
			totalCost = cost;
			distance = pos.distance(destination);
		}
		
		public GridTile pos() { return pos; }
		
		public double distance() { return distance; }
		
		public double value() { return distance * totalCost; }
		
		public boolean isEnd() { return pos.equals(destination); }
		
		public String toString() { return pos.toString(); }
		
		/** Returns true if we're the same position but faster */
		public boolean isBetter(AStarNode node)
		{
			return pos.equals(node.pos) && node.totalCost > totalCost;
		}
		
		protected AStarNode setHistory(List<AStarNode> historyIn)
		{
			history.clear();
			history.addAll(historyIn);
			return this;
		}
		
		public List<GridTile> route()
		{
			List<AStarNode> history = Lists.newArrayList(this.history);
			history.add(this);
			return history.stream().map(AStarNode::pos).toList();
		}
		
		public AStarNode apply(AStarMove move)
		{
			List<AStarNode> history = Lists.newArrayList(this.history);
			history.add(this);
			return new AStarNode(
					pos.add(move.offset), 
					Optional.of(move),
					totalCost + move.cost(this.moveTaken.orElse(null)), 
					destination)
					.setHistory(history);
		}
	}
	
	public static class AStarMove
	{
		public static final float COURSE_CHANGE_WEIGHT = 20F;
		private final Vector2i offset;
		
		public AStarMove(Vector2i offsetIn)
		{
			offset = offsetIn;
		}
		
		public boolean equals(Object obj) { return obj instanceof AStarMove && ((AStarMove)obj).offset.gridDistance(offset) == 0; }
		
		public boolean isOpposite(@Nullable AStarMove move) { return move != null && move.offset.equals(new Vector2i(-offset.x, -offset.y)); }
		
		public float cost(@Nullable AStarMove lastMove)
		{
			if(lastMove == null)
				return 1F;
			// Straight lines always permitted
			else if(lastMove.equals(this))
				return 1F;
			// Reversing forbidden
			else if(isOpposite(lastMove))
				return Float.MAX_VALUE;
			// Course changes inhibited
			else
				return COURSE_CHANGE_WEIGHT;
		}
	}
	
	public static class BoundTilePair extends Pair<GridTile,GridTile>
	{
		private final double distance;
		private final GridPathing pather;
		
		// Cached route between tiles, only calculated when necessary for performance reasons
		private Optional<PathingResult> route = Optional.empty();
		
		public BoundTilePair(GridTile startIn, GridTile endIn, Predicate<GridTile> walkableIn)
		{
			super(startIn, endIn);
			distance = getLeft().manhattanDistance(getRight());
			pather = new GridPathing(walkableIn);
		}
		
		public double distance() { return distance; }
		
		/** Returns the number of tiles this path occupies */
		public int length() { return route().isFailure() ? Integer.MAX_VALUE : route().result().size(); }
		
		public PathingResult route()
		{
			if(route.isEmpty())
				route = Optional.of(pather.findRouteBetween(getLeft(), getRight()));
			return route.get();
		}
	}
	
	@FunctionalInterface
	private interface GridPathFinder
	{
		/**
		 * @param start - The initial position
		 * @param end - The target position
		 * @param walkable - A predicate validating if a position is permitted
		 * @return
		 */
		public PathingResult findPath(GridTile start, GridTile end, Predicate<GridTile> walkable);
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
