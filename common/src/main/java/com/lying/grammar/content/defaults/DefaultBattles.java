package com.lying.grammar.content.defaults;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.lying.grammar.content.entities.Battle;
import com.lying.grammar.content.entities.CrowdBattle;
import com.lying.grammar.content.entities.SpawnerEntry;
import com.lying.grammar.content.entities.SquadBattle;
import com.lying.grammar.content.entities.SquadBattle.SquadEntry;
import com.lying.init.CDEntityTypes;

import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;

public class DefaultBattles
{
	private static final Map<Identifier, Supplier<SpawnerEntry>> BATTLES = new HashMap<>();
	
	public static final Identifier
		ID_PILLAGER_SQUAD	= prefix("pillager_squad"),
		ID_WOLF_PACK		= prefix("wolf_pack"),
		ID_ZOMBIE_CROWD		= prefix("zombie_crowd"),
		ID_SKELETONS		= prefix("skeletons"),
		ID_HUSK_CROWD		= prefix("husk_crowd"),
		ID_FIRE_TEAM		= prefix("fire_team"),
		ID_BOGGED			= prefix("bog_skeletons"),
		ID_COVEN			= prefix("coven");
	
	public static final Supplier<SpawnerEntry> WOLF_PACK		= register(ID_WOLF_PACK, () -> CrowdBattle.of(CDEntityTypes.RABID_WOLF.get(), 3, 4));
	public static final Supplier<SpawnerEntry> ZOMBIE_CROWD	= register(ID_ZOMBIE_CROWD, () -> CrowdBattle.of(EntityType.ZOMBIE, 4, 8));
	public static final Supplier<SpawnerEntry> SKELETONS		= register(ID_SKELETONS, () -> CrowdBattle.of(EntityType.SKELETON, 3, 5));
	public static final Supplier<SpawnerEntry> HUSK_CROWD	= register(ID_HUSK_CROWD, () -> CrowdBattle.of(EntityType.HUSK, 4, 8));
	public static final Supplier<SpawnerEntry> BOGGED		= register(ID_BOGGED, () -> CrowdBattle.of(EntityType.BOGGED, 3, 5));
	public static final Supplier<SpawnerEntry> COVEN			= register(ID_COVEN, () -> CrowdBattle.of(EntityType.WITCH, 2, 3));
	public static final Supplier<SpawnerEntry> FIRE_TEAM		= register(ID_FIRE_TEAM, () -> SquadBattle.create()
			.add(SquadEntry.Builder.of(EntityType.WITHER_SKELETON).build())
			.add(SquadEntry.Builder.of(EntityType.BLAZE).count(2, 3).build()));
	public static final Supplier<SpawnerEntry> PILLAGER_SQUAD	= register(ID_PILLAGER_SQUAD, () -> SquadBattle.create()
			.add(SquadEntry.Builder.of(EntityType.EVOKER).name("leader").count(0, 1).build())
			.add(SquadEntry.Builder.of(EntityType.VINDICATOR).name("elite").count(1, 2).build())
			.add(SquadEntry.Builder.of(EntityType.PILLAGER).count(2, 3).build()));
	
	private static Supplier<SpawnerEntry> register(final Identifier id, Supplier<Battle> func)
	{
		Supplier<SpawnerEntry> sup = () -> new SpawnerEntry(id, func.get());
		BATTLES.put(id, sup);
		return sup;
	}
	
	public static List<SpawnerEntry> getAll()
	{
		return BATTLES.values().stream().map(Supplier::get).toList();
	}
}
