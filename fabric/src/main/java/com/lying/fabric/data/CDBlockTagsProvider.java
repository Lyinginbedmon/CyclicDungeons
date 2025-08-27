package com.lying.fabric.data;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider.BlockTagProvider;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;

public class CDBlockTagsProvider extends BlockTagProvider
{
	public CDBlockTagsProvider(FabricDataOutput output, CompletableFuture<WrapperLookup> registriesFuture)
	{
		super(output, registriesFuture);
	}
	
	protected void configure(WrapperLookup wrapperLookup)
	{
		
	}
}
