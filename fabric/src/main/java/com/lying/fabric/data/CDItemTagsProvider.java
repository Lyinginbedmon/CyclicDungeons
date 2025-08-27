package com.lying.fabric.data;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider.ItemTagProvider;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;

public class CDItemTagsProvider extends ItemTagProvider
{
	public CDItemTagsProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> completableFuture)
	{
		super(output, completableFuture);
	}
	
	protected void configure(WrapperLookup wrapperLookup)
	{
		
	}
	
	
}
