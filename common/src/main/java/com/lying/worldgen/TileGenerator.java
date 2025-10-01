package com.lying.worldgen;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;
import com.lying.init.CDTiles;
import com.lying.utility.CDUtils;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public class TileGenerator
{
	public static void generate(TileSet map, Map<Tile,Float> tiles, Random rand)
	{
		List<BlockPos> positionsToGenerate = map.getBlanks();
		Map<Integer,List<BlockPos>> positionsPerY = new HashMap<>();
		positionsToGenerate.forEach(p -> 
		{
			int y = p.getY();
			List<BlockPos> set = positionsPerY.getOrDefault(y, Lists.newArrayList());
			set.add(p);
			positionsPerY.put(y, set);
		});
		
		List<Integer> levels = Lists.newArrayList();
		levels.addAll(positionsPerY.keySet());
		Collections.sort(levels);
		
		for(int level : levels)
			processSet(positionsPerY.get(level), map, tiles, rand);
	}
	
	private static void processSet(List<BlockPos> slots, TileSet map, Map<Tile,Float> tiles, Random rand)
	{
		final List<Tile> candidates = tiles.keySet().stream().toList();
		while(!slots.isEmpty())
		{
			/**
			 * Sort open slots by number of available options
			 * Pop slot with fewest
			 * Assign random viable option (or empty air) to slot
			 * Repeat until slots are depleted
			 */
			slots.sort((a,b) -> 
			{
				int eA = map.getOptionsFor(a, candidates).size();
				int eB = map.getOptionsFor(b, candidates).size();
				return eA < eB ? -1 : eA > eB ? 1 : 0;
			});
			
			BlockPos slot = slots.removeFirst();
			List<Tile> options = map.getOptionsFor(slot, candidates);
			
			Tile tile;
			switch(options.size())
			{
				case 0:
					tile = CDTiles.AIR.get();
					break;
				case 1:
					tile = options.get(0);
					break;
				default:
					tile = selectTile(options, tiles, rand);
					break;
			}
			map.put(slot, tile);
		}
	}
	
	public static Tile selectTile(List<Tile> options, Map<Tile,Float> weights, Random rand)
	{
		List<Pair<Tile,Float>> weightedList = Lists.newArrayList();
		options.forEach(tile -> weightedList.add(Pair.of(tile, (float)weights.get(tile))));
		
		return CDUtils.selectFromWeightedList(weightedList, rand.nextFloat());
	}
}
