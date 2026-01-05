package com.lying.worldgen.theme;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.google.common.collect.Lists;
import com.lying.blueprint.processor.BattleRoomProcessor;
import com.lying.blueprint.processor.BattleRoomProcessor.EncounterSet;
import com.lying.blueprint.processor.battle.SquadBattleEntry.SquadEntry;

import net.minecraft.entity.EntityType;
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
			new EncounterSet(), 
			List.of(),
			Map.of());
	public static final Supplier<Theme> DESERT	= register(ID_DESERT, 
			new EncounterSet()
				.addCrowd(prefix("husk_crowd"), EntityType.HUSK, 4, 8)
				.addSquad(prefix("fire_team"), BattleRoomProcessor.BASIC_SQUAD.get()
					.add(SquadEntry.Builder.of(EntityType.WITHER_SKELETON).build())
					.add(SquadEntry.Builder.of(EntityType.BLAZE).count(2, 3).build()))
				.addEntry(Theme.ENCOUNTER_PILLAGER_SQUAD)
				.addEntry(Theme.ENCOUNTER_WOLF_PACK), 
			List.of(),
			Map.of());
	public static final Supplier<Theme> UNDEAD	= register(ID_UNDEAD, 
			new EncounterSet()
				.addEntry(Theme.ENCOUNTER_SKELETONS)
				.addEntry(Theme.ENCOUNTER_ZOMBIE_CROWD), 
			List.of(),
			Map.of());
	public static final Supplier<Theme> JUNGLE	= register(ID_JUNGLE, 
			new EncounterSet(), 
			List.of(),
			Map.of());
	public static final Supplier<Theme> SWAMP	= register(ID_SWAMP, 
			new EncounterSet()
				.addCrowd(prefix("skeletons"), EntityType.BOGGED, 3, 5)
				.addCrowd(prefix("coven"), EntityType.WITCH, 2, 3)
				.addEntry(Theme.ENCOUNTER_ZOMBIE_CROWD)
				.addEntry(Theme.ENCOUNTER_PILLAGER_SQUAD), 
			List.of(),
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
