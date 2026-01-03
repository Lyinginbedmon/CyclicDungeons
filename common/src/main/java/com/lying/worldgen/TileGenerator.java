package com.lying.worldgen;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;
import com.lying.grid.BlueprintTileGrid;
import com.lying.init.CDLoggers;
import com.lying.init.CDTiles;
import com.lying.utility.CDUtils;
import com.lying.utility.DebugLogger;
import com.lying.worldgen.tile.Tile;
import com.lying.worldgen.tileset.TileSet;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public class TileGenerator
{
	public static final DebugLogger LOGGER = CDLoggers.WFC;
	public static boolean DEBUG = false;
	
	public static void generate(BlueprintTileGrid map, TileSet tiles, Random rand)
	{
		if(map.isEmpty())
		{
			LOGGER.error("Attempted to apply WFC to blank tile set");
			return;
		}
		
		LOGGER.info("Apply WFC to tile set of {} positions", map.volume());
		
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
		
		int index = 0;
		for(int level : levels)
		{
			List<BlockPos> positions = positionsPerY.get(level);
			int size = positions.size();
			processSet(positions, map, tiles, rand);
			
			LOGGER.info(" ## Generated set {} of {}, {} positions", ++index, levels.size(), size);
		}
		
		LOGGER.info("WFC complete");
	}
	
	private static void processSet(List<BlockPos> slots, BlueprintTileGrid map, TileSet tiles, Random rand)
	{
		if(slots.isEmpty() || tiles.isEmpty())
		{
			LOGGER.forceWarn("Position set empty or no tiles were provided");
			return;
		}
		
		final List<Tile> candidates = tiles.keys().stream().filter(t -> !t.isBlank()).toList();
		/**
		 * Sort open slots by number of available options
		 * Pop slot with fewest options
		 * Assign random viable option (or empty air, if none) to slot
		 * Repeat until open slots are depleted
		 */
		while(!slots.isEmpty())
		{
			map.clearOptionCache();
			MapEntry entry = getMostConstrained(slots, map, candidates);
			Tile tile;
			switch(entry.options().size())
			{
				case 0:
					tile = CDTiles.AIR.get();
					break;
				case 1:
					tile = entry.options().get(0);
					break;
				default:
					tile = selectTile(entry.options(), tiles, rand);
					break;
			}
			
			map.put(entry.pos(), tile);
			slots.remove(entry.index());
		}
	}
	
	protected static MapEntry getMostConstrained(List<BlockPos> slots, BlueprintTileGrid map, List<Tile> candidates)
	{
		map.clearOptionCache();
		MapEntry most = MapEntry.of(slots.get(0), map, candidates);
		for(int i=0; i<slots.size(); i++)
		{
			BlockPos slotB = slots.get(i);
			MapEntry entry = new MapEntry(i, slotB, map.getOptionsFor(slotB, candidates));
			if(entry.size() < most.size())
				most = entry;
		}
		return most;
	}
	
	public static Tile selectTile(List<Tile> options, TileSet weights, Random rand)
	{
		List<Pair<Tile,Float>> weightedList = Lists.newArrayList();
		options.forEach(tile -> weightedList.add(Pair.of(tile, (float)weights.get(tile))));
		
		return CDUtils.selectFromWeightedList(weightedList, rand.nextFloat());
	}
	
	protected record MapEntry(int index, BlockPos pos, List<Tile> options)
	{
		public int size() { return options.size(); }
		
		public static MapEntry of(BlockPos pos, BlueprintTileGrid map, List<Tile> options)
		{
			return new MapEntry(0, pos, map.getOptionsFor(pos, options));
		}
	}
}
