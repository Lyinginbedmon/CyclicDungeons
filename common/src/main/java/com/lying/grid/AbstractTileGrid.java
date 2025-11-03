package com.lying.grid;

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
import com.lying.worldgen.Tile;

import net.minecraft.util.math.Direction;

public abstract class AbstractTileGrid<T extends Object>
{
	public static final DebugLogger LOGGER = CDLoggers.WFC;
	public static final Tile BLANK	= CDTiles.BLANK.get();
	
	protected final Map<T, Tile> set = new HashMap<>();
	protected final Map<T, List<Tile>> optionCache = new HashMap<>();
	
	public final int volume() { return set.size(); }
	
	public final boolean isEmpty() { return set.isEmpty(); }
	
	public final AbstractTileGrid<T> addToVolume(T pos)
	{
		set.put(pos, BLANK);
		return this;
	}
	
	public final AbstractTileGrid<T> addAllToVolume(Collection<T> positions)
	{
		positions.forEach(this::addToVolume);
		return this;
	}
	
	public abstract AbstractTileGrid<T> addToVolume(T from, T to);
	
	public final AbstractTileGrid<T> removeFromVolume(T pos)
	{
		set.remove(pos);
		return this;
	}
	
	/** Expands the map in the given direction from all pre-existing positions */
	public final void grow(Direction direction)
	{
		grow(direction, 1);
	}
	
	/** Expands the map in the given direction from all pre-existing positions */
	public abstract void grow(Direction direction, int size);
	
	public final boolean contains(T pos)
	{
		return set.keySet().stream().anyMatch(pos::equals);
	}
	
	public abstract boolean containsAdjacent(T pos);
	
	public final boolean containsOrAdjacent(T pos)
	{
		return contains(pos) || containsAdjacent(pos);
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
		set.put(pos, tile == null ? BLANK : tile);
	}
	
	public final Collection<T> contents() { return set.keySet(); }
	
	public final List<T> getBoundaries()
	{
		return getBoundaries(Direction.Type.HORIZONTAL.stream().toList());
	}
	
	public abstract List<T> getBoundaries(List<Direction> faces);
	
	public final boolean hasBlanks() { return getBlanks().isEmpty(); }
	
	public final List<T> getBlanks()
	{
		List<T> blanks = Lists.newArrayList();
		set.entrySet().stream().filter(entry -> entry.getValue().isBlank()).map(Entry::getKey).forEach(blanks::add);
		return blanks;
	}
	
	public final void clearOptionCache()
	{
		optionCache.clear();
	}
	
	public final List<T> getMatchingTiles(BiPredicate<T,Tile> predicate)
	{
		return set.entrySet().stream().filter(e -> predicate.test(e.getKey(), e.getValue())).map(Entry::getKey).toList();
	}
}
