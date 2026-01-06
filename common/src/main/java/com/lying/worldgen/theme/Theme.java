package com.lying.worldgen.theme;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.lying.grammar.DefaultTerms;
import com.lying.grammar.GrammarTerm;
import com.lying.grammar.content.BattleRoomContent;
import com.lying.grammar.content.BattleRoomContent.BattleEntry;
import com.lying.grammar.content.BattleRoomContent.EncounterSet;
import com.lying.grammar.content.TrapRoomContent.TrapEntry;
import com.lying.grammar.content.battle.SquadBattleEntry.SquadEntry;
import com.lying.init.CDEntityTypes;
import com.lying.init.CDTerms;
import com.lying.init.CDTileSets;
import com.lying.init.CDTraps;
import com.lying.worldgen.tile.Tile;
import com.lying.worldgen.tileset.DefaultTileSets;
import com.lying.worldgen.tileset.TileSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.entity.EntityType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePools;
import net.minecraft.util.Identifier;

public record Theme(Identifier registryName, EncounterSet combatEncounters, List<Identifier> trapEncounters, Map<Identifier,Identifier> tileSets, Optional<Identifier> passageTiles)
{
	// TODO Incorporate term sets and initial grammar phrases into themes
	public static final Codec<Theme> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
			Identifier.CODEC.fieldOf("id").forGetter(Theme::registryName),
			EncounterSet.CODEC.fieldOf("monsters").forGetter(Theme::combatEncounters),
			Identifier.CODEC.listOf().fieldOf("traps").forGetter(Theme::trapEncounters),
			TileEntry.CODEC.listOf().fieldOf("tilesets").forGetter(Theme::tilesetEntries),
			Identifier.CODEC.optionalFieldOf("passage_tileset").forGetter(Theme::passageTiles)
			).apply(instance, (id,mobs,traps,tiles,passage)-> 
			{
				Map<Identifier, Identifier> tileSets = new HashMap<>();
				tiles.forEach(t -> tileSets.put(t.roomId(), t.tilesetId()));
				return new Theme(id, mobs, traps, tileSets, passage);
			}));
	
	public static final BattleEntry ENCOUNTER_PILLAGER_SQUAD = BattleRoomContent.BASIC_SQUAD.get()
			.add(SquadEntry.Builder.of(EntityType.EVOKER).name("leader").count(0, 1).build())
			.add(SquadEntry.Builder.of(EntityType.VINDICATOR).name("elite").count(1, 2).build())
			.add(SquadEntry.Builder.of(EntityType.PILLAGER).count(2, 3).build())
			.setName(prefix("pillager_squad"));
	public static final BattleEntry ENCOUNTER_WOLF_PACK = BattleRoomContent.CROWD.get().of(CDEntityTypes.RABID_WOLF.get(), 3, 4).setName(prefix("wolf_pack"));
	public static final BattleEntry ENCOUNTER_ZOMBIE_CROWD = BattleRoomContent.CROWD.get().of(EntityType.ZOMBIE, 4, 8).setName(prefix("zombie_crowd"));
	public static final BattleEntry ENCOUNTER_SKELETONS = BattleRoomContent.CROWD.get().of(EntityType.SKELETON, 3, 5).setName(prefix("skeletons"));
	public static final EncounterSet DEFAULT_ENCOUNTERS	= new EncounterSet()
			.addEntry(Theme.ENCOUNTER_ZOMBIE_CROWD)
			.addEntry(Theme.ENCOUNTER_SKELETONS)
			.addEntry(Theme.ENCOUNTER_PILLAGER_SQUAD)
			.addEntry(Theme.ENCOUNTER_WOLF_PACK);
	
	public static final List<Identifier> DEFAULT_TRAPS	= Lists.newArrayList(
			CDTraps.ID_SIMPLE_PITFALL,
			CDTraps.ID_LAVA_RIVER
			);
	public static final Map<Identifier, Identifier> DEFAULT_TILE_SETS = Map.of(
			CDTerms.ID_START, DefaultTileSets.ID_START,
			CDTerms.ID_END, DefaultTileSets.ID_END,
			CDTerms.ID_EMPTY, DefaultTileSets.ID_EMPTY,
			DefaultTerms.ID_BATTLE, DefaultTileSets.ID_BATTLE,
			DefaultTerms.ID_TRAP, DefaultTileSets.ID_TRAP,
			DefaultTerms.ID_BIG_PUZZLE, DefaultTileSets.ID_PUZZLE,
			DefaultTerms.ID_SML_PUZZLE, DefaultTileSets.ID_PUZZLE,
			DefaultTerms.ID_BOSS, DefaultTileSets.ID_BOSS,
			CDTerms.ID_TREASURE, DefaultTileSets.ID_TREASURE
			);
	
	public static final Identifier ID_BLANK	= prefix("blank");
	
	/** Default theme value storage, used if a value is not specified or the theme cannot be retrieved */
	public static final Theme BLANK	= new Theme(
			ID_BLANK, 
			DEFAULT_ENCOUNTERS, 
			DEFAULT_TRAPS, 
			DEFAULT_TILE_SETS, 
			Optional.of(DefaultTileSets.ID_DEFAULT_PASSAGE));
	
	public static Theme fromJson(JsonOps ops, JsonElement ele)
	{
		return CODEC.parse(ops, ele).getOrThrow();
	}
	
	public JsonElement toJson(JsonOps ops)
	{
		return CODEC.encodeStart(ops, this).getOrThrow();
	}
	
	public boolean is(Theme b) { return b.registryName().equals(registryName); }
	
	public EncounterSet encounters() { return combatEncounters.isEmpty() ? BLANK.encounters() : combatEncounters; }
	
	public List<TrapEntry> traps()
	{
		List<Identifier> names = trapEncounters.isEmpty() ? BLANK.trapEncounters() : trapEncounters;
		return names.stream().map(CDTraps::get).filter(Optional::isPresent).map(Optional::get).toList();
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
		else
			return BLANK.getTileSet(name);
		
		return CDTileSets.instance().get(setId).orElse(CDTileSets.DEFAULT);
	}
	
	protected List<Theme.TileEntry> tilesetEntries()
	{
		return tileSets.entrySet().stream().map(e -> new TileEntry(e.getKey(), e.getValue())).toList();
	}
	
	public TileSet passageTileSet()
	{
		Identifier passageTileSet = passageTiles.orElse(BLANK.passageTiles().get());
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
	
	private static record TileEntry(Identifier roomId, Identifier tilesetId)
	{
		public static final Codec<Theme.TileEntry> CODEC	= RecordCodecBuilder.create(instance -> instance.group(
				Identifier.CODEC.fieldOf("room").forGetter(TileEntry::roomId), 
				Identifier.CODEC.fieldOf("tileset").forGetter(TileEntry::tilesetId)
				).apply(instance, Theme.TileEntry::new));
	}
}