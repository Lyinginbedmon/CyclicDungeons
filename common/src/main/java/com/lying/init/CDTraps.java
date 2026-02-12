package com.lying.init;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.lying.CyclicDungeons;
import com.lying.grammar.content.TrapRoomContent.TrapEntry;
import com.lying.grammar.content.trap.BearTrapEntry;
import com.lying.grammar.content.trap.GreedTrapEntry;
import com.lying.grammar.content.trap.HatchPitfallTrapEntry;
import com.lying.grammar.content.trap.JumpingTrapEntry;
import com.lying.grammar.content.trap.LavaRiverTrapEntry;
import com.lying.grammar.content.trap.PitfallTrapEntry;
import com.lying.worldgen.tile.DefaultTiles;

import net.minecraft.util.Identifier;

public class CDTraps
{
	private static final Map<Identifier, Supplier<TrapEntry>> TRAPS	= new HashMap<>();
	
	public static final Identifier 
		ID_SIMPLE_PITFALL	= prefix("simple_pitfall"),
		ID_LAVA_RIVER		= prefix("lava_river"),
		ID_PITFALL			= prefix("pitfall"),
		ID_BEAR_TRAP		= prefix("bear_trap"),
		ID_GREED_TRAP		= prefix("greed_trap"),
		ID_JUMPING_TRAP_PIT		= prefix("jumping_trap_pit"),
		ID_JUMPING_TRAP_LAVA	= prefix("jumping_trap_lava");
	
	/*
	 * Corridor trap - Long room lined with darts/spikes and pressure plates
	 * Chaser corridor trap - As corridor but traps fired regularly in overt sequence
	 * Jumping trap - Majority of floor pre-seeded with pit/lava/spikes/etc
	 * Crumbling jumping trap - As jumping but path through is crumbling blocks
	 * Dart hail trap - Abundance of dart traps triggered by collision sensors on floor
	 * Warden trap - Spawners of thematic mobs triggered by sight sensor
	 * "Bear" trap - Actuated spawner that creates angry polar bears, triggered by proximity sensors
	 * Landmine trap - Spawners of instant harming splash potions triggered by proximity sensors
	 * 
	 * Spear corridor - Spike traps triggered in sequence by a clock
	 * Spear parkour - Spike traps on walls triggered in sequence by a clock, above pits
	 * Ceiling blade pendulums - Array of blade traps triggered in sequence by a clock
	 * Fusillade trap - Abundance of dart trips triggered in sequence by a clock
	 */
	
	public static final Supplier<TrapEntry> SIMPLE_PITFALL	= register(ID_SIMPLE_PITFALL, HatchPitfallTrapEntry::new);
	public static final Supplier<TrapEntry> LAVA_RIVER		= register(ID_LAVA_RIVER, LavaRiverTrapEntry::new);
	public static final Supplier<TrapEntry> PITFALL			= register(ID_PITFALL, PitfallTrapEntry::new);
	public static final Supplier<TrapEntry> BEAR_TRAP		= register(ID_BEAR_TRAP, BearTrapEntry::new);
	public static final Supplier<TrapEntry> GREED_TRAP		= register(ID_GREED_TRAP, GreedTrapEntry::new);
	public static final Supplier<TrapEntry> JUMPING_TRAP_PIT	= register(ID_JUMPING_TRAP_PIT, id -> new JumpingTrapEntry(id, DefaultTiles.ID_PIT));
	public static final Supplier<TrapEntry> JUMPING_TRAP_LAVA	= register(ID_JUMPING_TRAP_LAVA, id -> new JumpingTrapEntry(id, DefaultTiles.ID_LAVA));
	
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
