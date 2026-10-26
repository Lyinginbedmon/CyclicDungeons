package com.lying.grid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.lying.init.CDLoggers;
import com.lying.init.CDTiles;
import com.lying.utility.logging.DebugLogger;
import com.lying.worldgen.tile.Tile;

import net.minecraft.util.math.Direction;

public abstract class AbstractTileGrid<T extends Object>
{
	public static final DebugLogger LOGGER = CDLoggers.WFC;
	public static final Tile BLANK	= CDTiles.BLANK.get();
	
	protected final Map<T, Tile> set = new HashMap<>();
	
	public final int volume() { return set.size(); }
	
	public final boolean isEmpty() { return set.isEmpty(); }
	
	public AbstractTileGrid<T> addToVolume(T pos)
	{
		if(!contains(pos))
			setTile(pos, BLANK);
		return this;
	}
	
	public final AbstractTileGrid<T> addAllToVolume(Collection<T> positions)
	{
		positions.forEach(this::addToVolume);
		return this;
	}
	
	public boolean contains(T pos)
	{
		return set.keySet().stream().anyMatch(pos::equals);
	}
	
	public final Optional<Tile> get(T pos)
	{
		if(contains(pos))
			return Optional.of(set.get(pos));
		else
		{
			LOGGER.warn("Tried to retrieve position this grid does not recognise");
			return Optional.empty();
		}
	}
	
	public void put(T pos, @Nullable Tile tile)
	{
		if(tile == null)
			LOGGER.warn("Attempted to set null tile into grid");
		else if(!contains(pos))
		{
			LOGGER.warn("Attempted to set position outside of grid volume");
			return;
		}
		
		setTile(pos, tile == null ? BLANK : tile);
	}
	
	public final boolean isEmpty(T pos) { return get(pos).isEmpty() || get(pos).get().isBlank(); }
	
	protected void setTile(T pos, @NotNull Tile tile)
	{
		set.put(pos, tile);
	}
	
	public final Collection<T> contents() { return set.keySet(); }
	
	public abstract List<T> getBoundaries(List<Direction> faces);
	
	public final List<T> getBlanks()
	{
		List<T> blanks = new ArrayList<>();
		set.entrySet().stream().filter(entry -> entry.getValue().isBlank()).map(Entry::getKey).forEach(blanks::add);
		return blanks;
	}
}
