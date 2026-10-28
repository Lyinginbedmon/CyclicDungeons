package com.lying.grid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.google.common.base.Predicates;
import com.lying.worldgen.tile.Tile;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class GridVolume
{
	private Map<BlockPos,Boolean> set = new HashMap<>();
	private Optional<UUID> roomID = Optional.empty();
	
	private Map<Tile,Integer> tallies = new HashMap<>();
	
	public static GridVolume fromGraphGrid(GraphTileGrid graph, int height)
	{
		GridVolume map = new GridVolume();
		graph.contents().forEach(tile -> 
		{
			BlockPos pos = new BlockPos(tile.x, 0, tile.y);
			for(int i=0; i<height; i++)
				map.add(pos.up(i));
		});
		return map;
	}
	
	public GridVolume setRoom(@Nullable UUID uuid)
	{
		roomID = uuid == null ? Optional.empty() : Optional.of(uuid);
		return this;
	}
	
	public Optional<UUID> roomId() { return roomID; }
	
	public boolean isEmpty() { return set.isEmpty(); }
	
	public int size() { return set.size(); }
	
	public Collection<BlockPos> contents() { return set.keySet(); }
	
	public List<BlockPos> unsetPoints()
	{
		List<BlockPos> values = new ArrayList<BlockPos>();
		set.keySet().stream().filter(Predicates.not(this::isSet)).forEach(values::add);
		return values;
	}
	
	public GridVolume add(BlockPos pos)
	{
		if(!contains(pos))
			set.put(pos, false);
		return this;
	}
	
	public boolean contains(BlockPos pos)
	{
		return set.containsKey(pos);
	}
	
	public boolean isSet(BlockPos pos)
	{
		return contains(pos) && set.get(pos);
	}
	
	public void set(BlockPos pos)
	{
		set.put(pos, true);
	}
	
	public void clearTallies() { tallies.clear(); }
	
	public void tally(@Nullable Tile addition, @Nullable Tile removal)
	{
		if(addition != null && !addition.isBlank())
			tallies.put(addition, tallies.getOrDefault(addition, 0) + 1);
		
		if(removal != null && !addition.isBlank())
			tallies.put(removal, Math.max(0, tallies.getOrDefault(removal, 0) - 1));
	}
	
	public int tallyOf(Tile tileIn)
	{
		return tallies.getOrDefault(tileIn, 0);
	}
	
	public void remove(BlockPos pos)
	{
		set.remove(pos);
	}
	
	public boolean isComplete()
	{
		return set.values().stream().allMatch(Boolean::booleanValue);
	}
	
	public boolean isBoundary(BlockPos pos, Direction side) { return !contains(pos.offset(side)); }
	
	public List<BlockPos> getBoundaries(List<Direction> faces)
	{
		return set.keySet().stream().filter(p -> faces.stream().anyMatch(f -> isBoundary(p, f))).toList();
	}
}
