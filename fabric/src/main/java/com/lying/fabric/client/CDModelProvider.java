package com.lying.fabric.client;

import static net.minecraft.client.data.BlockStateModelGenerator.createSingletonBlockState;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.lying.init.CDBlocks;
import com.lying.init.CDItems;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.block.Block;
import net.minecraft.client.data.BlockStateModelGenerator;
import net.minecraft.client.data.BlockStateVariant;
import net.minecraft.client.data.BlockStateVariantMap;
import net.minecraft.client.data.ItemModelGenerator;
import net.minecraft.client.data.Model;
import net.minecraft.client.data.ModelSupplier;
import net.minecraft.client.data.Models;
import net.minecraft.client.data.TextureKey;
import net.minecraft.client.data.TextureMap;
import net.minecraft.client.data.VariantSettings;
import net.minecraft.client.data.VariantsBlockStateSupplier;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;

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
		
		registerUnrotatedPillar(CDBlocks.TRAP_LOGIC.get(), blockStateModelGenerator);
		blockStateModelGenerator.registerParented(CDBlocks.TRAP_LOGIC.get(), CDBlocks.TRAP_LOGIC_DECOY.get());
		
		registerTrapBlockStates(blockStateModelGenerator);
	}
	
	public void generateItemModels(ItemModelGenerator itemModelGenerator)
	{
		// Block items
		CDItems.BASIC_BLOCK_ITEMS.stream().map(e -> (BlockItem)e.get()).forEach(entry -> registerBlockModel(entry, itemModelGenerator));
		
		registerTrapItemModels(itemModelGenerator);
	}
	
	private void registerTrapBlockStates(BlockStateModelGenerator generator)
	{
		for(Block block : List.of(
				CDBlocks.SENSOR_REDSTONE, 
				CDBlocks.ACTOR_REDSTONE
				).stream().map(Supplier::get).toList())
			registerPowerablePillar(block, generator);
	}
	
	private void registerTrapItemModels(ItemModelGenerator itemModelGenerator)
	{
		for(Item block : List.of(
				CDItems.SENSOR_REDSTONE, 
				CDItems.ACTOR_REDSTONE
				).stream().map(Supplier::get).toList())
			registerBlockModel((BlockItem)block, itemModelGenerator);
	}
	
	public void registerUnrotatedPillar(Block block, BlockStateModelGenerator generator)
	{
		TextureMap map = TextureMap.sideEnd(block);
		map.put(TextureKey.PARTICLE, TextureMap.getSubId(block, "_side"));
		generator.blockStateCollector.accept(createSingletonBlockState(block, Models.CUBE_COLUMN.upload(block, map, generator.modelCollector)));
	}
	
	private void registerPowerablePillar(Block block, BlockStateModelGenerator generator)
	{
		final Function<Identifier,TextureMap> texMapFunc = id -> 
		{
			TextureMap map = new TextureMap();
			Identifier topTex = TextureMap.getSubId(CDBlocks.TRAP_LOGIC.get(), "_top");
			map.put(TextureKey.END, topTex);
			map.put(TextureKey.PARTICLE, topTex);
			map.put(TextureKey.SIDE, id);
			return map;
		};
		
		generator.blockStateCollector.accept(VariantsBlockStateSupplier.create(block).coordinate(createBooleanModelMap(Properties.POWERED, 
				createSubModel(block, "_on", Models.CUBE_COLUMN, texMapFunc, generator.modelCollector), 
				Models.CUBE_COLUMN.upload(block, texMapFunc.apply(TextureMap.getId(block)), generator.modelCollector))));
		
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
	
	private Identifier createSubModel(Block block, String suffix, Model model, Function<Identifier, TextureMap> texturesFactory, BiConsumer<Identifier, ModelSupplier> modelCollector)
	{
		return model.upload(block, suffix, (TextureMap)texturesFactory.apply(TextureMap.getSubId(block, suffix)), modelCollector);
	}
	
	private static BlockStateVariantMap createBooleanModelMap(BooleanProperty property, Identifier trueModel, Identifier falseModel)
	{
		return BlockStateVariantMap.create(property)
				.register(true, BlockStateVariant.create().put(VariantSettings.MODEL, trueModel))
				.register(false, BlockStateVariant.create().put(VariantSettings.MODEL, falseModel));
	}
}
