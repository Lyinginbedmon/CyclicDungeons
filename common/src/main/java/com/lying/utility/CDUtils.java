package com.lying.utility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Lists;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.Vec2f;

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
	
	@Nullable
	public static <T extends Object> T selectFromWeightedList(List<Pair<T,Float>> weightedList, final float selector)
	{
		if(weightedList.isEmpty())
			return null;
		else if(weightedList.size() == 1)
			return weightedList.get(0).getLeft();
		
		// Step 1 - Calculate the sum of all weights
		float totalWeight = 0F;
		for(float weight : weightedList.stream().map(Pair::getRight).map(Math::abs).filter(w -> w>0).toList())
			totalWeight += weight;
		
		// Step 2 - Use that sum to calculate the percentile of each choice within the set
		List<Pair<T,Float>> percentileList = Lists.newArrayList();
		for(Pair<T,Float> entry : weightedList)
			percentileList.add(Pair.of(entry.getLeft(), entry.getRight() / totalWeight));
		percentileList.sort((a,b) -> a.getRight() < b.getRight() ? -1 : a.getRight() > b.getRight() ? 1 : 0);
		
		// Step 3 - Select the first entry in the list whose weight val exceeds the selector value
		float lowerBound = 0F;
		for(int i=0; i<percentileList.size(); i++)
		{
			Pair<T,Float> entry = percentileList.get(i);
			float upperBound = lowerBound + entry.getRight();
			if(selector >= lowerBound && selector <= upperBound)
				return entry.getLeft();
			lowerBound = upperBound;
		}
		return percentileList.getLast().getLeft();
	}
	
	public static Vec2f rotate(Vec2f a, float radians)
	{
		if(radians == Math.toRadians(90D))
			return new Vec2f(-a.y, a.x);
		
		final double cos = Math.cos(radians), sin = Math.sin(radians);
		return new Vec2f(
				(float)(a.x * cos - a.y * sin),
				(float)(a.x * sin + a.y * cos)
				);
	}
}
