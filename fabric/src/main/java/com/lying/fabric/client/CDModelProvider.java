package com.lying.fabric.client;

import static com.lying.reference.Reference.ModInfo.prefix;
import static net.minecraft.client.data.BlockStateModelGenerator.createSingletonBlockState;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.lying.block.CollisionSensorBlock;
import com.lying.block.ProximitySensorBlock;
import com.lying.block.SightSensorBlock;
import com.lying.block.SoundSensorBlock;
import com.lying.init.CDBlocks;
import com.lying.init.CDItems;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.SculkSensorPhase;
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
import net.minecraft.util.math.Direction;

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
		
		PressureSensor.register(CDBlocks.SENSOR_COLLISION.get(), Blocks.POLISHED_ANDESITE, generator);
		SoundSensor.register(CDBlocks.SENSOR_SOUND.get(), generator);
		SightSensor.register(CDBlocks.SENSOR_SIGHT.get(), generator);
		ProximitySensor.register(CDBlocks.SENSOR_PROXIMITY.get(), generator);
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
	
	private static void appendSettings(Direction face, VariantSettings.Rotation x, VariantSettings.Rotation y, BlockStateVariantMap.DoubleProperty<Direction, Boolean> variants, Identifier model, Identifier modelOn)
	{
		Function<Boolean,BlockStateVariant> func = phase -> 
		{
			BlockStateVariant variant = BlockStateVariant.create().put(VariantSettings.MODEL, phase ? modelOn : model);
			
			if(x != VariantSettings.Rotation.R0)
				variant.put(VariantSettings.X, x);
			
			if(y != VariantSettings.Rotation.R0)
				variant.put(VariantSettings.Y, y);
			return variant;
		};
		
		variants.register(face, false, func.apply(false));
		variants.register(face, true, func.apply(true));
	}
	
	private static class ProximitySensor
	{
		private static final Model SENSOR = new Model(
				Optional.of(prefix("block/template_proximity_sensor")),
				Optional.empty(),
				TextureKey.TEXTURE);
		
		private static void register(Block block, BlockStateModelGenerator generator)
		{
			TextureMap map = TextureMap.texture(block);
			Identifier model = SENSOR.upload(block, map, generator.modelCollector);
			Identifier modelOn = SENSOR.upload(block, "_on", map.put(TextureKey.TEXTURE, TextureMap.getSubId(block, "_on")), generator.modelCollector);
			
			BlockStateVariantMap.DoubleProperty<Direction, Boolean> variants = BlockStateVariantMap.create(ProximitySensorBlock.FACING, ProximitySensorBlock.POWERED);
			appendSettings(Direction.UP, VariantSettings.Rotation.R0, VariantSettings.Rotation.R0, variants, model, modelOn);
			appendSettings(Direction.DOWN, VariantSettings.Rotation.R180, VariantSettings.Rotation.R0, variants, model, modelOn);
			appendSettings(Direction.NORTH, VariantSettings.Rotation.R90, VariantSettings.Rotation.R0, variants, model, modelOn);
			appendSettings(Direction.SOUTH, VariantSettings.Rotation.R90, VariantSettings.Rotation.R180, variants, model, modelOn);
			appendSettings(Direction.EAST, VariantSettings.Rotation.R90, VariantSettings.Rotation.R90, variants, model, modelOn);
			appendSettings(Direction.WEST, VariantSettings.Rotation.R90, VariantSettings.Rotation.R270, variants, model, modelOn);
			
			generator.blockStateCollector.accept(VariantsBlockStateSupplier.create(block).coordinate(variants));
		}
	}
	
	private static class PressureSensor
	{
		private static final Model SENSOR = new Model(
				Optional.of(prefix("block/template_pressure_sensor")),
				Optional.empty(),
				TextureKey.TEXTURE);
		private static final Model SENSOR_PRESSED = new Model(
				Optional.of(prefix("block/template_pressure_sensor_pressed")),
				Optional.of("_pressed"),
				TextureKey.TEXTURE);
		
		private static void register(Block block, Block texture, BlockStateModelGenerator generator)
		{
			TextureMap map = TextureMap.texture(texture);
			Identifier model = SENSOR.upload(block, map, generator.modelCollector);
			Identifier modelOn = SENSOR_PRESSED.upload(block, map, generator.modelCollector);
			
			BlockStateVariantMap.DoubleProperty<Direction, Boolean> variants = BlockStateVariantMap.create(CollisionSensorBlock.FACING, CollisionSensorBlock.POWERED);
			appendSettings(Direction.UP, VariantSettings.Rotation.R0, VariantSettings.Rotation.R0, variants, model, modelOn);
			appendSettings(Direction.DOWN, VariantSettings.Rotation.R180, VariantSettings.Rotation.R0, variants, model, modelOn);
			appendSettings(Direction.NORTH, VariantSettings.Rotation.R90, VariantSettings.Rotation.R0, variants, model, modelOn);
			appendSettings(Direction.SOUTH, VariantSettings.Rotation.R90, VariantSettings.Rotation.R180, variants, model, modelOn);
			appendSettings(Direction.EAST, VariantSettings.Rotation.R90, VariantSettings.Rotation.R90, variants, model, modelOn);
			appendSettings(Direction.WEST, VariantSettings.Rotation.R90, VariantSettings.Rotation.R270, variants, model, modelOn);
			
			generator.blockStateCollector.accept(VariantsBlockStateSupplier.create(block).coordinate(variants));
		}
	}
	
	private static class SoundSensor
	{
		private static final TextureKey TENDRILS	= TextureKey.of("tendrils");
		private static final Identifier TENDRILS_ACTIVE = TextureMap.getSubId(Blocks.SCULK_SENSOR, "_tendril_active");
		private static final Identifier TENDRILS_INACTIVE = TextureMap.getSubId(Blocks.SCULK_SENSOR, "_tendril_inactive");
		
		private static final Model SENSOR = new Model(
				Optional.of(prefix("block/template_sound_sensor")),
				Optional.empty(),
				TextureKey.BOTTOM, TextureKey.SIDE, TextureKey.TOP, TENDRILS);
		
		private static void register(Block block, BlockStateModelGenerator generator)
		{
			TextureMap map = new TextureMap()
				.put(TextureKey.BOTTOM, TextureMap.getSubId(block, "_bottom"))
				.put(TextureKey.SIDE, TextureMap.getSubId(block, "_side"))
				.put(TextureKey.TOP, TextureMap.getSubId(block, "_top"));
			
			Identifier model = SENSOR.upload(block, map.put(TENDRILS, TENDRILS_INACTIVE), generator.modelCollector);
			Identifier modelOn = SENSOR.upload(block, "_on", map.put(TENDRILS, TENDRILS_ACTIVE), generator.modelCollector);
			
			BlockStateVariantMap.DoubleProperty<Direction, SculkSensorPhase> variants = BlockStateVariantMap.create(SoundSensorBlock.FACING, SoundSensorBlock.PHASE);
			appendSettings(Direction.UP, VariantSettings.Rotation.R0, VariantSettings.Rotation.R0, variants, model, modelOn);
			appendSettings(Direction.DOWN, VariantSettings.Rotation.R180, VariantSettings.Rotation.R0, variants, model, modelOn);
			appendSettings(Direction.NORTH, VariantSettings.Rotation.R90, VariantSettings.Rotation.R0, variants, model, modelOn);
			appendSettings(Direction.SOUTH, VariantSettings.Rotation.R90, VariantSettings.Rotation.R180, variants, model, modelOn);
			appendSettings(Direction.EAST, VariantSettings.Rotation.R90, VariantSettings.Rotation.R90, variants, model, modelOn);
			appendSettings(Direction.WEST, VariantSettings.Rotation.R90, VariantSettings.Rotation.R270, variants, model, modelOn);
			
			generator.blockStateCollector.accept(VariantsBlockStateSupplier.create(block).coordinate(variants));
		}
		
		private static void appendSettings(Direction face, VariantSettings.Rotation x, VariantSettings.Rotation y, BlockStateVariantMap.DoubleProperty<Direction, SculkSensorPhase> variants, Identifier model, Identifier modelOn)
		{
			Function<SculkSensorPhase,BlockStateVariant> func = phase -> 
			{
				BlockStateVariant variant = BlockStateVariant.create().put(VariantSettings.MODEL, phase == SculkSensorPhase.ACTIVE ? modelOn : model);
				
				if(x != VariantSettings.Rotation.R0)
					variant.put(VariantSettings.X, x);
				
				if(y != VariantSettings.Rotation.R0)
					variant.put(VariantSettings.Y, y);
				return variant;
			};
			
			for(SculkSensorPhase phase : SculkSensorPhase.values())
				variants.register(face, phase, func.apply(phase));
		}
	}
	
	private static class SightSensor
	{
		private static final TextureKey EYE	= TextureKey.of("eye");
		private static final Model SENSOR = new Model(
				Optional.of(prefix("block/template_sight_sensor")),
				Optional.empty(),
				TextureKey.SIDE, EYE);
		
		private static void register(Block block, BlockStateModelGenerator generator)
		{
			TextureMap map = new TextureMap()
					.put(TextureKey.SIDE, TextureMap.getSubId(block, "_side"));
			Identifier model = SENSOR.upload(block, map.put(EYE, TextureMap.getId(block)), generator.modelCollector);
			Identifier modelOn = SENSOR.upload(block, "_on", map.put(EYE, TextureMap.getSubId(block, "_on")), generator.modelCollector);
			
			BlockStateVariantMap variants = BlockStateVariantMap.create(SightSensorBlock.POWERED)
					.register(false, BlockStateVariant.create().put(VariantSettings.MODEL, model))
					.register(true, BlockStateVariant.create().put(VariantSettings.MODEL, modelOn));
			
			generator.blockStateCollector.accept(VariantsBlockStateSupplier.create(block).coordinate(variants));
		}
	}
}
