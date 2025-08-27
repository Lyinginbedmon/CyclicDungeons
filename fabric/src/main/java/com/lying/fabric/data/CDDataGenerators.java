package com.lying.fabric.data;

import com.lying.fabric.client.CDModelProvider;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

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
}
