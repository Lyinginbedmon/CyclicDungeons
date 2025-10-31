package com.lying.blueprint;

import java.util.List;
import java.util.function.Supplier;

import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.init.CDLoggers;
import com.lying.utility.DebugLogger;
import com.lying.utility.LineSegment2f;
import com.lying.worldgen.Tile;

import net.minecraft.util.math.Vec2f;

/** Utility class for reducing the footprint of a blueprint */
public class BlueprintScruncher
{
	public static DebugLogger LOGGER = CDLoggers.PLANAR;
	
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
			// If there's only one segment, move against its direction to shrink it
			case 1:
				LineSegment2f line = lines.getFirst();
				if(line.isStraightLine() && line.manhattanLength() <= Tile.TILE_SIZE)
					return false;
				
				direction = line.direction().negate();
				break;
			// If there are two segments, move against the longest
			case 2:
				direction = (lines.getFirst().length() > lines.getLast().length() ? lines.getFirst() : lines.getLast()).direction().negate();
				break;
			default:
				// If there's more than 2 segments, use the nearest non-terminal segment as the guide
				direction = lines.get(lines.size() - 2).direction().negate();
				break;
		}
		
		return direction.length() > Tile.TILE_SIZE && tryMoveNodeByWorld(node, chart, direction);
	}
	
	public static Vector2i vec2fToVec2i(Vec2f vec)
	{
		return new Vector2i((int)vec.x, (int)vec.y);
	}
	
	public static boolean tryMoveNodeByWorld(BlueprintRoom node, Blueprint chart, Vec2f direction)
	{
		Vec2f gridDirection = direction.normalize().multiply((float)Math.floor(direction.length() / Tile.TILE_SIZE));
		for(float i=gridDirection.length(); i>0; i--)
		{
			Vec2f point = gridDirection.normalize().multiply(i);
			
			int x = Math.abs(point.x) >= 1 ? (int)point.x : (int)Math.signum(point.x);
			int y = Math.abs(point.y) >= 1 ? (int)point.y : (int)Math.signum(point.y);
			
			if(tryMoveRelative(node, chart, new Vector2i(x, y)))
				return true;
		}
		
		return false;
	}
	
	public static boolean tryMoveRelative(BlueprintRoom node, Blueprint chart, Vector2i move)
	{
		if(move.length() == 0)
			return false;
		
		// Try move the full offset
		if(tryMove(node, chart, move))
			return true;
		
		// If both axises are non-zero, try to move on each individually
		else if((Math.abs(move.x) + Math.abs(move.y)) != move.length())
		{
			Vector2i onlyX = new Vector2i(move.x, 0);
			Vector2i onlyY = new Vector2i(0, move.y);
			double distX = Math.abs(move.x);
			double distY = Math.abs(move.y);
			
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
	
	/**
	 * Simulates the move, then applies it if it does not produce an error
	 * @param node
	 * @param chart
	 * @param move
	 * @return
	 */
	public static boolean tryMove(BlueprintRoom node, Blueprint chart, Vector2i move)
	{
		if(move.length() == 0)
			return false;
		
		// Clone blueprint and simulate movement
		// Apply movement to all descendants as well to minimise processing time
		Blueprint sim = chart.clone();
		List<BlueprintRoom> simNodes = Lists.newArrayList();
		simNodes.add(sim.getRoom(node.uuid()).get());
		simNodes.addAll(gatherDescendantsOf(simNodes.getFirst(), sim));
		
		simNodes.forEach(n -> n.move(move));
		if(sim.hasErrors())
			return false;
		
		// If the simulation caused no errors, apply it to the live blueprint
		node.move(move);
		gatherDescendantsOf(node, chart).forEach(n -> n.move(move));
		
		return true;
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
}
