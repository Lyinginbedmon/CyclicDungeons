package com.lying.init;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Lists;
import com.lying.CyclicDungeons;
import com.lying.worldgen.Tile;

import net.minecraft.util.Identifier;

public class CDTileTags
{
	private static int tally = 0;
	
	private static final Map<Identifier, TileTag> TAGS	= new HashMap<>();
	
	public static final Identifier
		ID_SOLID_FLOORING	= prefix("solid_flooring"),
		ID_WET				= prefix("wet"),
		ID_DAMP				= prefix("damp"),
		ID_HOT				= prefix("hot"),
		ID_CEILING			= prefix("ceiling"),
		ID_TABLES			= prefix("tables"),
		ID_LIGHTING			= prefix("lighting"),
		ID_DECOR			= prefix("decor"),
		ID_OBTRUSIVE		= prefix("obtrusive"),
		ID_TRAPS			= prefix("traps");
	
	public static final TileTag SOLID_FLOORING	= make(ID_SOLID_FLOORING);
	public static final TileTag WET				= make(ID_WET);
	public static final TileTag DAMP			= make(ID_DAMP);
	public static final TileTag HOT				= make(ID_HOT);
	public static final TileTag CEILING			= make(ID_CEILING);
	public static final TileTag TABLES			= make(ID_TABLES);
	public static final TileTag LIGHTING		= make(ID_LIGHTING);
	public static final TileTag DECOR			= make(ID_DECOR);
	public static final TileTag OBTRUSIVE		= make(ID_OBTRUSIVE);
	public static final TileTag TRAPS			= make(ID_TRAPS);
	
	public static TileTag make(String name, Identifier... idsIn)
	{
		return make(prefix(name), idsIn);
	}
	
	public static TileTag make(Identifier registryName, Identifier... idsIn)
	{
		tally++;
		TileTag tag = new TileTag(registryName, idsIn);
		TAGS.put(registryName, tag);
		return tag;
	}
	
	public Optional<TileTag> get(Identifier registryName) { return TAGS.containsKey(registryName) ? Optional.of(TAGS.get(registryName)) : Optional.empty(); }
	
	public static void init()
	{
		CyclicDungeons.LOGGER.info("# Initialised {} tile tags", tally);
		
		List<Tile> allTiles = CDTiles.getAllTiles();
		TAGS.values().forEach(tag -> allTiles.stream().filter(tile -> tile.isIn(tag)).map(Tile::registryName).forEach(tag::add));
		
		// TAGS.entrySet().forEach(entry -> CyclicDungeons.LOGGER.info(" # {} : {} tiles", entry.getKey().toString(), entry.getValue().contents.size()));
	}
	
	public static final class TileTag
	{
		private final Identifier id;
		private final List<Identifier> contents = Lists.newArrayList();
		
		public TileTag(Identifier id)
		{
			this.id = id;
		}
		
		public TileTag(Identifier id, Identifier... tilesIn)
		{
			this(id);
			for(Identifier tile : tilesIn)
				add(tile);
		}
		
		public Identifier id() { return id; }
		
		public List<Identifier> contents() { return contents; }

		public boolean contains(Tile tileIn) { return contains(tileIn.registryName()); }
		
		public boolean contains(Identifier idIn) { return contents.stream().anyMatch(idIn::equals); }
		
		public TileTag add(Identifier id)
		{
			if(!contents.contains(id))
				contents.add(id);
			return this;
		}
	}
}
