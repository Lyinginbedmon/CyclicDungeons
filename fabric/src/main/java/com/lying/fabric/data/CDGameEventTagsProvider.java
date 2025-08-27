package com.lying.fabric.data;

import java.util.concurrent.CompletableFuture;

import dev.architectury.registry.registries.RegistrySupplier;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.tag.TagProvider;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.world.event.GameEvent;

@SuppressWarnings("unused")
public class CDGameEventTagsProvider extends TagProvider<GameEvent>
{
	public CDGameEventTagsProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> completableFuture)
	{
		super(output, RegistryKeys.GAME_EVENT, completableFuture);
	}
	
	protected void configure(WrapperLookup art)
	{
		
	}
	
	@SuppressWarnings("unchecked")
	private void register(TagKey<GameEvent> tagIn, RegistrySupplier<GameEvent>... events)
	{
		ProvidedTagBuilder<GameEvent> tag = getOrCreateTagBuilder(tagIn);
		for(RegistrySupplier<GameEvent> event : events)
			tag.add(((RegistryEntry<GameEvent>)event).getKey().get());
	}
}
