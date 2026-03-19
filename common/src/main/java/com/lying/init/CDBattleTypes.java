package com.lying.init;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.lying.CyclicDungeons;
import com.lying.grammar.content.battle.Battle;
import com.lying.grammar.content.battle.CrowdBattle;
import com.lying.grammar.content.battle.SquadBattle;

import net.minecraft.util.Identifier;

public class CDBattleTypes
{
	private static final Map<Identifier, Supplier<Battle>> BATTLES	= new HashMap<>();
	
	public static final Supplier<Battle> CROWD	= register(CrowdBattle.ID, CrowdBattle::new);
	public static final Supplier<Battle> BASIC_SQUAD	= register(SquadBattle.ID, SquadBattle::new);
	
	public static Supplier<Battle> register(Identifier name, Function<Identifier, Battle> func)
	{
		Supplier<Battle> supplier = () -> func.apply(name);
		BATTLES.put(name, supplier);
		return supplier;
	}
	
	public static Optional<Battle> get(Identifier name)
	{
		return BATTLES.containsKey(name) ? Optional.of(BATTLES.get(name).get()) : Optional.empty();
	}
	
	public static void init()
	{
		CyclicDungeons.LOGGER.info(" # Initialised {} encounter types", BATTLES.size());
	}
}
