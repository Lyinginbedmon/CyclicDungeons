package com.lying.worldgen;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiPredicate;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Lists;
import com.lying.init.CDLoggers;
import com.lying.init.CDTiles;
import com.lying.utility.DebugLogger;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

public class TileSet
{
	public static final DebugLogger LOGGER = CDLoggers.WFC;
	public static final Tile BLANK	= CDTiles.BLANK.get();
	public static final Random rand = Random.create();
	
	private final Map<BlockPos, Tile> set = new HashMap<>();
	private final Map<BlockPos, List<Tile>> optionCache = new HashMap<>();
	
	private final List<TileInstance> finalised = Lists.newArrayList();
	
	public static TileSet ofSize(BlockPos size)
	{
		TileSet map = new TileSet();
		
		int sizeX = Math.abs(size.getX());
		sizeX = sizeX == 0 ? 1 : sizeX;
		
		int sizeY = Math.abs(size.getY());
		sizeY = sizeY == 0 ? 1 : sizeY;
		
		int sizeZ = Math.abs(size.getZ());
		sizeZ = sizeZ == 0 ? 1 : sizeZ;
		
		for(int x=0; x<sizeX; x++)
			for(int z=0; z<sizeZ; z++)
				for(int y=0; y<sizeY; y++)
					map.addToVolume(new BlockPos(x, y, z));
		return map;
	}
	
	public int volume() { return set.size(); }
	
	public boolean isEmpty() { return set.isEmpty(); }
	
	public TileSet addToVolume(BlockPos pos)
	{
		set.put(pos, BLANK);
		return this;
	}
	
	public TileSet addToVolume(BlockPos from, BlockPos to)
	{
		BlockPos.Mutable.iterate(from, to).forEach(p -> addToVolume(p.toImmutable()));
		return this;
	}
	
	public TileSet removeFromVolume(BlockPos pos)
	{
		set.remove(pos);
		return this;
	}
	
	/** Expands the map in the given direction from all pre-existing positions */
	public void grow(Direction direction)
	{
		grow(direction, 1);
	}
	
	/** Expands the map in the given direction from all pre-existing positions */
	public void grow(Direction direction, int size)
	{
		// Filter to boundary positions to reduce excessive calls
		set.keySet().stream().filter(p -> isBoundary(p,direction)).toList().forEach(p -> 
		{
			for(int i=0; i<size; i++)
				addToVolume(p.offset(direction));
		});
	}
	
	public BlockPos size()
	{
		int x = Integer.MIN_VALUE, y = Integer.MIN_VALUE, z = Integer.MIN_VALUE;
		for(BlockPos pos : set.keySet())
		{
			if(pos.getX() > x)
				x = pos.getX();
			if(pos.getY() > y)
				y = pos.getY();
			if(pos.getZ() > z)
				z = pos.getZ();
		}
		return new BlockPos(x, y, z).add(1, 1, 1);
	}
	
	public boolean contains(BlockPos pos)
	{
		return set.keySet().stream().anyMatch(k -> k.getSquaredDistance(pos) < 1);
	}
	
	public boolean isBoundary(BlockPos pos, Direction side) { return !contains(pos.offset(side)); }
	
	public List<BlockPos> getBoundaries()
	{
		return getBoundaries(Direction.stream().toList());
	}
	
	public List<BlockPos> getBoundaries(List<Direction> faces)
	{
		List<BlockPos> boundaries = Lists.newArrayList();
		boundaries.addAll(set.keySet().stream().filter(p -> faces.stream().anyMatch(d -> isBoundary(p,d))).toList());
		return boundaries;
	}
	
	public void applyToAllValid(Tile tile)
	{
		Collection<BlockPos> points = set.keySet();
		points.stream().filter(this::contains).filter(p -> get(p).get().isBlank() && tile.canExistAt(p, this)).forEach(p -> put(p, tile));
	}
	
	public boolean hasBlanks() { return getBlanks().isEmpty(); }
	
	public List<BlockPos> getBlanks()
	{
		List<BlockPos> blanks = Lists.newArrayList();
		set.entrySet().forEach(entry -> 
		{
			if(entry.getValue().isBlank())
				blanks.add(entry.getKey());
		});
		return blanks;
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
	
	public void clearOptionCache()
	{
		optionCache.clear();
	}
	
	public Optional<Tile> get(BlockPos pos)
	{
		return contains(pos) ? Optional.of(set.getOrDefault(pos, BLANK)) : Optional.empty();
	}
	
	public List<BlockPos> getTiles(BiPredicate<BlockPos,Tile> predicate)
	{
		return set.entrySet().stream().filter(e -> predicate.test(e.getKey(), e.getValue())).map(Entry::getKey).toList();
	}
	
	public void put(BlockPos pos, @Nullable Tile tile)
	{
		finalised.clear();
		set.put(pos, tile == null ? BLANK : tile);
	}
	
	/** Populates the finalised map with tile instances with appropriate orientations */
	public void finalise()
	{
		LOGGER.info("Finalising tile set...");
		finalised.clear();
		set.entrySet().forEach(entry -> 
		{
			Tile tile = entry.getValue();
			if(tile == null || tile.isFlag())
				return;
			
			BlockPos pos = entry.getKey();
			BlockRotation rotation = tile.assignRotation(pos, this::get, rand);
			finalised.add(new TileInstance(pos, tile, rotation));
		});
		LOGGER.info("Tile set finalised");
	}
	
	/** Returns the finalised contents of this tile set */
	public List<TileInstance> contents() { return finalised; }
	
	/** Generates the finalised map into the world at the given position */
	public boolean generate(BlockPos origin, ServerWorld world)
	{
		if(finalised.isEmpty())
		{
			LOGGER.warn("Attempted to generate empty or non-finalised tile set");
			return false;
		}
		
		finalised.forEach(entry -> entry.generate(origin.add(entry.pos().multiply(Tile.TILE_SIZE)), world));
		LOGGER.info("Tile set generated successfully");
		return true;
	}
	
	public static record TileInstance(BlockPos pos, Tile tile, BlockRotation rotation)
	{
		public void generate(BlockPos position, ServerWorld world)
		{
			tile.generate(this, position, world);
		}
	}
}
