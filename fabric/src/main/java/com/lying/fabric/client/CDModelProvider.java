package com.lying.fabric.client;

import java.util.Optional;

import com.lying.init.CDBlocks;
import com.lying.init.CDItems;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.block.Block;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.data.BlockStateModelGenerator;
import net.minecraft.client.data.BlockStateVariant;
import net.minecraft.client.data.BlockStateVariantMap;
import net.minecraft.client.data.ItemModelGenerator;
import net.minecraft.client.data.Model;
import net.minecraft.client.data.ModelIds;
import net.minecraft.client.data.Models;
import net.minecraft.client.data.TexturedModel;
import net.minecraft.client.data.VariantSettings;
import net.minecraft.client.data.VariantsBlockStateSupplier;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.Identifier;

@SuppressWarnings("unused")
public class CDModelProvider extends FabricModelProvider
{
	public CDModelProvider(FabricDataOutput output)
	{
		super(output);
	}
	
	public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator)
	{
		// Simple solid cubes
		CDBlocks.SOLID_CUBES.forEach(entry -> blockStateModelGenerator.registerSimpleCubeAll(entry.get()));
	}
	
	public void generateItemModels(ItemModelGenerator itemModelGenerator)
	{
		CDItems.BASIC_BLOCK_ITEMS.stream().map(e -> (BlockItem)e.get()).forEach(entry -> registerBlockModel(entry, itemModelGenerator));
	}
	
	private static void registerParentedItem(Block block, Identifier modelIn, BlockStateModelGenerator generator)
	{
		generator.registerParentedItemModel(block, modelIn);
	}
	
	private static void registerBlockModel(BlockItem item, ItemModelGenerator itemModelGenerator)
	{
		itemModelGenerator.register(item, makeBlockModel(item));
	}
	
	private static Model makeBlockModel(BlockItem item)
	{
		Block block = item.getBlock();
		Identifier reg = Registries.BLOCK.getId(block);
		Model model = new Model(Optional.of(Identifier.of(reg.getNamespace(), "block/"+reg.getPath())), Optional.empty());
		return model;
	}
	
	private static void registerStairs(Block stairs, Block full, BlockStateModelGenerator blockStateModelGenerator)
	{
		TexturedModel textured = TexturedModel.CUBE_ALL.get(full);
		Identifier innerModel = Models.INNER_STAIRS.upload(stairs, textured.getTextures(), blockStateModelGenerator.modelCollector);
		Identifier regularModel = Models.STAIRS.upload(stairs, textured.getTextures(), blockStateModelGenerator.modelCollector);
		Identifier outerModel = Models.OUTER_STAIRS.upload(stairs, textured.getTextures(), blockStateModelGenerator.modelCollector);
		blockStateModelGenerator.blockStateCollector.accept(BlockStateModelGenerator.createStairsBlockState(stairs, innerModel, regularModel, outerModel));
	}
	
	private static void registerSlab(Block slab, Block full, BlockStateModelGenerator blockStateModelGenerator)
	{
		TexturedModel textured = TexturedModel.CUBE_ALL.get(full);
		Identifier bottomModel = Models.SLAB.upload(slab, textured.getTextures(), blockStateModelGenerator.modelCollector);
		Identifier topModel = Models.SLAB_TOP.upload(slab, textured.getTextures(), blockStateModelGenerator.modelCollector);
		Identifier fullModel = ModelIds.getBlockModelId(full);
		BlockStateVariantMap map = BlockStateVariantMap.create(SlabBlock.TYPE)
				.register(SlabType.BOTTOM, BlockStateVariant.create().put(VariantSettings.MODEL, bottomModel))
				.register(SlabType.TOP, BlockStateVariant.create().put(VariantSettings.MODEL, topModel))
				.register(SlabType.DOUBLE, BlockStateVariant.create().put(VariantSettings.MODEL, fullModel));
		blockStateModelGenerator.blockStateCollector.accept(VariantsBlockStateSupplier.create(slab).coordinate(map));
	}
	
	private static BlockStateVariantMap createBooleanModelMap(BooleanProperty property, Identifier trueModel, Identifier falseModel) {
		return BlockStateVariantMap.create(property)
			.register(true, BlockStateVariant.create().put(VariantSettings.MODEL, trueModel))
			.register(false, BlockStateVariant.create().put(VariantSettings.MODEL, falseModel));
	}
}
