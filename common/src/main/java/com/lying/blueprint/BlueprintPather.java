package com.lying.blueprint;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Lists;

public class BlueprintPather
{
	public static List<BlueprintRoom> calculateCriticalPath(Blueprint chart)
	{
		Optional<BlueprintRoom> start = chart.start();
		Optional<BlueprintRoom> end = chart.end();
		return start.isPresent() && end.isPresent() ? calculatePathBetween(start.get(), end.get(), chart) : Lists.newArrayList();
	}
	
	public static List<BlueprintRoom> calculatePathBetween(BlueprintRoom start, BlueprintRoom end, Blueprint chart)
	{
		// Map of nodes to the node they are accessed from
		Map<BlueprintRoom, BlueprintRoom> pathMap = new HashMap<>();
		
		// Nodes available to check
		List<BlueprintRoom> checkList = Lists.newArrayList();
		checkList.add(start);
		
		// Nodes already checked
		List<BlueprintRoom> checkedList = Lists.newArrayList();
		final Comparator<BlueprintRoom> distSort = (a,b) -> 
		{
			double distA = a.position().distance(end.position());
			double distB = b.position().distance(end.position());
			return distA < distB ? -1 : distA > distB ? 1 : 0;
		};
		
		while(!checkList.isEmpty())
		{
			checkList.removeAll(checkedList);
			checkList.sort(distSort);
			
			BlueprintRoom node = checkList.get(0);
			checkedList.add(node);
			
			if(node.equals(end))
			{
				// Work backwards through the pathMap to identify the route
				List<BlueprintRoom> finalPath = Lists.newArrayList();
				BlueprintRoom point = node;
				finalPath.add(point);
				while(pathMap.containsKey(point))
				{
					point = pathMap.get(point);
					finalPath.add(point);
				}
				
				return finalPath;
			}
			
			for(BlueprintRoom child : node.getChildren(chart))
			{
				checkList.add(child);
				pathMap.put(child, node);
			}
		}
		
		return Lists.newArrayList();
	}
}
