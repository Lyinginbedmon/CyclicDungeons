package com.lying.init;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.lying.CyclicDungeons;
import com.lying.worldgen.tile.Tile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.util.Identifier;

public class CDTileTags
{
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
	
	public static TileTag make(Identifier registryName, Identifier... idsIn)
	{
		return new TileTag(registryName, idsIn);
	}
	
	public static Optional<TileTag> get(Identifier registryName) { return TAGS.containsKey(registryName) ? Optional.of(TAGS.get(registryName)) : Optional.empty(); }
	
	public static void init()
	{
		CyclicDungeons.LOGGER.info(" # Initialised tile tag registry");
	}
	
	public static void reload()
	{
		TAGS.clear();
		for(Tile tile : CDTiles.instance().getAll())
			for(TileTag tag : tile.tags())
				if(TAGS.containsKey(tag.id()))
					TAGS.put(tag.id(), TAGS.get(tag.id()).mergeWith(tag));
				else
					TAGS.put(tag.id(), tag);
		
		CyclicDungeons.LOGGER.info(" # Loaded {} tile tags", TAGS.size());
	}
	
	public static final class TileTag
	{
		public static final Codec<TileTag> CODEC	= Identifier.CODEC.comapFlatMap(
				id -> DataResult.success(new TileTag(id)), 
				TileTag::id);
		
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
		
		public boolean canMerge(TileTag tag) { return tag.id.equals(this.id); }
		
		public TileTag mergeWith(TileTag tag)
		{
			if(!canMerge(tag))
				return this;
			
			tag.contents.stream().filter(Predicates.not(this::contains)).forEach(this::add);
			return this;
		}
		
		public TileTag add(Identifier id)
		{
			if(!contents.contains(id))
				contents.add(id);
			return this;
		}
	}
}
