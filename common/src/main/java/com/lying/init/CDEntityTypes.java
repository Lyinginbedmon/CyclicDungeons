package com.lying.init;

import static com.lying.reference.Reference.ModInfo.prefix;

import com.lying.CyclicDungeons;
import com.lying.reference.Reference;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

@SuppressWarnings("unused")
public class CDEntityTypes
{
	private static final DeferredRegister<EntityType<?>> ENTITIES	= DeferredRegister.create(Reference.ModInfo.MOD_ID, RegistryKeys.ENTITY_TYPE);
	private static int tally = 0;
	
	private static <T extends Entity> RegistrySupplier<EntityType<T>> register(String nameIn, EntityType.Builder<T> entry)
	{
		tally++;
		return ENTITIES.register(prefix(nameIn), () -> entry.build(keyOf(nameIn)));
	}
	
	private static RegistryKey<EntityType<?>> keyOf(String nameIn) { return RegistryKey.of(RegistryKeys.ENTITY_TYPE, prefix(nameIn)); }
	
	public static void init()
	{
		ENTITIES.register();
		
		CyclicDungeons.LOGGER.info("# Initialised {} entity types", tally);
	}
}
