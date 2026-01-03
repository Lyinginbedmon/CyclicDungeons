package com.lying.init;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.lying.CyclicDungeons;
import com.lying.blueprint.processor.BattleRoomProcessor.BattleEntry;
import com.lying.blueprint.processor.BattleRoomProcessor.EncounterSet;
import com.lying.blueprint.processor.TrapRoomProcessor.TrapEntry;
import com.lying.blueprint.processor.battle.SimpleBattleEntry;
import com.lying.blueprint.processor.battle.SquadBattleEntry;
import com.lying.blueprint.processor.battle.SquadBattleEntry.SquadEntry;
import com.lying.grammar.GrammarTerm;
import com.lying.worldgen.tile.Tile;
import com.lying.worldgen.tileset.DefaultTileSets;
import com.lying.worldgen.tileset.TileSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.entity.EntityType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePools;
import net.minecraft.util.Identifier;

public class CDThemes
{
	private static final Map<Identifier, Supplier<Theme>> THEMES = new HashMap<>();
	
	private static final Map<Identifier, Identifier> BASIC_TILESETS = Map.of(
			CDTerms.ID_START, DefaultTileSets.ID_START,
			CDTerms.ID_END, DefaultTileSets.ID_END,
			CDTerms.ID_EMPTY, DefaultTileSets.ID_EMPTY,
			CDTerms.ID_BATTLE, DefaultTileSets.ID_BATTLE,
			CDTerms.ID_TRAP, DefaultTileSets.ID_TRAP,
			CDTerms.ID_BIG_PUZZLE, DefaultTileSets.ID_PUZZLE,
			CDTerms.ID_SML_PUZZLE, DefaultTileSets.ID_PUZZLE,
			CDTerms.ID_BOSS, DefaultTileSets.ID_BOSS,
			CDTerms.ID_TREASURE, DefaultTileSets.ID_TREASURE
			);
	
	private static final List<Identifier> BASIC_TRAPS	= List.of(
			CDTraps.ID_SIMPLE_PITFALL,
			CDTraps.ID_LAVA_RIVER
			);
	
	private static final BattleEntry ENCOUNTER_PILLAGER_SQUAD = new SquadBattleEntry(prefix("pillager_squad"))
			.add(SquadEntry.Builder.of(EntityType.EVOKER).name("leader").count(0, 1).build())
			.add(SquadEntry.Builder.of(EntityType.VINDICATOR).name("elite").count(1, 2).build())
			.add(SquadEntry.Builder.of(EntityType.PILLAGER).count(2, 3).build());
	private static final BattleEntry ENCOUNTER_WOLF_PACK = new SimpleBattleEntry<>(prefix("wolf_pack"), CDEntityTypes.RABID_WOLF.get(), 3, 4);
	private static final BattleEntry ENCOUNTER_ZOMBIE_CROWD = new SimpleBattleEntry<>(prefix("zombie_crowd"), EntityType.ZOMBIE, 4, 8);
	private static final BattleEntry ENCOUNTER_SKELETONS = new SimpleBattleEntry<>(prefix("skeletons"), EntityType.SKELETON, 3, 5);
	private static final EncounterSet BASIC_ENCOUNTERS = new EncounterSet()
			.addEntry(ENCOUNTER_ZOMBIE_CROWD)
			.addEntry(ENCOUNTER_SKELETONS)
			.addEntry(ENCOUNTER_PILLAGER_SQUAD)
			.addEntry(ENCOUNTER_WOLF_PACK);
	
	public static final Identifier
		ID_GENERIC	= prefix("generic"),
		ID_DESERT	= prefix("desert"),
		ID_UNDEAD	= prefix("undead"),
		ID_JUNGLE	= prefix("jungle"),
		ID_SWAMP	= prefix("swamp");
	
	public static final Supplier<Theme> GENERIC	= register(ID_GENERIC, 
			new EncounterSet(), 
			List.of(),
			Map.of());
	public static final Supplier<Theme> DESERT	= register(ID_DESERT, 
			new EncounterSet()
				.addSimple(prefix("husk_crowd"), EntityType.HUSK, 4, 8)
				.addSquad(prefix("fire_team"), e -> e
					.add(SquadEntry.Builder.of(EntityType.WITHER_SKELETON).build())
					.add(SquadEntry.Builder.of(EntityType.BLAZE).count(2, 3).build()))
				.addEntry(ENCOUNTER_PILLAGER_SQUAD)
				.addEntry(ENCOUNTER_WOLF_PACK), 
			List.of(),
			Map.of());
	public static final Supplier<Theme> UNDEAD	= register(ID_UNDEAD, 
			new EncounterSet()
				.addEntry(ENCOUNTER_SKELETONS)
				.addEntry(ENCOUNTER_ZOMBIE_CROWD), 
			List.of(),
			Map.of());
	public static final Supplier<Theme> JUNGLE	= register(ID_JUNGLE, 
			new EncounterSet(), 
			List.of(),
			Map.of());
	public static final Supplier<Theme> SWAMP	= register(ID_SWAMP, 
			new EncounterSet()
				.addSimple(prefix("skeletons"), EntityType.BOGGED, 3, 5)
				.addSimple(prefix("coven"), EntityType.WITCH, 2, 3)
				.addEntry(ENCOUNTER_ZOMBIE_CROWD)
				.addEntry(ENCOUNTER_PILLAGER_SQUAD), 
			List.of(),
			Map.of());
	
	private static Supplier<Theme> register(Identifier id, EncounterSet combat, List<Identifier> traps, Map<Identifier, Identifier> tileSets)
	{
		final Supplier<Theme> entry = () -> new Theme(id, combat, traps, tileSets, DefaultTileSets.ID_DEFAULT_PASSAGE);
		THEMES.put(id, entry);
		return entry;
	}
	
	public static Optional<Theme> get(Identifier id)
	{
		return THEMES.containsKey(id) ? Optional.of(THEMES.get(id).get()) : Optional.empty();
	}
	
	public static void init()
	{
		CyclicDungeons.LOGGER.info(" # Initialised {} themes", THEMES.size());
	}
	
	public static record Theme(Identifier registryName, EncounterSet combatEncounters, List<Identifier> trapEncounters, Map<Identifier,Identifier> tileSets, Identifier passageTiles)
	{
		public static final Codec<Theme> CODEC = Identifier.CODEC.comapFlatMap(id -> 
		{
			Optional<Theme> type = CDThemes.get(id);
			if(type.isPresent())
				return DataResult.success(type.get());
			else
				return DataResult.error(() -> "Not a recognised theme: '"+String.valueOf(id) + "'");
		}, Theme::registryName);
		
		public boolean is(Theme b) { return b.registryName().equals(registryName); }
		
		public EncounterSet encounters() { return combatEncounters.isEmpty() ? BASIC_ENCOUNTERS : combatEncounters; }
		
		public List<TrapEntry> traps()
		{
			List<Identifier> names = trapEncounters.isEmpty() ? BASIC_TRAPS : trapEncounters;
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
				setId = BASIC_TILESETS.getOrDefault(name, CDTileSets.DEFAULT.registryName());
			
			return CDTileSets.instance().get(setId).orElse(CDTileSets.DEFAULT);
		}
		
		public TileSet passageTileSet()
		{
			return CDTileSets.instance().get(passageTiles).orElse(CDTileSets.DEFAULT);
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
	}
}
