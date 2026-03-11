package com.lying.fabric.data;

import java.util.concurrent.CompletableFuture;

import com.lying.data.CDTags;
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
		getOrCreateTagBuilder(CDTags.TRAP_HATCHES).add(
				CDBlocks.COBBLESTONE_HATCH.get(),
				CDBlocks.DIRT_HATCH.get(),
				CDBlocks.GRASS_HATCH.get(),
				CDBlocks.MOSSY_COBBLESTONE_HATCH.get(),
				CDBlocks.RED_SANDSTONE_HATCH.get(),
				CDBlocks.SANDSTONE_HATCH.get(),
				CDBlocks.STONE_BRICK_HATCH.get(),
				CDBlocks.STONE_HATCH.get()
				);
		getOrCreateTagBuilder(CDTags.TRAP_SENSOR).add(
				CDBlocks.SENSOR_COLLISION.get(),
				CDBlocks.SENSOR_PROXIMITY.get(),
				CDBlocks.SENSOR_REDSTONE.get(),
				CDBlocks.SENSOR_SIGHT.get(),
				CDBlocks.SENSOR_SOUND.get());
		getOrCreateTagBuilder(CDTags.TRAP_ACTOR).add(
				CDBlocks.ACTOR_REDSTONE.get(),
				CDBlocks.DART_TRAP.get(),
				CDBlocks.FLAME_JET.get(),
				CDBlocks.SPIKE_TRAP.get(),
				CDBlocks.SPAWNER.get(),
				CDBlocks.SWINGING_BLADE.get()
				).addTag(CDTags.TRAP_HATCHES);
	}
}
