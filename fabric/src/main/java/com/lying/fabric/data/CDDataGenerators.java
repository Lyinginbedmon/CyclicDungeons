package com.lying.fabric.data;

import com.lying.data.CDTemplatePoolProvider;
import com.lying.fabric.client.CDModelProvider;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.registry.RegistryBuilder;
import net.minecraft.registry.RegistryKeys;

public class CDDataGenerators implements DataGeneratorEntrypoint
{
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator)
	{
		FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
		pack.addProvider(CDModelProvider::new);
		pack.addProvider(CDBlockTagsProvider::new);
		pack.addProvider(CDItemTagsProvider::new);
		pack.addProvider(CDGameEventTagsProvider::new);
		pack.addProvider(CDBlockLootTableProvider::new);
		pack.addProvider(CDRecipeProvider::new);
	}
	
	public void buildRegistry(RegistryBuilder registryBuilder)
	{
		registryBuilder.addRegistry(RegistryKeys.TEMPLATE_POOL, CDTemplatePoolProvider::new);
	}
}
