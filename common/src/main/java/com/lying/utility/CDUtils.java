package com.lying.utility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;

public class CDUtils
{
	public static <T extends Object> T selectFromWeightedList(List<Pair<T,Float>> weightList, final float selector)
	{
		// Step 1 - Calculate the sum of all weights
		float totalWeight = 0F;
		for(Pair<T,Float> entry : weightList)
			totalWeight += entry.getRight();
		
		// Step 2 - Use that sum to calculate the percentile of each choice within the set
		Map<T, Float> percentileMap = new HashMap<>();
		for(Pair<T,Float> entry : weightList)
			percentileMap.put(entry.getLeft(), entry.getRight() / totalWeight);
		
		// Step 3 - Select the last entry in the list whose position in the set does not exceed the selector value
		float cumulative = 0F;
		List<Entry<T,Float>> entryList = Lists.newArrayList(percentileMap.entrySet());
		T result = entryList.get(0).getKey();
		for(Entry<T, Float> entry : entryList)
		{
			cumulative += entry.getValue();
			if(cumulative > selector)
				break;
			
			result = entry.getKey();
		}
		return result;
	}
}
