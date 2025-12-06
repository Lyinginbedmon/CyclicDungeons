package com.lying.init;

import java.util.List;
import java.util.function.Function;

import com.google.common.collect.Lists;
import com.lying.CyclicDungeons;
import com.lying.block.BladeBlock;
import com.lying.block.CollisionSensorBlock;
import com.lying.block.FlameJetBlock;
import com.lying.block.HatchBlock;
import com.lying.block.PitBlock;
import com.lying.block.ProximitySensorBlock;
import com.lying.block.RedstoneActorBlock;
import com.lying.block.RedstoneSensorBlock;
import com.lying.block.SightSensorBlock;
import com.lying.block.SoundSensorBlock;
import com.lying.block.SwingingBladeBlock;
import com.lying.block.TrapLogicBlock;
import com.lying.reference.Reference;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

@SuppressWarnings("unused")
public class CDBlocks
{
	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Reference.ModInfo.MOD_ID, RegistryKeys.BLOCK);
	
	public static final List<RegistrySupplier<Block>> ALL_BLOCKS = Lists.newArrayList(), SOLID_CUBES = Lists.newArrayList();
	
	/*
	 * World Guard
	 * 
	 * Trap sensors
	 * * Area
	 * Trap actors
	 * * Spikes
	 * * Dart trap
	 * * Entity spawner (mobs, potion clouds, etc.)
	 */
	
	// Primary logic block for managing complex trap functions
	public static final RegistrySupplier<Block> TRAP_LOGIC			= register("trap_logic", s -> new TrapLogicBlock(s.luminance(l->3).emissiveLighting(CDBlocks::always)));
	public static final RegistrySupplier<Block> TRAP_LOGIC_DECOY	= register("trap_logic_decoy", s -> new Block(s.luminance(l->3).emissiveLighting(CDBlocks::always)));
	
	// Sensors
	public static final RegistrySupplier<Block> SENSOR_COLLISION	= register("collision_sensor", CollisionSensorBlock::new);
	public static final RegistrySupplier<Block> SENSOR_PROXIMITY	= register("proximity_sensor", ProximitySensorBlock::new);
	public static final RegistrySupplier<Block> SENSOR_REDSTONE		= register("redstone_sensor", RedstoneSensorBlock::new);
	public static final RegistrySupplier<Block> SENSOR_SIGHT		= register("sight_sensor", SightSensorBlock::new);
	public static final RegistrySupplier<Block> SENSOR_SOUND		= register("sound_sensor", SoundSensorBlock::new);
	
	// Actors
	public static final RegistrySupplier<Block> PIT					= register("pit", PitBlock::new);
	public static final RegistrySupplier<Block> ACTOR_REDSTONE		= register("redstone_actor", RedstoneActorBlock::new);
	public static final RegistrySupplier<Block> COBBLESTONE_HATCH	= register("cobblestone_hatch", HatchBlock::new);
	public static final RegistrySupplier<Block> DIRT_HATCH			= register("dirt_hatch", HatchBlock::new);
	public static final RegistrySupplier<Block> GRASS_HATCH			= register("grass_hatch", HatchBlock::new);
	public static final RegistrySupplier<Block> STONE_HATCH			= register("stone_hatch", HatchBlock::new);
	public static final RegistrySupplier<Block> STONE_BRICK_HATCH	= register("stone_brick_hatch", HatchBlock::new);
	public static final RegistrySupplier<Block> SWINGING_BLADE		= register("swinging_blade", SwingingBladeBlock::new);
	public static final RegistrySupplier<Block> BLADE				= register("blade", BladeBlock::new);
	public static final RegistrySupplier<Block> FLAME_JET			= register("flame_jet", FlameJetBlock::new);
	
	private static RegistrySupplier<Block> registerSolidCube(String nameIn, Function<AbstractBlock.Settings, Block> supplierIn)
	{
		RegistrySupplier<Block> registry = register(nameIn, supplierIn);
		SOLID_CUBES.add(registry);
		return registry;
	}
	
	private static RegistrySupplier<Block> register(String nameIn, Function<AbstractBlock.Settings, Block> supplierIn)
	{
		Identifier id = Reference.ModInfo.prefix(nameIn);
		RegistryKey<Block> key = RegistryKey.of(RegistryKeys.BLOCK, id);
		AbstractBlock.Settings settings = AbstractBlock.Settings.create().registryKey(key);
		RegistrySupplier<Block> registry = BLOCKS.register(id, () -> supplierIn.apply(settings));
		ALL_BLOCKS.add(registry);
		return registry;
	}
	
	public static Boolean always(BlockState state, BlockView world, BlockPos pos, EntityType<?> type) { return true; }
	
	public static Boolean always(BlockState state, BlockView world, BlockPos pos) { return true; }
	
	public static Boolean never(BlockState state, BlockView world, BlockPos pos, EntityType<?> type) { return false; }
	
	public static Boolean never(BlockState state, BlockView world, BlockPos pos) { return false; }
	
	private static AbstractBlock.Settings copyLootTable(AbstractBlock.Settings settings, Block block, boolean copyTranslationKey)
	{
		settings.lootTable(block.getLootTableKey());
		if(copyTranslationKey)
			settings.overrideTranslationKey(block.getTranslationKey());
		return settings;
	}
	
	public static void init()
	{
		BLOCKS.register();
		CyclicDungeons.LOGGER.info("# Initialised {} blocks", ALL_BLOCKS.size());
	}
}
