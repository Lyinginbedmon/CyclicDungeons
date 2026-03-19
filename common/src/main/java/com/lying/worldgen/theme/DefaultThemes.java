package com.lying.worldgen.theme;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.google.common.collect.Lists;
import com.lying.grammar.DefaultTerms;
import com.lying.grammar.content.BattleRoomContent.EncounterSet;
import com.lying.grammar.content.battle.DefaultBattles;
import com.lying.grammar.content.trap.DefaultTraps;
import com.lying.init.CDTerms;
import com.lying.worldgen.tileset.DefaultTileSets;

import net.minecraft.util.Identifier;

public class DefaultThemes
{
	private static final List<Supplier<Theme>> THEMES = Lists.newArrayList();
	
	public static final Identifier
		ID_GENERIC	= prefix("generic"),
		ID_DESERT	= prefix("desert"),
		ID_UNDEAD	= prefix("undead"),
		ID_JUNGLE	= prefix("jungle"),
		ID_SWAMP	= prefix("swamp");
	
	public static final Supplier<Theme> GENERIC	= register(ID_GENERIC, 
			new EncounterSet(List.of(
					DefaultBattles.ID_PILLAGER_SQUAD,
					DefaultBattles.ID_WOLF_PACK,
					DefaultBattles.ID_ZOMBIE_CROWD,
					DefaultBattles.ID_SKELETONS
					)), 
			Lists.newArrayList(
					DefaultTraps.ID_PITFALL,
					DefaultTraps.ID_LAVA_RIVER,
					DefaultTraps.ID_PIT_JUMPING,
					DefaultTraps.ID_LAVA_JUMPING,
					DefaultTraps.ID_MINEFIELD,
					DefaultTraps.ID_BEARTRAPS,
					DefaultTraps.ID_BEAR_TRAPS,
					DefaultTraps.ID_HATCH_PITFALL,
					DefaultTraps.ID_MODULE_TEST
					),
			Map.of(
					CDTerms.ID_START, DefaultTileSets.ID_START,
					CDTerms.ID_END, DefaultTileSets.ID_END,
					CDTerms.ID_EMPTY, DefaultTileSets.ID_EMPTY,
					DefaultTerms.ID_BATTLE, DefaultTileSets.ID_BATTLE,
					DefaultTerms.ID_TRAP, DefaultTileSets.ID_TRAP,
					DefaultTerms.ID_BIG_PUZZLE, DefaultTileSets.ID_PUZZLE,
					DefaultTerms.ID_SML_PUZZLE, DefaultTileSets.ID_PUZZLE,
					DefaultTerms.ID_BOSS, DefaultTileSets.ID_BOSS,
					CDTerms.ID_TREASURE, DefaultTileSets.ID_TREASURE
					));
	public static final Supplier<Theme> DESERT	= register(ID_DESERT, 
			new EncounterSet(List.of(
				DefaultBattles.ID_HUSK_CROWD, 
				DefaultBattles.ID_FIRE_TEAM, 
				DefaultBattles.ID_PILLAGER_SQUAD, 
				DefaultBattles.ID_WOLF_PACK)), 
			List.of(
				DefaultTraps.ID_LAVA_JUMPING,
				DefaultTraps.ID_LAVA_RIVER
					),
			Map.of());
	public static final Supplier<Theme> UNDEAD	= register(ID_UNDEAD, 
			new EncounterSet(List.of(
				DefaultBattles.ID_SKELETONS, 
				DefaultBattles.ID_ZOMBIE_CROWD)), 
			List.of(),
			Map.of());
	public static final Supplier<Theme> JUNGLE	= register(ID_JUNGLE, 
			new EncounterSet(), 
			List.of(),
			Map.of());
	public static final Supplier<Theme> SWAMP	= register(ID_SWAMP, 
			new EncounterSet(List.of(
				DefaultBattles.ID_BOGGED, 
				DefaultBattles.ID_COVEN, 
				DefaultBattles.ID_ZOMBIE_CROWD, 
				DefaultBattles.ID_PILLAGER_SQUAD)), 
			List.of(
				DefaultTraps.ID_BEARTRAPS,
				DefaultTraps.ID_MINEFIELD
					),
			Map.of());
	
	private static Supplier<Theme> register(Identifier id, EncounterSet combat, List<Identifier> traps, Map<Identifier, Identifier> tileSets)
	{
		return register(id, combat, traps, tileSets, Optional.empty());
	}
	
	private static Supplier<Theme> register(Identifier id, EncounterSet combat, List<Identifier> traps, Map<Identifier, Identifier> tileSets, Optional<Identifier> passageTileSet)
	{
		final Supplier<Theme> entry = () -> new Theme(id, combat, traps, tileSets, passageTileSet);
		THEMES.add(entry);
		return entry;
	}
	
	public static List<Theme> getDefaults() { return THEMES.stream().map(Supplier::get).toList(); }
}
