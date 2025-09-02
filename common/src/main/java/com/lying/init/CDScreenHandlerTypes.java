package com.lying.init;

import com.lying.CyclicDungeons;
import com.lying.reference.Reference;
import com.lying.screen.DungeonScreenHandler;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;

public class CDScreenHandlerTypes
{
	private static final DeferredRegister<ScreenHandlerType<?>> HANDLERS = DeferredRegister.create(Reference.ModInfo.MOD_ID, RegistryKeys.SCREEN_HANDLER);
	private static int tally = 0;
	
	public static final RegistrySupplier<ScreenHandlerType<DungeonScreenHandler>> DUNGEON_LAYOUT_HANDLER	= register("dungeon_layout", new ScreenHandlerType<>((syncId, playerInventory) -> new DungeonScreenHandler(syncId), FeatureFlags.VANILLA_FEATURES));
	
	private static <T extends ScreenHandler> RegistrySupplier<ScreenHandlerType<T>> register(String nameIn, ScreenHandlerType<T> typeIn)
	{
		tally++;
		return HANDLERS.register(Reference.ModInfo.prefix(nameIn), () -> typeIn);
	}
	
	public static void init()
	{
		HANDLERS.register();
		
		CyclicDungeons.LOGGER.info("# Initialised {} screen handler types", tally);
	}
}
