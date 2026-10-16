package com.lying.grid;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Lists;
import com.lying.block.entity.IRoomTaggedBlock;
import com.lying.init.CDLoggers;
import com.lying.init.CDTiles;
import com.lying.utility.logging.DebugLogger;
import com.lying.worldgen.theme.Theme;
import com.lying.worldgen.tile.Tile;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

public class BlueprintTileGrid extends AbstractTileGrid<BlockPos>
{
	public static final DebugLogger LOGGER = CDLoggers.WFC;
	public static final Tile BLANK	= CDTiles.BLANK.get();
	private final Map<BlockPos, TileInstance> finalised = new HashMap<>();
	
	private BlockPos 
		min = null, 
		max = null;
	
	public static BlueprintTileGrid fromGraphGrid(GraphTileGrid graph, int height)
	{
		BlueprintTileGrid map = new BlueprintTileGrid();
		graph.contents().forEach(tile -> 
		{
			BlockPos pos = new BlockPos(tile.x, 0, tile.y);
			for(int i=0; i<height; i++)
				map.addToVolume(pos.up(i));
		});
		return map;
	}
	
	public BlueprintTileGrid addTilesFor(GridVolume vol)
	{
		vol.contents().forEach(this::addToVolume);
		return this;
	}
	
	public AbstractTileGrid<BlockPos> addToVolume(BlockPos pos)
	{
		final int x = pos.getX(), y = pos.getY(), z = pos.getZ();
		if(min == null)
			min = pos;
		else
		{
			if(x < min.getX())
				min = new BlockPos(x, min.getY(), min.getZ());
			if(y < min.getY())
				min = min.withY(y);
			if(z < min.getZ())
				min = new BlockPos(min.getX(), min.getY(), z);
		}
		
		if(max == null)
			max = pos;
		else
		{
			if(x > max.getX())
				max = new BlockPos(x, max.getY(), max.getZ());
			if(y > max.getY())
				max = max.withY(y);
			if(z > max.getZ())
				max = new BlockPos(max.getX(), max.getY(), z);
		}
		
		return super.addToVolume(pos);
	}
	
	public boolean containsAdjacent(BlockPos pos)
	{
		// Any point more than 1 increment outside the bounds of this grid cannot have an adjacent point within it
		final int x = pos.getX(), y = pos.getY(), z = pos.getZ();
		if(x < (min.getX() - 1) || x > (max.getX() + 1))
			return false;
		if(y < (min.getY() - 1) || y > (max.getY() + 1))
			return false;
		if(z < (min.getZ() - 1) || z > (max.getZ() + 1))
			return false;
		
		return set.keySet().stream().anyMatch(p2 -> p2.getManhattanDistance(pos) == 1);
	}
	
	public boolean contains(BlockPos pos)
	{
		final int x = pos.getX(), y = pos.getY(), z = pos.getZ();
		if(x < min.getX() || x > max.getX())
			return false;
		if(y < min.getY() || y > max.getY())
			return false;
		if(z < min.getZ() || z > max.getZ())
			return false;
		
		return super.contains(pos);
	}
	
	public boolean isBoundary(BlockPos pos, Direction side) { return !contains(pos.offset(side)); }
	
	public List<BlockPos> getBoundaries(List<Direction> faces)
	{
		return set.keySet().stream().filter(p -> faces.stream().anyMatch(f -> isBoundary(p, f))).toList();
	}
	
	public List<Tile> getOptionsFor(BlockPos pos, List<Tile> useable)
	{
		List<Tile> options = Lists.newArrayList();
		
		// Cache the result so we don't have to recalculate it every single step of tile generation
		options.addAll(optionCache.getOrDefault(pos, useable.stream().filter(t -> t.canExistAt(pos, this)).toList()));
		if(!optionCache.containsKey(pos))
			optionCache.put(pos, options);
		
		return options;
	}
	
	public void put(BlockPos pos, @Nullable Tile tile)
	{
		// FIXME Check tile priority to prevent overwriting
//		if(tile != null && !tile.isBlank())
//		{
//			Optional<Tile> existing;
//			if(tile != null && (existing = get(pos)).isPresent() && !existing.get().isBlank())
//			{
//				// Avoid setting tiles when the new tile is lower priority than the existing one
//				final int priorityEx = existing.get().priority();
//				final int priorityIn = tile.priority();
//				if(priorityEx != priorityIn && (priorityEx < 0 || priorityEx > priorityIn))
//					return;
//			}
//		}
		super.put(pos, tile);
		if(finalised.containsKey(pos))
			finalised.remove(pos);
	}
	
	public BlueprintTileGrid finaliseRegion(GridVolume vol, Theme theme, Random rand)
	{
		LOGGER.info("Finalising tile set region...");
		return finaliseRegion(vol.contents(), theme, rand);
	}
	
	/** Populates the finalised map with tile instances with appropriate orientations */
	public BlueprintTileGrid finaliseGrid(Theme theme, Random rand)
	{
		LOGGER.info("Finalising total tile set...");
		return finaliseRegion(this.contents(), theme, rand);
	}
	
	protected BlueprintTileGrid finaliseRegion(Collection<BlockPos> set, Theme theme, Random rand)
	{
		for(BlockPos pos : set)
		{
			if(finalised.containsKey(pos))
				continue;
			
			Tile tile = get(pos).orElse(null);
			if(tile == null || tile.isFlag())
				continue;
			
			finalise(new TileInstance(
					pos, 
					tile, 
					theme, 
					tile.assignRotation(pos, this, this::get, rand), 
					Optional.empty()));	// FIXME Add room tag
		}
		return this;
	}
	
	public void finalise(TileInstance instance)
	{
		finalised.put(instance.pos(), instance);
	}
	
	/** Generates the finalised map into the world at the given position */
	public boolean generate(BlockPos origin, ServerWorld world)
	{
		if(finalised.isEmpty())
		{
			LOGGER.warn("Attempted to generate empty or non-finalised tile set");
			return false;
		}
		
		finalised.values().forEach(entry -> entry.generate(origin.add(entry.pos().multiply(Tile.TILE_SIZE)), world));
		LOGGER.info("Tile set generated successfully");
		return true;
	}
	
	public static record TileInstance(BlockPos pos, Tile tile, Theme theme, BlockRotation rotation, Optional<UUID> room)
	{
		public void generate(BlockPos position, ServerWorld world)
		{
			tile.generate(this, position, world);
			room.ifPresent(id -> 
				BlockPos.Mutable.iterate(position, position.add(1, 1, 1)).forEach(pos -> 
				{
					BlockEntity entity = world.getBlockEntity(pos);
					if(entity != null && entity instanceof IRoomTaggedBlock)
						((IRoomTaggedBlock)entity).setRoom(id);
				})
			);
		}
	}
}
