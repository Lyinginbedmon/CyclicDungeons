package com.lying.blueprint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import org.joml.Vector2i;

import com.google.common.collect.Lists;
import com.lying.utility.Line2;
import com.lying.utility.Vector2iUtils;

/** Utility class for reducing the footprint of a blueprint */
public class BlueprintScruncher
{
	/** Applies scrunch algorithm until failure */
	public static void collapse(Blueprint chart)
	{
		while(scrunch(chart)) { }
	}
	
	/** Reduces the distance between nodes */
	public static boolean scrunch(Blueprint chart)
	{
		int maxDepth = chart.maxDepth();
		
		boolean anyMoved = false;
		for(int i=maxDepth; i>0; i--)
			anyMoved = tryScrunch(chart.byDepth(i), chart) || anyMoved;
		
		return anyMoved;
	}
	
	private static boolean tryScrunch(List<Node> nodes, Blueprint chart)
	{
		boolean anyMoved = false;
		for(Node node : nodes)
		{
			// Calculated "ideal" position, ie. right on top of or between the parents
			Vector2i ideal = node.getParentPosition(chart);
			
			// Amount & direction to move
			double len = Vector2iUtils.subtract(node.position(), ideal).length();
			if(len < 1)
				continue;
			
			// Collect node and all descendants as a "cluster"
			List<Node> toMove = gatherDescendantsOf(node, chart);
			toMove.add(node);
			
			len = 1;	// TODO Scrunching restricted to 1 step for debugging
			List<Node> otherNodes = chart.stream().filter(n -> !toMove.contains(n)).toList();
			while(len-- > 0 && tryMoveTowards(node, toMove, otherNodes, chart, ideal))
				anyMoved = true;
		};
		return anyMoved;
	}
	
	public static List<Node> gatherDescendantsOf(Node node, Blueprint chart)
	{
		List<Node> children = Lists.newArrayList();
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
	
	private static boolean tryMoveTowards(Node node, List<Node> cluster, List<Node> otherNodes, Blueprint chart, Vector2i point)
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
	
	private static boolean tryMove(List<Node> cluster, List<Node> otherNodes, Blueprint chart, Vector2i move)
	{
		if(move.length() == 0 || cluster.isEmpty())
			return false;

		// Check if the adjusted bounding box of any node in the cluster would cause an intersection
		if(cluster.stream().anyMatch(n -> Blueprint.boxHasIntersection(n.bounds().offset(move), otherNodes)))
			return false;
		
		// Calculate modified positions of all nodes in the cluster (for their updated paths)
		Map<UUID,Vector2i> clusterPositions = new HashMap<>();
		cluster.forEach(n -> clusterPositions.put(n.uuid(), Vector2iUtils.add(n.position(), move)));
		final Function<Node,Vector2i> getter = p -> clusterPositions.containsKey(p.uuid()) ? clusterPositions.get(p.uuid()) : p.position();
		
		// FIXME Check if any path between this node and its parents or children would cause an intersection with the rest of the blueprint
		for(Node n : cluster)
			if(
				n.getParents(chart).stream().map(getter::apply).anyMatch(p -> Blueprint.pathHasIntersection(new Line2(getter.apply(n), p), otherNodes)) || 
				n.getChildren(chart).stream().map(getter::apply).anyMatch(c -> Blueprint.pathHasIntersection(new Line2(getter.apply(n), c), otherNodes))
				)
				return false;
		
		for(Node n : cluster)
			n.offset(move);
		
		return true;
	}
}
