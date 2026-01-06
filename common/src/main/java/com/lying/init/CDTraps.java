package com.lying.init;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.lying.CyclicDungeons;
import com.lying.grammar.content.TrapRoomContent.TrapEntry;
import com.lying.grammar.content.trap.LavaRiverTrapEntry;
import com.lying.grammar.content.trap.PitfallTrapEntry;

import net.minecraft.util.Identifier;

public class CDTraps
{
	private static final Map<Identifier, Supplier<TrapEntry>> TRAPS	= new HashMap<>();
	
	public static final Identifier 
		ID_SIMPLE_PITFALL	= prefix("simple_pitfall"),
		ID_LAVA_RIVER		= prefix("lava_river");
	
	public static final Supplier<TrapEntry> SIMPLE_PITFALL	= register(ID_SIMPLE_PITFALL, PitfallTrapEntry::new);
	public static final Supplier<TrapEntry> LAVA_RIVER		= register(ID_LAVA_RIVER, LavaRiverTrapEntry::new);
	
	public static Supplier<TrapEntry> register(Identifier name, Function<Identifier, TrapEntry> func)
	{
		Supplier<TrapEntry> supplier = () -> func.apply(name);
		TRAPS.put(name, supplier);
		return supplier;
	}
	
	public static Optional<TrapEntry> get(Identifier name)
	{
		return TRAPS.containsKey(name) ? Optional.of(TRAPS.get(name).get()) : Optional.empty();
	}
	
	public static void init()
	{
		CyclicDungeons.LOGGER.info(" # Initialised {} traps", TRAPS.size());
	}
}
