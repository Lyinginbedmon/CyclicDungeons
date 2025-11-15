package com.lying.init;

import java.util.List;
import java.util.function.Function;

import com.google.common.collect.Lists;
import com.lying.CyclicDungeons;
import com.lying.block.RedstoneActorBlock;
import com.lying.block.RedstoneSensorBlock;
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
	 * * Proximity
	 * * Area
	 * * Line-of-sight
	 * * Sound
	 * Trap actors
	 * * Flamethrower
	 * * Spikes
	 * * Dart trap
	 * * Hatch
	 * * Entity spawner (mobs, potion clouds, etc.)
	 */
	
	// Primary logic block for managing trap functions
	public static final RegistrySupplier<Block> TRAP_LOGIC			= register("trap_logic", s -> new TrapLogicBlock(s.luminance(l->3).emissiveLighting(CDBlocks::always)));
	public static final RegistrySupplier<Block> TRAP_LOGIC_DECOY	= register("trap_logic_decoy", s -> new Block(s.luminance(l->3).emissiveLighting(CDBlocks::always)));
	
	// Sensors
	public static final RegistrySupplier<Block> SENSOR_REDSTONE		= register("redstone_sensor", RedstoneSensorBlock::new);
	
	// Actors
	public static final RegistrySupplier<Block> ACTOR_REDSTONE		= register("redstone_actor", RedstoneActorBlock::new);
	
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
	
	private static Boolean always(BlockState state, BlockView world, BlockPos pos, EntityType<?> type) { return true; }
	
	private static Boolean always(BlockState state, BlockView world, BlockPos pos) { return true; }
	
	private static Boolean never(BlockState state, BlockView world, BlockPos pos, EntityType<?> type) { return false; }
	
	private static Boolean never(BlockState state, BlockView world, BlockPos pos) { return false; }
	
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
