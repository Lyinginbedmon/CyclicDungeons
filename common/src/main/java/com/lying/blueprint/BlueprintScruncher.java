package com.lying.blueprint;

import java.util.List;
import java.util.function.Supplier;

import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.utility.AbstractBox2f;
import com.lying.utility.LineSegment2f;
import com.lying.utility.Vector2iUtils;
import com.lying.worldgen.Tile;

import net.minecraft.util.math.Vec2f;

/** Utility class for reducing the footprint of a blueprint */
public class BlueprintScruncher
{
	/** Applies scrunch algorithm until failure */
	public static void collapse(Blueprint chart, boolean reverse)
	{
		int cap = 1000;
		while(scrunch(chart, reverse) && --cap > 0) { }
	}
	
	/** Reduces the distance between nodes */
	public static boolean scrunch(Blueprint chart, boolean reverse)
	{
		int maxDepth = chart.maxDepth();
		
		boolean anyMoved = false;
		int depth = reverse ? maxDepth : -maxDepth;
		int inc = (int)Math.signum(depth) * -1;
		while(depth != 0)
		{
			anyMoved = tryScrunch(chart.byDepth(Math.abs(depth)), chart) || anyMoved;
			depth += inc;
		}
		
		return anyMoved;
	}
	
	private static boolean tryScrunch(List<BlueprintRoom> nodes, Blueprint chart)
	{
		boolean anyMoved = false;
		for(BlueprintRoom node : nodes)
			anyMoved = tryScrunchNode(node, chart) || anyMoved;
		return anyMoved;
	}
	
	public static boolean tryScrunchNode(BlueprintRoom node, Blueprint chart)
	{
		BlueprintRoom parent = node.getParents(chart).getFirst();
		if(parent == null)
			return false;
		
		BlueprintPassage path = new BlueprintPassage(parent, node);
		Vec2f direction;
		List<LineSegment2f> lines = path.asLines();
		switch(lines.size())
		{
			case 1:
				// If there's only one segment, move against its direction to shrink it
				LineSegment2f line = lines.getFirst();
				if((line.isHorizontal || line.isVertical) && line.manhattanLength() <= Tile.TILE_SIZE)
					return false;
				
				direction = line.direction().negate();
				break;
			case 2:
				// If we have only two line segments, try to merge their end points
				LineSegment2f first = lines.getFirst();
				LineSegment2f last = lines.getLast();
				
				AbstractBox2f parentBounds = parent.bounds();
				boolean firstIsStart = parentBounds.intersects(first);
				
				Vec2f start = (firstIsStart ? first : last).getRight();
				Vec2f end = (firstIsStart ? last : first).getLeft();
				
				direction = start.add(end.negate());
				break;
			default:
			case 3:
				// If there's more than 2 segments, use the nearest non-terminal segment as the guide
				direction = lines.get(lines.size() - 2).direction().negate();
				break;
		}
		
		return direction.length() > 0 ? tryMoveRelative(node, chart, floatVecToNormalizedInt(direction)) : false;
	}
	
	private static Vector2i floatVecToNormalizedInt(Vec2f vec)
	{
		if(vec.length() == 0)
			return new Vector2i(0, 0);
		
		return new Vector2i((int)Math.signum(vec.x), (int)Math.signum(vec.y)).mul(Tile.TILE_SIZE);
	}
	
	/** Collects all nodes down-stream of the given node */
	public static List<BlueprintRoom> gatherDescendantsOf(BlueprintRoom node, Blueprint chart)
	{
		List<BlueprintRoom> children = Lists.newArrayList();
		node.getChildren(chart).forEach(child -> 
		{
			if(children.contains(child))
				return;
			
			children.add(child);
			if(child.hasChildren())
			{
				List<BlueprintRoom> childRooms = gatherDescendantsOf(child, chart);
				childRooms.removeIf(children::contains);
				children.addAll(childRooms);
			}
		});
		return children;
	}
	
	public static boolean tryMoveRelative(BlueprintRoom node, Blueprint chart, Vector2i offset)
	{
		if(offset.length() == 0)
			return false;
		
		// Try move the full offset
		if(tryMove(node, chart, offset))
			return true;
		
		// If both axises are non-zero, try to move on each individually
		else if(offset.x != 0 && offset.y != 0)
		{
			Vector2i onlyX = new Vector2i(offset.x, 0);
			Vector2i onlyY = new Vector2i(0, offset.y);
			double distX = Math.abs(offset.x);
			double distY = Math.abs(offset.y);
			
			Supplier<Boolean> tryX = () -> tryMove(node, chart, onlyX);
			Supplier<Boolean> tryY = () -> tryMove(node, chart, onlyY);
			
			// Prioritise moving in whichever direction results in the shortest distance to the point
			if(distX < distY)
				return tryX.get() ? true : tryY.get();
			else
				return tryY.get() ? true : tryX.get();
		}
		
		return false;
	}
	
	public static boolean tryMove(BlueprintRoom node, Blueprint chart, Vector2i move)
	{
		if(move.length() == 0)
			return false;
		
		// Move the cluster, check for errors, revert if any are found
		// Taxing but effective without any greater checking
		node.nudge(move);
		
		if(chart.hasErrors())
		{
			node.nudge(Vector2iUtils.negate(move));
			return false;
		}
		
		return true;
	}
}
