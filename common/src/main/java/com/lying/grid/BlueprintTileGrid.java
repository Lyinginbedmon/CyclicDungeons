package com.lying.grid;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Lists;
import com.lying.init.CDLoggers;
import com.lying.init.CDTiles;
import com.lying.utility.DebugLogger;
import com.lying.worldgen.Tile;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

public class BlueprintTileGrid extends AbstractTileGrid<BlockPos>
{
	public static final DebugLogger LOGGER = CDLoggers.WFC;
	public static final Tile BLANK	= CDTiles.BLANK.get();
	public static final Random rand = Random.create();
	private final List<TileInstance> finalised = Lists.newArrayList();
	
	public static BlueprintTileGrid ofSize(BlockPos size)
	{
		BlueprintTileGrid map = new BlueprintTileGrid();
		
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
	
	public BlueprintTileGrid addToVolume(BlockPos from, BlockPos to)
	{
		BlockPos.Mutable.iterate(from, to).forEach(p -> addToVolume(p.toImmutable()));
		return this;
	}
	
	public boolean containsAdjacent(BlockPos pos)
	{
		return set.keySet().stream().anyMatch(p2 -> p2.getManhattanDistance(pos) == 1);
	}
	
	/** Expands the map in the given direction from all pre-existing positions */
	public void grow(Direction direction, int size)
	{
		getBoundaries(List.of(direction)).forEach(p -> 
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
	
	public boolean isBoundary(BlockPos pos, Direction side) { return !contains(pos.offset(side)); }
	
	public List<BlockPos> getBoundaries(List<Direction> faces)
	{
		return set.keySet().stream().filter(p -> faces.stream().anyMatch(f -> isBoundary(p, f))).toList();
	}
	
	public void applyToAllValid(Tile tile)
	{
		Collection<BlockPos> points = set.keySet();
		points.stream()
			.filter(p -> get(p).get().isBlank() && tile.canExistAt(p, this))
			.forEach(p -> put(p, tile));
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
		super.put(pos, tile);
		finalised.clear();
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
		if(!finalised.isEmpty())
			LOGGER.info("Tile set finalised");
		else
			LOGGER.warn("Error while finalising tile set");
	}
	
	public void finalise(TileInstance instance)
	{
		finalised.removeIf(i -> i.pos().getManhattanDistance(instance.pos()) == 0);
		finalised.add(instance);
	}
	
	/** Returns the finalised contents of this tile set */
	public List<TileInstance> getTiles() { return finalised; }
	
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
