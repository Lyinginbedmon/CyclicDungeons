package com.lying.blueprint;

import java.util.List;
import java.util.function.Supplier;

import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.utility.Vector2iUtils;

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
		while(depth != 0)
		{
			depth -= Math.signum(depth);
			anyMoved = tryScrunch(chart.byDepth(Math.abs(depth)), chart) || anyMoved;
		}
		
		return anyMoved;
	}
	
	private static boolean tryScrunch(List<BlueprintRoom> nodes, Blueprint chart)
	{
		boolean anyMoved = false;
		for(BlueprintRoom node : nodes)
		{
			// Calculated "ideal" position, ie. right on top of or between the parents
			Vector2i ideal = node.getParentPosition(chart);
			
			// Amount & direction to move
			double len = Vector2iUtils.subtract(node.position(), ideal).length();
			if(len < 1)
				continue;
			
			// Collect node and all descendants as a "cluster"
			List<BlueprintRoom> toMove = gatherDescendantsOf(node, chart);
			toMove.add(node);
			
			List<BlueprintRoom> otherNodes = chart.stream().filter(n -> !toMove.contains(n)).toList();
			if(otherNodes.isEmpty())
				continue;
			
			while(len-- > 0 && tryMoveTowards(node, toMove, otherNodes, chart, ideal))
				anyMoved = true;
		};
		return anyMoved;
	}
	
	public static List<BlueprintRoom> gatherDescendantsOf(BlueprintRoom node, Blueprint chart)
	{
		List<BlueprintRoom> children = Lists.newArrayList();
		node.getChildren(chart).forEach(child -> 
		{
			if(children.contains(child))
				return;
			
			children.add(child);
			if(child.hasChildren())
				children.addAll(gatherDescendantsOf(child, chart));
		});
		// FIXME Ensure uniqueness in list of children
		return children;
	}
	
	private static boolean tryMoveTowards(BlueprintRoom node, List<BlueprintRoom> cluster, List<BlueprintRoom> otherNodes, Blueprint chart, Vector2i point)
	{
		// Current position, from which we calculate offset
		Vector2i position = node.position();
		
		Vector2i offset = Vector2iUtils.subtract(position, point);
		double len = offset.length();
		if(len < 1)
			return false;
		
		offset = new Vector2i(
				offset.x != 0 ? (int)Math.signum(offset.x) : 0, 
				offset.y != 0 ? (int)Math.signum(offset.y) : 0);
		
		// Try move the full offset, then if that fails try to move on one axis
		if(tryMove(cluster, otherNodes, chart, offset))
			return true;
		else
		{
			// If both axises are non-zero, try to move on each individually
			if(offset.x != 0 && offset.y != 0)
			{
				Vector2i onlyX = new Vector2i(offset.x, 0);
				Vector2i onlyY = new Vector2i(0, offset.y);
				double distX = Vector2iUtils.add(position, onlyX).distance(point);
				double distY = Vector2iUtils.add(position, onlyY).distance(point);
				
				Supplier<Boolean> tryX = () -> tryMove(cluster, otherNodes, chart, onlyX);
				Supplier<Boolean> tryY = () -> tryMove(cluster, otherNodes, chart, onlyY);
				
				// Prioritise moving in whichever direction results in the shortest distance to the point
				if(distX < distY)
					return tryX.get() ? true : tryY.get();
				else
					return tryY.get() ? true : tryX.get();
			}
			else
				return false;
		}
	}
	
	private static boolean tryMove(List<BlueprintRoom> cluster, List<BlueprintRoom> otherNodes, Blueprint chart, Vector2i move)
	{
		if(move.length() == 0 || cluster.isEmpty())
			return false;
		
		// Move the cluster, check for errors, revert if any are found
		cluster.forEach(n -> n.offset(move));
		
		if(chart.hasErrors())
		{
			cluster.forEach(n -> n.offset(Vector2iUtils.negate(move)));
			return false;
		}
		
		return true;
	}
}
