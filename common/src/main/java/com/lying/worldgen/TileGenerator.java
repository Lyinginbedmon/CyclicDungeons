package com.lying.worldgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Predicates;
import com.lying.grid.BlueprintTileGrid;
import com.lying.grid.GridVolume;
import com.lying.init.CDLoggers;
import com.lying.init.CDTiles;
import com.lying.utility.CDUtils;
import com.lying.utility.logging.DebugLogger;
import com.lying.worldgen.tile.Tile;
import com.lying.worldgen.tileset.TileSet;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public class TileGenerator
{
	public static final DebugLogger LOGGER = CDLoggers.WFC;
	public static boolean DEBUG = false;
	
	public static void generate(BlueprintTileGrid map, GridVolume vol, TileSet tiles, Random rand)
	{
		generate(map, vol, tiles, CDTiles.AIR, rand);
	}
	
	public static void generate(BlueprintTileGrid map, GridVolume vol, TileSet tiles, Supplier<Tile> fallback, Random rand)
	{
		if(map.isEmpty() || vol.isEmpty())
		{
			LOGGER.error("Attempted to apply WFC to blank tile set");
			return;
		}
		
		LOGGER.info("Apply WFC to tile set of {} positions", map.volume());
		
		List<BlockPos> positionsToGenerate = vol.unsetPoints();
		Map<Integer,List<BlockPos>> positionsPerY = new HashMap<>();
		positionsToGenerate.forEach(p -> 
		{
			int y = p.getY();
			List<BlockPos> set = positionsPerY.getOrDefault(y, new ArrayList<>());
			set.add(p);
			positionsPerY.put(y, set);
		});
		
		List<Integer> levels = new ArrayList<>();
		levels.addAll(positionsPerY.keySet());
		Collections.sort(levels);
		
		int index = 0;
		for(int level : levels)
		{
			List<BlockPos> positions = positionsPerY.get(level);
			int size = positions.size();
			processSet(positions, map, vol, tiles, fallback, rand);
			
			LOGGER.info(" ## Generated set {} of {}, {} positions", ++index, levels.size(), size);
		}
		
		LOGGER.info("WFC complete");
	}
	
	private static void processSet(List<BlockPos> slots, BlueprintTileGrid map, GridVolume vol, TileSet tiles, Supplier<Tile> fallback, Random rand)
	{
		if(slots.isEmpty() || tiles.isEmpty())
		{
			LOGGER.forceWarn("Position set empty or no tiles were provided");
			return;
		}
		
		final List<Tile> candidates = tiles.keys().stream()
				.filter(Predicates.not(Tile::isBlank))
				.toList();
		/**
		 * Sort open slots by number of available options
		 * Pop slot with fewest options
		 * Assign random viable option (or empty air, if none) to slot
		 * Repeat until open slots are depleted
		 */
		while(!slots.isEmpty())
		{
			MapEntry entry = getMostConstrained(slots, map, candidates);
			Tile tile;
			switch(entry.options().size())
			{
				case 0:
					tile = fallback.get();
					break;
				case 1:
					tile = entry.options().get(0);
					break;
				default:
					tile = selectTile(entry.options(), tiles, rand);
					break;
			}
			
			if(tile != null)
				setTile(entry.pos(), tile, map, vol);
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
		List<Pair<Tile,Float>> weightedList = new ArrayList<>();
		options.forEach(tile -> weightedList.add(Pair.of(tile, (float)weights.get(tile))));
		
		return CDUtils.selectFromWeightedList(weightedList, rand.nextFloat());
	}
	
	/** Sets the tile in the grid and marks it as checked within the volume */
	public static void setTile(BlockPos pos, Tile tile, BlueprintTileGrid map, GridVolume vol)
	{
		if(!map.contains(pos))
		{
			map.addToVolume(pos);
			vol.add(pos);
		}
		map.put(pos, tile);
		if(!tile.isBlank())
			vol.set(pos);
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
