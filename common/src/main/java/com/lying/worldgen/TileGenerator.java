package com.lying.worldgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import com.google.common.base.Predicates;
import com.lying.grid.BlueprintTileGrid;
import com.lying.grid.GridVolume;
import com.lying.init.CDLoggers;
import com.lying.init.CDTiles;
import com.lying.utility.CDUtils;
import com.lying.utility.logging.DebugLogger;
import com.lying.worldgen.tile.Tile;
import com.lying.worldgen.tile.Tile.GridSensitivity;
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
		
		// Ensure grid volume knows its own contents
		vol.clearTallies();
		vol.contents().forEach(p -> 
		{
			Tile tileAt = map.get(p).orElse(CDTiles.BLANK.get());
			vol.tally(tileAt, null);
		});
		
		final OptionLog optionManager = new OptionLog(map, vol, tiles.keys().stream()
				.filter(Predicates.not(Tile::isBlank))
				.toList());
		
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
			processSet(positions, map, vol, tiles, optionManager, fallback, rand);
			
			LOGGER.info(" ## Generated set {} of {}, {} positions", ++index, levels.size(), size);
		}
		
		LOGGER.info("WFC complete");
	}
	
	private static void processSet(List<BlockPos> slots, BlueprintTileGrid map, GridVolume vol, final TileSet tiles, final OptionLog manager, final Supplier<Tile> fallback, Random rand)
	{
		if(slots.isEmpty() || tiles.isEmpty())
		{
			LOGGER.forceWarn("Position set empty or no tiles were provided");
			return;
		}
		
		/**
		 * Sort open slots by number of available options
		 * Pop slot with fewest options
		 * Assign random viable option (or empty air, if none) to slot
		 * Repeat until open slots are depleted
		 */
		while(!slots.isEmpty())
		{
			final BlockPos pos = manager.getMostConstrainedAmong(slots);
			if(pos == null)
				break;
			
			List<Tile> options = manager.getOptionsFor(pos);
			Tile tile = null;
			switch(options.size())
			{
				case 0:
					tile = fallback.get();
					break;
				case 1:
					tile = options.get(0);
					break;
				default:
					tile = selectTile(options, tiles, rand);
					break;
			}
			
			if(tile != null)
			{
				setTile(pos, tile, map, vol);
				manager.removeAndUpdate(pos, map, vol);
			}
			slots.remove(pos);
		}
	}
	
	public static Tile selectTile(List<Tile> options, TileSet weights, Random rand)
	{
		return CDUtils.selectFromWeightedList(
				options.stream().map(t -> Pair.of(t, weights.get(t))).toList(),
				rand);
	}
	
	/** Sets the tile in the grid and marks it as checked within the volume */
	public static void setTile(BlockPos pos, Tile tile, BlueprintTileGrid map, GridVolume vol)
	{
		if(!map.contains(pos))
		{
			map.addToVolume(pos);
			vol.add(pos);
		}
		Optional<Tile> existing = map.get(pos);
		map.put(pos, tile);
		vol.tally(tile, existing.orElse(null));
		if(!tile.isBlank())
			vol.set(pos);
	}
	
	/** Centralised tile options manager object */
	public static class OptionLog
	{
		private Map<BlockPos,OptionData> data = new HashMap<>();
		private List<BlockPos> sequence = new ArrayList<>();
		private final Comparator<BlockPos> placeOrder = (a,b) -> 
		{
			int cA = data.get(a).constraint();
			int cB = data.get(b).constraint();
			return cA < cB ? -1 : cA > cB ? 1 : 0;
		};
		
		public OptionLog(BlueprintTileGrid map, GridVolume vol, List<Tile> tileSet)
		{
			data.clear();
			for(BlockPos pos : vol.unsetPoints())
				data.put(pos, new OptionData(tileSet).initialise(pos, map, vol));
			
			sequence.addAll(data.keySet());
			sequence.sort(placeOrder);
		}
		
		/** Removes the given position and updates all relevant candidates for remaining positions */
		public void removeAndUpdate(BlockPos pos, BlueprintTileGrid map, GridVolume vol)
		{
			data.remove(pos);
			sequence.remove(pos);
			
			boolean anyChange = false;
			for(BlockPos p2 : vol.unsetPoints())
			{
				int constraint = data.get(p2).constraint();
				data.get(p2).update(pos, p2, map, vol);
				if(constraint > data.get(p2).constraint())
					anyChange = true;
			}
			
			// If any candidates became more/less constrained, update the sequence
			if(anyChange)
				sequence.sort(placeOrder);
		}
		
		/** Returns the position with the fewest viable options */
		@Nullable
		public BlockPos getMostConstrainedAmong(List<BlockPos> set)
		{
			int minIndex = Integer.MAX_VALUE;
			BlockPos earliest = null;
			for(BlockPos pos : set)
			{
				int index = sequence.indexOf(pos);
				if(index < minIndex)
				{
					earliest = pos;
					minIndex = index;
				}
			}
			return earliest;
		}
		
		public List<Tile> getOptionsFor(BlockPos pos)
		{
			return data.get(pos).options();
		}
		
		public static class OptionData
		{
			/** Map of tiles affected at different levels of sensitivity */
			private Map<GridSensitivity,List<Tile>> candidates = new HashMap<>();
			/** Map of tiles to their viability at the end of the last update */
			private Map<Tile, Boolean> viability = new HashMap<>();
			private List<Tile> options = List.of();
			
			public OptionData(List<Tile> options)
			{
				for(GridSensitivity space : GridSensitivity.values())
					candidates.put(space, options.stream().filter(t -> t.sensitivity() == space).toList());
				options.forEach(t -> viability.put(t, false));
			}
			
			/** Updates all tile options at once */
			public OptionData initialise(BlockPos pos, BlueprintTileGrid map, GridVolume vol)
			{
				for(Tile tile : viability.keySet())
					updateTile(tile, pos, map, vol);
				
				options = viability.entrySet().stream()
						.filter(Entry::getValue)
						.map(Entry::getKey)
						.toList();
				return this;
			}
			
			public boolean update(BlockPos origin, BlockPos pos, BlueprintTileGrid map, GridVolume vol)
			{
				boolean anyChange = false;
				
				// Update regional tiles
				final BlockPos offset = origin.subtract(pos);
				for(Tile tile : candidates.getOrDefault(GridSensitivity.REGIONAL, List.of()))
				{
					if(!tile.placementAffectedBy(offset))
						continue;
					
					anyChange = updateTile(tile, pos, map, vol) || anyChange;
				}
				
				// Update adjacent tiles
				if(pos.getManhattanDistance(origin) == 1)
					for(Tile tile : candidates.getOrDefault(GridSensitivity.ADJACENT, List.of()))
						anyChange = updateTile(tile, pos, map, vol) || anyChange;
				
				// If anything changed, update the options cache
				if(anyChange)
					options = viability.entrySet().stream().filter(Entry::getValue).map(Entry::getKey).toList();
				
				return anyChange;
			}
			
			protected boolean updateTile(Tile tile, BlockPos pos, BlueprintTileGrid map, GridVolume vol)
			{
				final boolean prev = viability.get(tile);
				final boolean now = tile.canExistAt(pos, map, vol);
				viability.put(tile, now);
				return prev != now;
			}
			
			public List<Tile> options() { return options; }
			
			public int constraint() { return options().size(); }
		}
	}
}
