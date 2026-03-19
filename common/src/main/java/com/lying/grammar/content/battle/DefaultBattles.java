package com.lying.grammar.content.battle;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.lying.grammar.content.battle.SquadBattle.SquadEntry;
import com.lying.init.CDEntityTypes;

import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;

public class DefaultBattles
{
	private static final Map<Identifier, Supplier<BattleEntry>> BATTLES = new HashMap<>();
	
	public static final Identifier
		ID_PILLAGER_SQUAD	= prefix("pillager_squad"),
		ID_WOLF_PACK		= prefix("wolf_pack"),
		ID_ZOMBIE_CROWD		= prefix("zombie_crowd"),
		ID_SKELETONS		= prefix("skeletons"),
		ID_HUSK_CROWD		= prefix("husk_crowd"),
		ID_FIRE_TEAM		= prefix("fire_team"),
		ID_BOGGED			= prefix("bog_skeletons"),
		ID_COVEN			= prefix("coven");
	
	public static final Supplier<BattleEntry> WOLF_PACK		= register(ID_WOLF_PACK, () -> CrowdBattle.of(CDEntityTypes.RABID_WOLF.get(), 3, 4));
	public static final Supplier<BattleEntry> ZOMBIE_CROWD	= register(ID_ZOMBIE_CROWD, () -> CrowdBattle.of(EntityType.ZOMBIE, 4, 8));
	public static final Supplier<BattleEntry> SKELETONS		= register(ID_SKELETONS, () -> CrowdBattle.of(EntityType.SKELETON, 3, 5));
	public static final Supplier<BattleEntry> HUSK_CROWD	= register(ID_HUSK_CROWD, () -> CrowdBattle.of(EntityType.HUSK, 4, 8));
	public static final Supplier<BattleEntry> BOGGED		= register(ID_BOGGED, () -> CrowdBattle.of(EntityType.BOGGED, 3, 5));
	public static final Supplier<BattleEntry> COVEN			= register(ID_COVEN, () -> CrowdBattle.of(EntityType.WITCH, 2, 3));
	public static final Supplier<BattleEntry> FIRE_TEAM		= register(ID_FIRE_TEAM, () -> SquadBattle.create()
			.add(SquadEntry.Builder.of(EntityType.WITHER_SKELETON).build())
			.add(SquadEntry.Builder.of(EntityType.BLAZE).count(2, 3).build()));
	public static final Supplier<BattleEntry> PILLAGER_SQUAD	= register(ID_PILLAGER_SQUAD, () -> SquadBattle.create()
			.add(SquadEntry.Builder.of(EntityType.EVOKER).name("leader").count(0, 1).build())
			.add(SquadEntry.Builder.of(EntityType.VINDICATOR).name("elite").count(1, 2).build())
			.add(SquadEntry.Builder.of(EntityType.PILLAGER).count(2, 3).build()));
	
	private static Supplier<BattleEntry> register(final Identifier id, Supplier<Battle> func)
	{
		Supplier<BattleEntry> sup = () -> new BattleEntry(id, func.get());
		BATTLES.put(id, sup);
		return sup;
	}
	
	public static List<BattleEntry> getAll()
	{
		return BATTLES.values().stream().map(Supplier::get).toList();
	}
}
