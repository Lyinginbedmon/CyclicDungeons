package com.lying.init;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.Set;

import com.lying.CyclicDungeons;
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
	
	public static final RegistrySupplier<BlockEntityType<TrapLogicBlockEntity>> TRAP_LOGIC	= register("trap_logic", TrapLogicBlockEntity::new, CDBlocks.TRAP_LOGIC.get());
	
	private static <T extends BlockEntity> RegistrySupplier<BlockEntityType<T>> register(String nameIn, BlockEntityType.BlockEntityFactory<? extends T> factory, Block... blocksIn)
	{
		tally++;
		return TYPES.register(prefix(nameIn), () -> new BlockEntityType<T>(factory, Set.of(blocksIn)));
	}
	
	private static RegistryKey<BlockEntityType<?>> keyOf(String nameIn) { return RegistryKey.of(RegistryKeys.BLOCK_ENTITY_TYPE, prefix(nameIn)); }
	
	public static void init()
	{
		TYPES.register();
		
		CyclicDungeons.LOGGER.info("# Initialised {} block entity types", tally);
	}
}
