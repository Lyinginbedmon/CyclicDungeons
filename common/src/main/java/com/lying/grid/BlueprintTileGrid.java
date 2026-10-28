package com.lying.grid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiPredicate;

import org.jetbrains.annotations.Nullable;

import com.lying.block.entity.IRoomTaggedBlock;
import com.lying.init.CDTiles;
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
	private final GridVolume volume = new GridVolume();
	private final Map<BlockPos, TileInstance> finalised = new HashMap<>();
	protected final Map<BlockPos, List<Tile>> optionCache = new HashMap<>();
	
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
		
		volume.add(pos);
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
		
		for(Direction face : Direction.values())
			if(contains(pos.offset(face)))
				return true;
		
		return false;
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
		return volume.getBoundaries(faces);
	}
	
	public final List<BlockPos> getMatchingTilesWithin(GridVolume volume, BiPredicate<BlockPos,Tile> predicate)
	{
		List<BlockPos> matches = new ArrayList<>();
		for(BlockPos pos : volume.contents())
			if(!isEmpty(pos) && predicate.test(pos, get(pos).get()))
				matches.add(pos);
		return matches;
	}
	
	public final void clearOptionCache()
	{
		optionCache.clear();
	}
	
	public List<Tile> getOptionsFor(BlockPos pos, List<Tile> useable, GridVolume volume)
	{
		if(optionCache.containsKey(pos))
			return optionCache.get(pos);
		
		List<Tile> options = new ArrayList<>();
		
		// Cache the result so we don't have to recalculate it every single step of tile generation
		for(Tile option : useable)
			if(option.canExistAt(pos, this, volume))
				options.add(option);
		
		if(!optionCache.containsKey(pos))
			optionCache.put(pos, options);
		
		return options;
	}
	
	public void put(BlockPos pos, @Nullable Tile tile)
	{
		if(tile != null && !tile.isBlank())
		{
			Optional<Tile> existing;
			if(tile != null && (existing = get(pos)).isPresent() && !existing.get().isBlank())
			{
				// Avoid setting tiles when the new tile is lower priority than the existing one
				final int priorityEx = existing.get().priority();
				final int priorityIn = tile.priority();
				if(priorityEx == priorityIn)
					return;
				if(priorityEx < 0)
					return;
				if(priorityIn >= 0 && priorityIn < priorityEx)
					return;
			}
		}
		super.put(pos, tile);
		if(isFinalised(pos))
			finalised.remove(pos);
	}
	
	public boolean isFinalised(BlockPos pos) { return finalised.keySet().stream().anyMatch(pos::equals); }
	
	/** Populates the finalised map with tile instances with appropriate orientations */
	public BlueprintTileGrid finaliseGrid(Theme theme, Random rand)
	{
		LOGGER.info("Finalising total tile set...");
		finaliseRegion(volume, Optional.empty(), theme, rand);
		
		List<BlockPos> empties = new ArrayList<>();
		empties.addAll(volume.contents());
		empties.removeAll(finalised.keySet());
		empties.forEach(p -> finalised.put(p, new TileInstance(p, CDTiles.AIR.get(), theme, BlockRotation.NONE, Optional.empty())));
		
		return finaliseRegion(volume, Optional.empty(), theme, rand);
	}
	
	public BlueprintTileGrid finaliseRegion(GridVolume region, Optional<UUID> uuid, Theme theme, Random rand)
	{
		LOGGER.info("Finalising tile set region...");
		region.contents().forEach(pos ->
		{
			Tile tile = get(pos).orElse(null);
			if(tile != null && !tile.isBlank())
				finalise(new TileInstance(
						pos, 
						tile, 
						theme, 
						tile.assignRotation(pos, this, region, this::get, rand),
						uuid));
		});
		return this;
	}
	
	public void finalise(TileInstance instance)
	{
		if(instance.tile().isBlank())
			return;
		
		final BlockPos pos = instance.pos();
		if(get(pos).get() != instance.tile())
			return;
		
		if(!isFinalised(pos))
			finalised.put(pos, instance);
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
	
	public static boolean tryTagBlockAt(BlockPos pos, ServerWorld world, UUID uuid)
	{
		BlockEntity e = world.getBlockEntity(pos);
		IRoomTaggedBlock taggable;
		if(e != null && e instanceof IRoomTaggedBlock && !(taggable = (IRoomTaggedBlock)e).hasRoom())
		{
			taggable.setRoom(uuid);
			return true;
		}
		return false;
	}
	
	public static record TileInstance(BlockPos pos, Tile tile, Theme theme, BlockRotation rotation, Optional<UUID> uuid)
	{
		public void generate(BlockPos position, ServerWorld world)
		{
			tile.generate(this, position, world);
			
			uuid.ifPresent(id -> 
			{
				BlockPos.Mutable.iterate(
						position, 
						position.add(Tile.TILE_SIZE, Tile.TILE_SIZE, Tile.TILE_SIZE))
					.forEach(p -> tryTagBlockAt(p, world, id));
			}
			);
		}
	}
}
