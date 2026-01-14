package com.lying.fabric.data;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import com.lying.init.CDBlocks;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemConvertible;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.BlockStatePropertyLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.predicate.StatePredicate;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.state.property.Property;

@SuppressWarnings("unused")
public class CDBlockLootTableProvider extends FabricBlockLootTableProvider
{
	private final RegistryWrapper.Impl<Enchantment> enchantments = this.registries.getOrThrow(RegistryKeys.ENCHANTMENT);
	private static final List<Supplier<Block>> DROP_SELF = List.of(
			CDBlocks.CRUMBLING_COBBLESTONE,
			CDBlocks.CRUMBLING_MOSSY_COBBLESTONE,
			CDBlocks.CRUMBLING_RED_SANDSTONE,
			CDBlocks.CRUMBLING_SANDSTONE,
			CDBlocks.CRUMBLING_STONE,
			CDBlocks.CRUMBLING_STONE_BRICKS
			);
	
	public CDBlockLootTableProvider(FabricDataOutput dataOutput, CompletableFuture<WrapperLookup> registryLookup)
	{
		super(dataOutput, registryLookup);
	}
	
	public void generate()
	{
		DROP_SELF.stream().map(Supplier::get).forEach(this::addDrop);
		
		// Resetting crumbling blocks drop their non-resetting version when mined
		Map.of(
				CDBlocks.RESETTING_CRUMBLING_COBBLESTONE, CDBlocks.CRUMBLING_COBBLESTONE,
				CDBlocks.RESETTING_CRUMBLING_MOSSY_COBBLESTONE, CDBlocks.CRUMBLING_MOSSY_COBBLESTONE,
				CDBlocks.RESETTING_CRUMBLING_RED_SANDSTONE, CDBlocks.CRUMBLING_RED_SANDSTONE,
				CDBlocks.RESETTING_CRUMBLING_SANDSTONE, CDBlocks.CRUMBLING_SANDSTONE,
				CDBlocks.RESETTING_CRUMBLING_STONE, CDBlocks.CRUMBLING_STONE,
				CDBlocks.RESETTING_CRUMBLING_STONE_BRICKS, CDBlocks.CRUMBLING_STONE_BRICKS
			).entrySet().forEach(entry -> addDrop(entry.getKey().get(), entry.getValue().get()));
	}
	
	private <T extends Property<U>, U extends Comparable<U>> LootPool.Builder conditionalPool(Block drop, T property, U val)
	{
		return addSurvivesExplosionCondition(
				drop,
				LootPool.builder()
					.rolls(ConstantLootNumberProvider.create(1.0F))
					.with(
						ofItem(drop)
							.conditionally(BlockStatePropertyLootCondition.builder(drop).properties(StatePredicate.Builder.create().exactMatch(property, property.name(val))))
							// FIXME Ensure block only dropped when mined by entity
					)
			);
	}
	
	private static ItemEntry.Builder<?> ofItem(ItemConvertible item)
	{
		return ItemEntry.builder(item);
	}
}
