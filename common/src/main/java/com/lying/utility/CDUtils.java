package com.lying.utility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.DyeColor;

public class CDUtils
{
	private static final Map<DyeColor, Pair<Block,Block>> DYE_TO_CONCRETE = new HashMap<>();
	
	static
	{
		DYE_TO_CONCRETE.put(DyeColor.BLACK, Pair.of(Blocks.BLACK_CONCRETE_POWDER, Blocks.BLACK_CONCRETE));
		DYE_TO_CONCRETE.put(DyeColor.BLUE, Pair.of(Blocks.BLUE_CONCRETE_POWDER, Blocks.BLUE_CONCRETE));
		DYE_TO_CONCRETE.put(DyeColor.BROWN, Pair.of(Blocks.BROWN_CONCRETE_POWDER, Blocks.BROWN_CONCRETE));
		DYE_TO_CONCRETE.put(DyeColor.CYAN, Pair.of(Blocks.CYAN_CONCRETE_POWDER, Blocks.CYAN_CONCRETE));
		DYE_TO_CONCRETE.put(DyeColor.GRAY, Pair.of(Blocks.GRAY_CONCRETE_POWDER, Blocks.GRAY_CONCRETE));
		DYE_TO_CONCRETE.put(DyeColor.GREEN, Pair.of(Blocks.GREEN_CONCRETE_POWDER, Blocks.GREEN_CONCRETE));
		DYE_TO_CONCRETE.put(DyeColor.LIGHT_BLUE, Pair.of(Blocks.LIGHT_BLUE_CONCRETE_POWDER, Blocks.LIGHT_BLUE_CONCRETE));
		DYE_TO_CONCRETE.put(DyeColor.LIGHT_GRAY, Pair.of(Blocks.LIGHT_GRAY_CONCRETE_POWDER, Blocks.LIGHT_GRAY_CONCRETE));
		DYE_TO_CONCRETE.put(DyeColor.LIME, Pair.of(Blocks.LIME_CONCRETE_POWDER, Blocks.LIME_CONCRETE));
		DYE_TO_CONCRETE.put(DyeColor.MAGENTA, Pair.of(Blocks.MAGENTA_CONCRETE_POWDER, Blocks.MAGENTA_CONCRETE));
		DYE_TO_CONCRETE.put(DyeColor.ORANGE, Pair.of(Blocks.ORANGE_CONCRETE_POWDER, Blocks.ORANGE_CONCRETE));
		DYE_TO_CONCRETE.put(DyeColor.PINK, Pair.of(Blocks.PINK_CONCRETE_POWDER, Blocks.PINK_CONCRETE));
		DYE_TO_CONCRETE.put(DyeColor.PURPLE, Pair.of(Blocks.PURPLE_CONCRETE_POWDER, Blocks.PURPLE_CONCRETE));
		DYE_TO_CONCRETE.put(DyeColor.RED, Pair.of(Blocks.RED_CONCRETE_POWDER, Blocks.RED_CONCRETE));
		DYE_TO_CONCRETE.put(DyeColor.WHITE, Pair.of(Blocks.WHITE_CONCRETE_POWDER, Blocks.WHITE_CONCRETE));
		DYE_TO_CONCRETE.put(DyeColor.YELLOW, Pair.of(Blocks.YELLOW_CONCRETE_POWDER, Blocks.YELLOW_CONCRETE));
	}
	
	public static Block dyeToConcretePowder(DyeColor color) { return DYE_TO_CONCRETE.get(color).getLeft(); }
	public static Block dyeToConcrete(DyeColor color) { return DYE_TO_CONCRETE.get(color).getRight(); }
	
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
