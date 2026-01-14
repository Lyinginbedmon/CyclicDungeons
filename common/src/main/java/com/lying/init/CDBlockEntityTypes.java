package com.lying.init;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.lying.CyclicDungeons;
import com.lying.block.entity.DartTrapBlockEntity;
import com.lying.block.entity.FlameJetBlockEntity;
import com.lying.block.entity.ProximitySensorBlockEntity;
import com.lying.block.entity.SightSensorBlockEntity;
import com.lying.block.entity.SoundSensorBlockEntity;
import com.lying.block.entity.SpikeTrapBlockEntity;
import com.lying.block.entity.SwingingBladeBlockEntity;
import com.lying.block.entity.TrapActorBlockEntity;
import com.lying.block.entity.TrapLogicBlockEntity;
import com.lying.reference.Reference;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

@SuppressWarnings("unused")
public class CDBlockEntityTypes
{
	private static final DeferredRegister<BlockEntityType<?>> TYPES	= DeferredRegister.create(Reference.ModInfo.MOD_ID, RegistryKeys.BLOCK_ENTITY_TYPE);
	private static int tally = 0;
	
	public static final RegistrySupplier<BlockEntityType<TrapLogicBlockEntity>> TRAP_LOGIC			= register("trap_logic", TrapLogicBlockEntity::new, CDBlocks.TRAP_LOGIC);
	public static final RegistrySupplier<BlockEntityType<SoundSensorBlockEntity>> SOUND_SENSOR		= register("sound_sensor", SoundSensorBlockEntity::new, CDBlocks.SENSOR_SOUND);
	public static final RegistrySupplier<BlockEntityType<SightSensorBlockEntity>> SIGHT_SENSOR		= register("sight_sensor", SightSensorBlockEntity::new, CDBlocks.SENSOR_SIGHT);
	public static final RegistrySupplier<BlockEntityType<ProximitySensorBlockEntity>> PROXIMITY_SENSOR	= register("proximity_sensor", ProximitySensorBlockEntity::new, CDBlocks.SENSOR_PROXIMITY);
	public static final RegistrySupplier<BlockEntityType<TrapActorBlockEntity>> TRAP_ACTOR			= register("trap_actor", TrapActorBlockEntity::new, 
			CDBlocks.ACTOR_REDSTONE, 
			CDBlocks.STONE_BRICK_HATCH,
			CDBlocks.STONE_HATCH,
			CDBlocks.COBBLESTONE_HATCH,
			CDBlocks.MOSSY_COBBLESTONE_HATCH,
			CDBlocks.GRASS_HATCH,
			CDBlocks.DIRT_HATCH,
			CDBlocks.SANDSTONE_HATCH,
			CDBlocks.RED_SANDSTONE_HATCH);
	public static final RegistrySupplier<BlockEntityType<SwingingBladeBlockEntity>> SWINGING_BLADE	= register("swinging_blade", SwingingBladeBlockEntity::new, CDBlocks.SWINGING_BLADE);
	public static final RegistrySupplier<BlockEntityType<FlameJetBlockEntity>> FLAME_JET			= register("flame_jet", FlameJetBlockEntity::new, CDBlocks.FLAME_JET);
	public static final RegistrySupplier<BlockEntityType<DartTrapBlockEntity>> DART_TRAP			= register("dart_trap", DartTrapBlockEntity::new, CDBlocks.DART_TRAP);
	public static final RegistrySupplier<BlockEntityType<SpikeTrapBlockEntity>> SPIKE_TRAP			= register("spike_trap", SpikeTrapBlockEntity::new, CDBlocks.SPIKE_TRAP);
	
	@SafeVarargs
	private static <T extends BlockEntity> RegistrySupplier<BlockEntityType<T>> register(String nameIn, BlockEntityType.BlockEntityFactory<? extends T> factory, RegistrySupplier<Block>... blocksIn)
	{
		tally++;
		return TYPES.register(prefix(nameIn), () -> new BlockEntityType<T>(factory, Set.of(List.of(blocksIn).stream().map(Supplier::get).toList().toArray(new Block[0]))));
	}
	
	private static RegistryKey<BlockEntityType<?>> keyOf(String nameIn) { return RegistryKey.of(RegistryKeys.BLOCK_ENTITY_TYPE, prefix(nameIn)); }
	
	public static void init()
	{
		TYPES.register();
		
		CyclicDungeons.LOGGER.info(" # Initialised {} block entity types", tally);
	}
}
