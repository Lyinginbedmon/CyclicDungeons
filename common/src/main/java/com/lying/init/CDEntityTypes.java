package com.lying.init;

import static com.lying.reference.Reference.ModInfo.prefix;

import com.lying.CyclicDungeons;
import com.lying.entity.RabidWolfEntity;
import com.lying.reference.Reference;

import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.Vec3d;

public class CDEntityTypes
{
	private static final DeferredRegister<EntityType<?>> ENTITIES	= DeferredRegister.create(Reference.ModInfo.MOD_ID, RegistryKeys.ENTITY_TYPE);
	private static int tally = 0;
	
	public static final RegistrySupplier<EntityType<RabidWolfEntity>> RABID_WOLF	= register("rabid_wolf", EntityType.Builder.create(RabidWolfEntity::new, SpawnGroup.MONSTER)
			.dimensions(0.6F, 0.85F)
			.eyeHeight(0.68F)
			.passengerAttachments(new Vec3d(0, 0.81875, -0.0625))
			.maxTrackingRange(10));
	
	private static <T extends Entity> RegistrySupplier<EntityType<T>> register(String nameIn, EntityType.Builder<T> entry)
	{
		tally++;
		return ENTITIES.register(prefix(nameIn), () -> entry.build(keyOf(nameIn)));
	}
	
	private static RegistryKey<EntityType<?>> keyOf(String nameIn) { return RegistryKey.of(RegistryKeys.ENTITY_TYPE, prefix(nameIn)); }
	
	public static void init()
	{
		ENTITIES.register();
		
		EntityAttributeRegistry.register(RABID_WOLF, () -> WolfEntity.createWolfAttributes());
		
		CyclicDungeons.LOGGER.info(" # Initialised {} entity types", tally);
	}
}
