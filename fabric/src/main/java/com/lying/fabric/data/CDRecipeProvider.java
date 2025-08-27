package com.lying.fabric.data;
import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.recipe.RecipeExporter;
import net.minecraft.data.recipe.RecipeGenerator;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;

public class CDRecipeProvider extends FabricRecipeProvider
{
	public static final int PILE_TO_LEAF_CONVERSION_RATE = 8;
	
	public CDRecipeProvider(FabricDataOutput output, CompletableFuture<WrapperLookup> lookup)
	{
		super(output, lookup);
	}
	
	public String getName() { return "Cyclic Dungeons recipes"; }
	
	protected RecipeGenerator getRecipeGenerator(RegistryWrapper.WrapperLookup wrapperLookup, RecipeExporter exporter)
	{
		return new RecipeGenerator(wrapperLookup, exporter)
				{
					public void generate()
					{
						
					}
				};
	}
}