package com.lying.init;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.lying.CyclicDungeons;
import com.lying.blueprint.processor.BattleRoomProcessor.BattleEntry;
import com.lying.blueprint.processor.TrapRoomProcessor.TrapEntry;
import com.lying.blueprint.processor.battle.SimpleBattleEntry;
import com.lying.blueprint.processor.battle.SquadBattleEntry;
import com.lying.blueprint.processor.battle.SquadBattleEntry.SquadEntry;
import com.lying.blueprint.processor.trap.LavaRiverTrapEntry;
import com.lying.blueprint.processor.trap.PitfallTrapEntry;
import com.lying.reference.Reference;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;

public class CDThemes
{
	private static final Map<Identifier, Supplier<Theme>> THEMES = new HashMap<>();
	
	public static final Supplier<Theme> BASIC	= register("basic", List.of(), List.of());
	public static final Supplier<Theme> DESERT	= register("desert", List.of(
			new SimpleBattleEntry<>(prefix("husk_crowd"), EntityType.HUSK, 4, 8),
			new SquadBattleEntry(prefix("fire_team"))
				.add(SquadEntry.Builder.of(EntityType.WITHER_SKELETON).build())
				.add(SquadEntry.Builder.of(EntityType.BLAZE).count(2, 3).build()),
			new SquadBattleEntry(prefix("pillager_squad"))
				.add(SquadEntry.Builder.of(EntityType.EVOKER).name("leader").count(0, 1).build())
				.add(SquadEntry.Builder.of(EntityType.VINDICATOR).name("elite").count(1, 2).build())
				.add(SquadEntry.Builder.of(EntityType.PILLAGER).count(2, 3).build()),
			new SquadBattleEntry(prefix("wolf_pack"))
				.add(SquadEntry.Builder.of(CDEntityTypes.RABID_WOLF.get()).count(3, 4).build())
			), List.of());
	public static final Supplier<Theme> UNDEAD	= register("undead", List.of(), List.of());
	public static final Supplier<Theme> JUNGLE	= register("jungle", List.of(), List.of());
	public static final Supplier<Theme> SWAMP	= register("swamp", List.of(
			new SimpleBattleEntry<>(prefix("zombie_crowd"), EntityType.ZOMBIE, 4, 8),
			new SimpleBattleEntry<>(prefix("skeletons"), EntityType.BOGGED, 3, 5),
			new SimpleBattleEntry<>(prefix("coven"), EntityType.WITCH, 2, 3),
			new SquadBattleEntry(prefix("pillager_squad"))
				.add(SquadEntry.Builder.of(EntityType.EVOKER).name("leader").count(0, 1).build())
				.add(SquadEntry.Builder.of(EntityType.VINDICATOR).name("elite").count(1, 2).build())
				.add(SquadEntry.Builder.of(EntityType.PILLAGER).count(2, 3).build())
			), List.of());
	
	private static Supplier<Theme> register(String nameIn, List<BattleEntry> combat, List<TrapEntry> traps)
	{
		final Identifier id = Reference.ModInfo.prefix(nameIn); 
		final Supplier<Theme> entry = () -> new Theme(id, combat, traps);
		THEMES.put(id, entry);
		return entry;
	}
	
	public static Optional<Theme> get(Identifier id)
	{
		return THEMES.containsKey(id) ? Optional.of(THEMES.get(id).get()) : Optional.empty();
	}
	
	public static void init()
	{
		CyclicDungeons.LOGGER.info("# Initialised {} themes", THEMES.size());
	}
	
	public static record Theme(Identifier registryName, List<BattleEntry> combatEncounters, List<TrapEntry> trapEncounters)
	{
		public static final Codec<Theme> CODEC = Identifier.CODEC.comapFlatMap(id -> 
		{
			Optional<Theme> type = CDThemes.get(id);
			if(type.isPresent())
				return DataResult.success(type.get());
			else
				return DataResult.error(() -> "Not a recognised theme: '"+String.valueOf(id) + "'");
		}, Theme::registryName);
		
		private static final List<BattleEntry> BASIC_ENCOUNTERS = List.of(
				new SimpleBattleEntry<>(prefix("zombie_crowd"), EntityType.ZOMBIE, 4, 8),
				new SimpleBattleEntry<>(prefix("skeletons"), EntityType.SKELETON, 3, 5),
				new SquadBattleEntry(prefix("pillager_squad"))
					.add(SquadEntry.Builder.of(EntityType.EVOKER).name("leader").count(0, 1).build())
					.add(SquadEntry.Builder.of(EntityType.VINDICATOR).name("elite").count(1, 2).build())
					.add(SquadEntry.Builder.of(EntityType.PILLAGER).count(2, 3).build()),
				new SquadBattleEntry(prefix("wolf_pack"))
					.add(SquadEntry.Builder.of(CDEntityTypes.RABID_WOLF.get()).count(3, 4).build())
					);
		
		private static final List<TrapEntry> BASIC_TRAPS	= List.of(
				new PitfallTrapEntry(prefix("simple_pitfall")),
				new LavaRiverTrapEntry(prefix("lava_river"))
				);
		
		public boolean is(Theme b) { return b.registryName().equals(registryName); }
		
		public List<BattleEntry> encounters() { return combatEncounters.isEmpty() ? BASIC_ENCOUNTERS : combatEncounters; }
		
		public List<TrapEntry> traps() { return trapEncounters.isEmpty() ? BASIC_TRAPS : trapEncounters; }
	}
}
