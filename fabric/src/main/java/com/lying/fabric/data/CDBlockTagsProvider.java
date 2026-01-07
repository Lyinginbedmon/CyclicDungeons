package com.lying.fabric.data;

import java.util.concurrent.CompletableFuture;

import com.lying.init.CDBlocks;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider.BlockTagProvider;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.registry.tag.BlockTags;

public class CDBlockTagsProvider extends BlockTagProvider
{
	public CDBlockTagsProvider(FabricDataOutput output, CompletableFuture<WrapperLookup> registriesFuture)
	{
		super(output, registriesFuture);
	}
	
	protected void configure(WrapperLookup wrapperLookup)
	{
		getOrCreateTagBuilder(BlockTags.PICKAXE_MINEABLE).add(
				CDBlocks.CRUMBLING_COBBLESTONE.get(),
				CDBlocks.CRUMBLING_MOSSY_COBBLESTONE.get(),
				CDBlocks.CRUMBLING_RED_SANDSTONE.get(),
				CDBlocks.CRUMBLING_SANDSTONE.get(),
				CDBlocks.CRUMBLING_STONE.get(),
				CDBlocks.CRUMBLING_STONE_BRICKS.get(),
				CDBlocks.RESETTING_CRUMBLING_COBBLESTONE.get(),
				CDBlocks.RESETTING_CRUMBLING_MOSSY_COBBLESTONE.get(),
				CDBlocks.RESETTING_CRUMBLING_RED_SANDSTONE.get(),
				CDBlocks.RESETTING_CRUMBLING_SANDSTONE.get(),
				CDBlocks.RESETTING_CRUMBLING_STONE.get(),
				CDBlocks.RESETTING_CRUMBLING_STONE_BRICKS.get()
				);
	}
}
