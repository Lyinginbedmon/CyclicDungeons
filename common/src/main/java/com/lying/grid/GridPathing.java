package com.lying.grid;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.init.CDLoggers;
import com.lying.utility.DebugLogger;
import com.lying.utility.LineSegment2f;
import com.lying.worldgen.tile.Tile;

import net.minecraft.util.math.Direction;

/** Utility functions for generating paths on a 2D tile grid */
public class GridPathing
{
	@SuppressWarnings("unused")
	private static final DebugLogger LOGGER = CDLoggers.PLANAR;
	public static final int TILE_SIZE = Tile.TILE_SIZE;
	protected static final Vector2i[] MOVE_SET = Direction.Type.HORIZONTAL.stream().map(d -> new Vector2i(d.getOffsetX(), d.getOffsetZ())).toList().toArray(new Vector2i[0]);
	
	/** Available grid path finding algorithms in ascending order of complexity */
	private static final List<GridPathFinder> PATHERS = List.of(
			GridPathing::findImmediateRoute,
			GridPathing::findDirectRoute,
			GridPathing::findLinearRoute
			);
	
	public static List<GridTile> findRouteBetween(GridTile start, GridTile end, Predicate<GridTile> validityCheck)
	{
		List<GridTile> tiles = Lists.newArrayList();
		
		// Trial the cheapest route generators
		tiles.addAll(findCheapRoute(start, end, validityCheck));
		if(!tiles.isEmpty())
			return tiles;
		
		// Generate an A* path and try to streamline it
		List<GridTile> aStarPath = findAStarRoute(start, end, validityCheck);
		for(GridTile position : aStarPath)
		{
			tiles.add(position);
			
			// Try finding a more direct or immediate route if available from the new position
			List<GridTile> shortcut = findCheapRoute(position, end, validityCheck);
			if(!shortcut.isEmpty())
			{
				tiles.addAll(shortcut);
				return tiles;
			}
			
			// Otherwise, continue moving along the A* path
		}
		
		return tiles;
	}
	
	public static List<GridTile> findCheapRoute(GridTile start, GridTile end, Predicate<GridTile> validityCheck)
	{
		List<GridTile> tiles = Lists.newArrayList();
		for(GridPathFinder method : PATHERS)
			if(!(tiles = method.findPath(start, end, validityCheck)).isEmpty())
				return tiles;
		return List.of();
	}
	
	/** Returns a route between two points only if those points are adjacent or identical */
	public static List<GridTile> findImmediateRoute(GridTile start, GridTile end, Predicate<GridTile> validityCheck)
	{
		if(start.equals(end))
			return List.of(start);
		else if(start.isAdjacent(end))
			return List.of(start, end);
		else
			return List.of();
	}
	
	/** Returns a straight grid-aligned path between points, as long as they are parallel */
	public static List<GridTile> findDirectRoute(GridTile start, GridTile end, Predicate<GridTile> validityCheck)
	{
		if(!start.isParallel(end) || start.equals(end))
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
			if(!validityCheck.test(tile))
				return List.of();
			
			set.add(tile);
		}
		return set;
	}
	
	public static List<GridTile> findLinearRoute(GridTile start, GridTile end, Predicate<GridTile> validityCheck)
	{
		List<GridTile> tiles = TileUtils.lineToTiles(new LineSegment2f(start, end));
		return tiles.stream().allMatch(validityCheck) ? tiles : List.of();
	}
	
	public static List<GridTile> findAStarRoute(GridTile start, GridTile end, Predicate<GridTile> validityCheck)
	{
		// Nodes we've already visited
		List<AStarNode> closed = Lists.newArrayList();
		// Nodes we've yet to investigate
		List<AStarNode> open = Lists.newArrayList(new AStarNode(start, null, 0, end));
		
		while(!open.isEmpty())
		{
			int index = 0;
			double minDist = Double.MAX_VALUE;
			for(int i=0; i<open.size(); i++)
			{
				double d = open.get(i).distance();
				if(d < minDist)
				{
					minDist = d;
					index = i;
				}
			}
			
			final AStarNode candidate = open.remove(index);
			closed.add(candidate);
			
			if(candidate.isEnd())
				return recomposeRoute(closed);
			
			for(AStarNode move : getAStarCandidates(candidate, validityCheck, closed.stream().map(AStarNode::pos).toList()))
			{
				open.removeIf(move::isBetter);
				open.add(move);
			}
		}
		
		return List.of();
	}
	
	/** Builds a linked list of nodes from {@link AStarNode.isEnd} to an unparented node (ie. the start) then reverses the order */
	private static List<GridTile> recomposeRoute(List<AStarNode> nodes)
	{
		List<AStarNode> route = Lists.newArrayList();
		AStarNode node = nodes.getLast();
		while(node.parent() != null)
		{
			route.add(node);
			
			for(AStarNode node2 : nodes)
				if(node2.isParent(node))
				{
					node = node2;
					break;
				}
		}
		
		return route.stream().sorted(AStarNode.STEP_SORT).map(AStarNode::pos).toList();
	}
	
	public static List<AStarNode> getAStarCandidates(AStarNode node, Predicate<GridTile> validityCheck, List<GridTile> exclude)
	{
		List<AStarNode> candidates = Lists.newArrayList();
		for(Vector2i move : MOVE_SET)
		{
			GridTile option = node.pos().add(move);
			if(option.equals(node.target()))
			{
				candidates.add(new AStarNode(option, node.pos(), node.step() + 1, node.target()));
				break;
			}
			else if(!exclude.contains(option) && validityCheck.test(option))
				candidates.add(new AStarNode(option, node.pos(), node.step() + 1, node.target()));
		}
		return candidates;
	}
	
	public static List<GridTile> connectTiles(GridTile start, GridTile end, Direction step)
	{
		List<GridTile> tiles = Lists.newArrayList();
		Vector2i offset = end.toVec2i().sub(start.toVec2i());
		// Straight line march from start to finish
		if(offset.x == 0 || offset.y == 0)
		{
			Direction dir = Direction.fromVector(offset.x, 0, offset.y, step.getOpposite());
			GridTile pos = start;
			do
			{
				pos = pos.offset(dir);
				tiles.add(pos);
			}
			while(!pos.isAdjacentOrSame(end));
		}
		// Line traversal
		else
		{
			GridTile childBind = end.offset(step);
			tiles.addAll(TileUtils.lineToTiles(new LineSegment2f(start, childBind)));
			tiles.add(childBind);
		}
		tiles.add(end);
		return tiles;
	}
	
	public record AStarNode(GridTile pos, GridTile parent, int step, GridTile target)
	{
		public static Comparator<AStarNode> STEP_SORT = (a,b) -> b.isParent(a) ? 1 : a.isParent(b) ? -1 : 0;
		
		public double distance() { return pos.distance(target); }
		
		public boolean isEnd() { return pos.equals(target); }
		
		public String toString() { return pos.toString(); }
		
		/** Returns true if we are the parent of the given node */
		public boolean isParent(AStarNode node) { return node.parent.equals(pos) && node.step > step; }
		
		/** Returns true if we're the same position but faster */
		public boolean isBetter(AStarNode node)
		{
			return pos.equals(node.pos) && node.step >= step;
		}
	}
	
	@FunctionalInterface
	private interface GridPathFinder
	{
		public List<GridTile> findPath(GridTile start, GridTile end, Predicate<GridTile> validityCheck);
	}
}
