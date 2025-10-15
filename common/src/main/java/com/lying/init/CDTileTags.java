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
	
	public static final TileTag SOLID_FLOORING	= make("solid_flooring", 
			CDTiles.ID_FLOOR_ROOM,
			CDTiles.ID_FLOOR_PASSAGE);
	public static final TileTag CEILING			= make("ceiling");
	public static final TileTag TABLES			= make("tables", 
			CDTiles.ID_TABLE, 
			CDTiles.ID_LIGHT_TABLE);
	public static final TileTag LIGHTING		= make("lighting", 
			CDTiles.ID_LIGHT_FLOOR, 
			CDTiles.ID_LIGHT_TABLE);
	public static final TileTag DECOR			= make("decor", 
			CDTiles.ID_LIGHT_FLOOR, 
			CDTiles.ID_TABLE, 
			CDTiles.ID_LIGHT_TABLE, 
			CDTiles.ID_SEAT,
			CDTiles.ID_WORKSTATION);
	public static final TileTag OBTRUSIVE		= make("obtrusive", 
			CDTiles.ID_TABLE, 
			CDTiles.ID_LIGHT_TABLE, 
			CDTiles.ID_SEAT);
	
	private static TileTag make(String name, Identifier... idsIn)
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
