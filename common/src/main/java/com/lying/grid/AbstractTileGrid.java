package com.lying.grid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiPredicate;

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
	protected final Map<Tile, Integer> tally = new HashMap<>();
	protected final Map<T, List<Tile>> optionCache = new HashMap<>();
	
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
	
	public final AbstractTileGrid<T> removeFromVolume(T pos)
	{
		set.remove(pos);
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
	
	private final void setTile(T pos, @NotNull Tile tile)
	{
		get(pos).ifPresent(t -> tally.put(t, tallyOf(t) - 1));
		
		set.put(pos, tile);
		tally.put(tile, tallyOf(tile) + 1);
	}
	
	public final Collection<T> contents() { return set.keySet(); }
	
	public abstract List<T> getBoundaries(List<Direction> faces);
	
	public final int tallyOf(Tile tile) { return tally.getOrDefault(tile, 0); }
	
	public final List<T> getBlanks()
	{
		List<T> blanks = new ArrayList<>();
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
