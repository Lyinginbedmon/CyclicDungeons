package com.lying.worldgen;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Lists;
import com.lying.init.CDTiles;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class TileSet
{
	public static final Tile BLANK	= CDTiles.BLANK.get();
	
	private final Map<BlockPos, Tile> set = new HashMap<>();
	private final Map<BlockPos, List<Tile>> optionCache = new HashMap<>();
	
	public static TileSet ofSize(BlockPos size)
	{
		TileSet map = new TileSet();
		for(int x=0; x<size.getX(); x++)
			for(int z=0; z<size.getZ(); z++)
				for(int y=0; y<size.getY(); y++)
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
		return set.keySet().stream().anyMatch(k -> isSamePos(k, pos));
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
		points.stream().filter(p -> get(p).isBlank() && tile.canExistAt(p, this)).forEach(p -> put(p, tile));
	}
	
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
	
	@Nullable
	public Tile get(BlockPos pos)
	{
		if(contains(pos))
			return set.getOrDefault(pos, BLANK);
		return null;
	}
	
	public void put(BlockPos pos, @Nullable Tile tile)
	{
		set.put(pos, tile == null ? BLANK : tile);
		
		// Update the tile options for neighbouring positions
		for(Direction d : Direction.values())
		{
			BlockPos neighbour = pos.offset(d);
			if(contains(neighbour))
				optionCache.put(pos, CDTiles.getUseableTiles().stream().filter(t -> t.canExistAt(pos, this)).toList());
		}
	}
	
	public void generate(BlockPos origin, ServerWorld world)
	{
		set.entrySet().forEach(entry -> 
		{
			BlockPos pos = origin.add(entry.getKey().multiply(Tile.TILE_SIZE));
			entry.getValue().generate(pos, world);
		});
	}
	
	private static boolean isSamePos(BlockPos a, BlockPos b) { return a.getSquaredDistance(b) < 1; }
}
