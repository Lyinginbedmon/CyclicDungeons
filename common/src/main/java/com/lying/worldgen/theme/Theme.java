package com.lying.worldgen.theme;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonElement;
import com.lying.grammar.GrammarTerm;
import com.lying.grammar.content.BattleRoomContent.EncounterSet;
import com.lying.grammar.content.TrapRoomContent.TrapEntry;
import com.lying.grammar.content.battle.BattleEntry;
import com.lying.init.CDBattleEntries;
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

public record Theme(Identifier registryName, EncounterSet combatEncounters, List<Identifier> trapEncounters, Map<Identifier,Identifier> tileSets, Optional<Identifier> passageTiles)
{
	// TODO Incorporate term sets and initial grammar phrases into themes
	public static final Codec<Theme> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			Identifier.CODEC.fieldOf("id").forGetter(Theme::registryName),
			EncounterSet.CODEC.fieldOf("encounters").forGetter(Theme::combatEncounters),
			Identifier.CODEC.listOf().fieldOf("traps").forGetter(Theme::trapEncounters),
			TileSetEntry.CODEC.listOf().fieldOf("tilesets").forGetter(Theme::tilesetEntries),
			Identifier.CODEC.optionalFieldOf("passage_tileset").forGetter(Theme::passageTiles)
			).apply(instance, (id,mobs,traps,tiles,passage)-> 
			{
				Map<Identifier, Identifier> tileSets = new HashMap<>();
				tiles.forEach(t -> tileSets.put(t.roomId(), t.tilesetId()));
				return new Theme(id, mobs, traps, tileSets, passage);
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
				Identifier.CODEC.fieldOf("room").forGetter(TileSetEntry::roomId), 
				Identifier.CODEC.fieldOf("tileset").forGetter(TileSetEntry::tilesetId)
				).apply(instance, Theme.TileSetEntry::new));
	}
}