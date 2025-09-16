package com.lying.blueprint;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Lists;

public class BlueprintPather
{
	public static List<Node> calculateGoldenPath(Blueprint chart)
	{
		Optional<Node> start = chart.start();
		Optional<Node> end = chart.end();
		return start.isPresent() && end.isPresent() ? calculatePathBetween(start.get(), end.get(), chart) : Lists.newArrayList();
	}
	
	public static List<Node> calculatePathBetween(Node start, Node end, Blueprint chart)
	{
		// Map of nodes to the node they are accessed from
		Map<Node, Node> pathMap = new HashMap<>();
		
		// Nodes available to check
		List<Node> checkList = Lists.newArrayList();
		checkList.add(start);
		
		// Nodes already checked
		List<Node> checkedList = Lists.newArrayList();
		final Comparator<Node> distSort = (a,b) -> 
		{
			double distA = a.position().distance(end.position());
			double distB = b.position().distance(end.position());
			return distA < distB ? -1 : distA > distB ? 1 : 0;
		};
		
		while(!checkList.isEmpty())
		{
			checkList.removeAll(checkedList);
			checkList.sort(distSort);
			
			Node node = checkList.get(0);
			checkedList.add(node);
			
			if(node.equals(end))
			{
				// Work backwards through the pathMap to identify the route
				List<Node> finalPath = Lists.newArrayList();
				Node point = node;
				finalPath.add(point);
				while(pathMap.containsKey(point))
				{
					point = pathMap.get(point);
					finalPath.add(point);
				}
				
				return finalPath;
			}
			
			for(Node child : node.getChildren(chart))
			{
				checkList.add(child);
				pathMap.put(child, node);
			}
		}
		
		return Lists.newArrayList();
	}
}
