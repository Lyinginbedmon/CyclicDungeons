package com.lying.blueprint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.lying.grammar.CDGraph;
import com.lying.grammar.CDRoom;
import com.lying.utility.Box2;
import com.lying.utility.Line2;

@SuppressWarnings("serial")
public class Blueprint extends ArrayList<Node>
{
	protected int maxDepth = 0;
	protected Map<Integer, List<Node>> byDepth = new HashMap<>();
	
	public static Blueprint fromGraph(CDGraph graphIn)
	{
		Blueprint graph = new Blueprint();
		graphIn.getStart().ifPresent(r -> addNodeToBlueprint(r, null, graph, graphIn));
		return graph;
	}
	
	private static Node addNodeToBlueprint(CDRoom room, @Nullable Node parent, Blueprint graph, CDGraph graphIn)
	{
		Node node = new Node(room.uuid(), room.metadata(), room.getChildLinks(), room.getParentLinks());
		graph.add(node);
		if(room.hasLinks())
			room.getChildRooms(graphIn).forEach(r -> addNodeToBlueprint(r, parent, graph, graphIn));
		return node;
	}
	
	public boolean add(Node node)
	{
		boolean result = super.add(node);
		if(result)
		{
			// Update the depth range
			maxDepth = 0;
			for(Node n : this)
				if(n.metadata().depth() > maxDepth)
					maxDepth = n.metadata().depth();
			
			// Update the depth map
			byDepth.clear();
			for(int i=0; i<=maxDepth; i++)
			{
				final int depth = i;
				byDepth.put(i, stream().filter(n -> n.metadata().depth() == depth).toList());
			}
		}
		return result;
	}
	
	public int maxDepth() { return maxDepth; }
	
	@NotNull
	public List<Node> byDepth(int depth) { return byDepth.getOrDefault(depth, Lists.newArrayList()); }
	
	/** Returns true if this blueprint contains any errors that may interfere with generation */
	public boolean hasErrors()
	{
		// Check if any bounding boxes intersect
		List<Box2> bounds = stream().map(Node::bounds).toList();
		for(Box2 boundA : bounds)
			if(bounds.stream().filter(b -> !b.equals(boundA)).anyMatch(b -> boundA.intersects(b)))
				return true;
		
		List<Line2> paths = getAllPaths(this);
		for(Line2 path : paths)
		{
			// Check if any paths intersect unrelated nodes
			if(stream().filter(n -> !(n.position().equals(path.getLeft()) || n.position().equals(path.getRight()))).map(Node::bounds).anyMatch(b -> b.intersects(path)))
				return true;

			// Check if any paths intersect other paths
			if(paths.stream().filter(p -> !p.equals(path)).anyMatch(p -> p.intersects(path)))
				return true;
		}
		
		return false;
	}
	
	/** Returns a list of all paths between the given nodes */
	public static List<Line2> getAllPaths(Collection<Node> chart)
	{
		List<Line2> existingPaths = Lists.newArrayList();
		for(Node node : chart)
			node.getChildren(chart).forEach(c -> existingPaths.add(new Line2(node.position(), c.position())));
		return existingPaths;
	}
	
	/** Returns a list of all paths in the given blueprint, excluding any that connect to the given node */
	public static List<Line2> getPathsExcluding(Collection<Node> chart, @Nullable List<Node> excludeList)
	{
		Predicate<Node> exclusion = 
				(excludeList.isEmpty() || excludeList == null) ? 
					Predicates.alwaysFalse() : 
					n -> excludeList.stream().map(Node::uuid).anyMatch(id -> id.equals(n.uuid()));
		List<Line2> paths = Lists.newArrayList();
		for(Node node : chart)
			node.getChildren(chart).forEach(child -> 
			{
				if(exclusion.test(node) || exclusion.test(child))
					return;
				
				paths.add(new Line2(node.position(), child.position()));
			});
		return paths;
	}
	
	public static List<Box2> getAllBounds(Collection<Node> chart)
	{
		List<Box2> existingPaths = Lists.newArrayList();
		for(Node node : chart)
			existingPaths.add(node.bounds());
		return existingPaths;
	}
	
	public static List<Box2> getBoundsExcluding(Collection<Node> chart, @Nullable List<Node> excludeList)
	{
		Predicate<Node> exclusion = 
				(excludeList.isEmpty() || excludeList == null) ? 
					Predicates.alwaysFalse() : 
					n -> excludeList.stream().map(Node::uuid).anyMatch(id -> id.equals(n.uuid()));
		List<Box2> bounds = Lists.newArrayList();
		for(Node node : chart)
			node.getChildren(chart).forEach(child -> 
			{
				if(!exclusion.test(child))
					bounds.add(child.bounds());
			});
		return bounds;
	}
	
	
	public static boolean intersectsAnyBoundingBox(Box2 box, Collection<Node> chart)
	{
		return chart.stream().anyMatch(b -> b.intersects(box));
	}
	
	public static boolean intersectsAnyPath(Box2 box, Collection<Node> chart)
	{
		return getAllPaths(chart).stream()
				.anyMatch(p -> box.intersects(p));
	}
	
	public static boolean boxHasIntersection(Box2 box, Collection<Node> chart)
	{
		return intersectsAnyBoundingBox(box, chart) || intersectsAnyPath(box, chart);
	}
	
	public static boolean pathHasIntersection(Line2 line, Collection<Node> chart)
	{
		// Intersection with node bounding boxes
		if(chart.stream()
				.filter(n -> !(n.position().equals(line.getLeft()) || n.position().equals(line.getRight())))
				.anyMatch(n -> n.bounds().intersects(line)))
			return false;
		
		// Intersection with inter-node paths
		return getAllPaths(chart).stream()
				.filter(p -> !line.equals(p))
				.anyMatch(p -> p.intersects(line));
	}
}
