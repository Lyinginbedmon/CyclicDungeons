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
	
	public static TileSet enclosing(BlockPos start, BlockPos end)
	{
		return (new TileSet()).addToVolume(start, end);
	}
	
	public int volume() { return set.size(); }
	
	public TileSet addToVolume(BlockPos pos)
	{
		set.put(pos, BLANK);
		return this;
	}
	
	public TileSet addToVolume(BlockPos start, BlockPos end)
	{
		BlockPos.Mutable.iterate(start, end).forEach(p -> addToVolume(p.toImmutable()));
		return this;
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
		set.keySet().stream().filter(p -> faces.stream().anyMatch(d -> isBoundary(p,d))).forEach(boundaries::add);
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
	
	public List<Tile> getOptionsFor(BlockPos pos)
	{
		List<Tile> options = Lists.newArrayList();
		options.addAll(CDTiles.getUseableTiles().stream().filter(t -> t.canExistAt(pos, this)).toList());
		return options;
	}
	
	@Nullable
	public Tile get(BlockPos pos)
	{
		if(contains(pos))
			return set.getOrDefault(pos, BLANK);
		return null;
	}
	
	public void put(BlockPos pos, Tile tile)
	{
		set.put(pos, tile);
	}
	
	public void generate(ServerWorld world)
	{
		set.entrySet().forEach(entry -> entry.getValue().generate(entry.getKey(), world));
	}
	
	private static boolean isSamePos(BlockPos a, BlockPos b) { return a.getSquaredDistance(b) < 1; }
}
