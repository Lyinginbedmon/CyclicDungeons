package com.lying.fabric.data;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

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
			
			);
	
	public CDBlockLootTableProvider(FabricDataOutput dataOutput, CompletableFuture<WrapperLookup> registryLookup)
	{
		super(dataOutput, registryLookup);
	}
	
	public void generate()
	{
		DROP_SELF.stream().map(Supplier::get).forEach(this::addDrop);
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
					)
			);
	}
	
	private static ItemEntry.Builder<?> ofItem(ItemConvertible item)
	{
		return ItemEntry.builder(item);
	}
}
