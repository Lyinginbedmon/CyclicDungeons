package com.lying.init;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.lying.CyclicDungeons;
import com.lying.grammar.content.trap.ModularTrap;
import com.lying.grammar.content.trap.SatelliteStructurePlacerTrap;
import com.lying.grammar.content.trap.SimpleJumpingTrap;
import com.lying.grammar.content.trap.StructurePlacerTrap;
import com.lying.grammar.content.trap.TileSetTrap;
import com.lying.grammar.content.trap.TileToBlockTrap;
import com.lying.grammar.content.trap.TileTrap;
import com.lying.grammar.content.trap.Trap;

import net.minecraft.util.Identifier;

public class CDTrapTypes
{
	private static final Map<Identifier, Supplier<Trap>> TRAPS	= new HashMap<>();
	
	/*
	 * Chaser corridor trap - As corridor but traps fired regularly in overt sequence
	 * Warden trap - Spawners of thematic mobs triggered by sight sensor
	 * Corridor trap - Long room lined with darts/spikes and pressure plates
	 * Dart hail trap - Abundance of dart traps triggered by collision sensors on floor
	 * Spear corridor - Spike traps triggered in sequence by a clock
	 * Spear parkour - Spike traps on walls triggered in sequence by a clock, above pits
	 * Ceiling blade pendulums - Array of blade traps triggered in sequence by a clock
	 * Fusillade trap - Abundance of dart trips triggered in sequence by a clock
	 */
	
	// Configurable
	public static final Supplier<Trap> STRUCTURE_PLACER		= register(StructurePlacerTrap.ID, StructurePlacerTrap::new);
	public static final Supplier<Trap> ADJACENT_PLACER		= register(SatelliteStructurePlacerTrap.ID, SatelliteStructurePlacerTrap::new);
	public static final Supplier<Trap> SIMPLE_JUMPER		= register(SimpleJumpingTrap.ID, SimpleJumpingTrap::new);
	public static final Supplier<Trap> TILE_PREGEN			= register(TileTrap.ID, TileTrap::new);
	public static final Supplier<Trap> TILE_SET_PREGEN		= register(TileSetTrap.ID, TileSetTrap::new);
	public static final Supplier<Trap> TILE_TO_BLOCK		= register(TileToBlockTrap.ID, TileToBlockTrap::new);
	public static final Supplier<Trap> MODULAR				= register(ModularTrap.ID, ModularTrap::new);
	
	public static Supplier<Trap> register(Identifier name, Function<Identifier, Trap> func)
	{
		Supplier<Trap> supplier = () -> func.apply(name);
		TRAPS.put(name, supplier);
		return supplier;
	}
	
	public static Optional<Trap> get(Identifier name)
	{
		return TRAPS.containsKey(name) ? Optional.of(TRAPS.get(name).get()) : Optional.empty();
	}
	
	public static void init()
	{
		CyclicDungeons.LOGGER.info(" # Initialised {} trap types", TRAPS.size());
	}
}
