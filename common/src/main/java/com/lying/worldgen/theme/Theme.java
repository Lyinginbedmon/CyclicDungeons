package com.lying.worldgen.theme;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.lying.grammar.GrammarPhrase;
import com.lying.grammar.GrammarTerm;
import com.lying.grammar.content.BattleRoomContent.EncounterSet;
import com.lying.grammar.content.TrapRoomContent.TrapEntry;
import com.lying.grammar.content.battle.BattleEntry;
import com.lying.init.CDBattleEntries;
import com.lying.init.CDPhrases;
import com.lying.init.CDTerms;
import com.lying.init.CDTileSets;
import com.lying.init.CDTrapEntries;
import com.lying.worldgen.tile.Tile;
import com.lying.worldgen.tileset.TileSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.registry.RegistryKey;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePools;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

public record Theme(
		Identifier registryName, 
		List<GrammarTerm> dictionary, 
		List<InitialPhrase> phrases,
		EncounterSet combatEncounters, 
		List<Identifier> trapEncounters, 
		Map<Identifier,Identifier> tileSets, 
		Optional<Identifier> passageTiles)
{
	public static final Codec<Theme> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			Identifier.CODEC.fieldOf("id").forGetter(Theme::registryName),
			Identifier.CODEC.listOf().fieldOf("dictionary").forGetter(t -> t.dictionary.stream().map(GrammarTerm::registryName).toList()),
			Identifier.CODEC.listOf().fieldOf("phrases").forGetter(t -> t.phrases.stream().map(InitialPhrase::registryName).toList()),
			EncounterSet.CODEC.fieldOf("encounters").forGetter(Theme::combatEncounters),
			Identifier.CODEC.listOf().fieldOf("traps").forGetter(Theme::trapEncounters),
			TileSetEntry.CODEC.listOf().fieldOf("tilesets").forGetter(Theme::tilesetEntries),
			Identifier.CODEC.optionalFieldOf("passage_tileset").forGetter(Theme::passageTiles)
			).apply(instance, (id,terms,phrases,mobs,traps,tiles,passage)-> 
			{
				Map<Identifier, Identifier> tileSets = new HashMap<>();
				tiles.forEach(t -> tileSets.put(t.roomId(), t.tilesetId()));
				return new Theme(
						id, 
						terms.stream().map(CDTerms.instance()::get).filter(Optional::isPresent).map(Optional::get).toList(), 
						phrases.stream().map(CDPhrases.instance()::get).filter(Optional::isPresent).map(Optional::get).toList(),
						mobs, 
						traps, 
						tileSets, 
						passage);
			}));
	
	public static Theme fromJson(JsonOps ops, JsonElement ele)
	{
		return CODEC.parse(ops, ele).getOrThrow();
	}
	
	public JsonElement toJson(JsonOps ops)
	{
		return CODEC.encodeStart(ops, this).getOrThrow();
	}
	
	public boolean is(Theme b) { return b.registryName().equals(registryName); }
	
	public List<GrammarTerm> getPlaceableTerms()
	{
		return dictionary.stream().filter(GrammarTerm::isPlaceable).toList();
	}
	
	@Nullable
	public GrammarPhrase chooseInitialPhrase(Random rand)
	{
		List<GrammarPhrase> phraseSet = Lists.newArrayList();
		for(InitialPhrase phrase : phrases)
		{
			GrammarPhrase result = null;
			try
			{
				result = phrase.toPhrase();
			}
			catch(Exception e) { }
			if(result != null)
				phraseSet.add(result);
		}
		
		switch(phraseSet.size())
		{
			case 0:	return null;
			case 1: return phraseSet.get(0);
			default: return phraseSet.get(rand.nextInt(phraseSet.size()));
		}
	}
	
	public List<BattleEntry> encounters()
	{
		if(combatEncounters.isEmpty())
			return List.of();
		else
		{
			return combatEncounters.stream()
					.map(CDBattleEntries.instance()::get)
					.filter(Optional::isPresent)
					.map(Optional::get)
					.toList();
		}
	}
	
	public List<TrapEntry> traps()
	{
		if(trapEncounters.isEmpty())
			return List.of();
		else
			return trapEncounters.stream()
					.map(CDTrapEntries.instance()::get)
					.filter(Optional::isPresent)
					.map(Optional::get)
					.toList();
	}
	
	public TileSet getTileSet(GrammarTerm termIn)
	{
		return getTileSet(termIn.registryName());
	}
	
	public TileSet getTileSet(Identifier name)
	{
		Identifier setId = null;
		if(tileSets.containsKey(name))
			setId = tileSets.get(name);
		
		return CDTileSets.instance().get(setId).orElse(CDTileSets.DEFAULT);
	}
	
	protected List<Theme.TileSetEntry> tilesetEntries()
	{
		return tileSets.entrySet().stream().map(e -> new TileSetEntry(e.getKey(), e.getValue())).toList();
	}
	
	public TileSet passageTileSet()
	{
		Identifier passageTileSet = passageTiles.orElse(null);
		return CDTileSets.instance().get(passageTileSet).orElse(CDTileSets.DEFAULT);
	}
	
	public RegistryKey<StructurePool> getTilePool(Tile tileIn)
	{
		return getTilePool(tileIn.registryName());
	}
	
	public RegistryKey<StructurePool> getTilePool(Identifier tile)
	{
		return getTilePool(registryName(), tile);
	}
	
	public static RegistryKey<StructurePool> getTilePool(Identifier theme, Identifier tile)
	{
		return StructurePools.of(tile.getNamespace()+":dungeon/"+theme.getPath()+"/"+tile.getPath());
	}
	
	private static record TileSetEntry(Identifier roomId, Identifier tilesetId)
	{
		public static final Codec<Theme.TileSetEntry> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
				Identifier.CODEC.fieldOf("term").forGetter(TileSetEntry::roomId), 
				Identifier.CODEC.fieldOf("tile_set").forGetter(TileSetEntry::tilesetId)
				).apply(instance, Theme.TileSetEntry::new));
	}
}