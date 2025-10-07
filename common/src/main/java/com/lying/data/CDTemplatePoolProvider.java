package com.lying.data;

import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import com.lying.CyclicDungeons;
import com.lying.reference.Reference;
import com.mojang.datafixers.util.Pair;

import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePool.Projection;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.structure.pool.StructurePools;

public class CDTemplatePoolProvider
{
	public CDTemplatePoolProvider(Registerable<StructurePool> context)
	{
		CyclicDungeons.LOGGER.info("# Generated structure template pools");
		
		RegistryEntryLookup<StructurePool> lookup = context.getRegistryLookup(RegistryKeys.TEMPLATE_POOL);
		RegistryEntry.Reference<StructurePool> registry = lookup.getOrThrow(StructurePools.EMPTY);
		context.register(CDStructurePools.FLOOR_KEY, new StructurePool(registry, ImmutableList.of(
				create("dungeon/floor/plains/flooring_01", 1),
				create("dungeon/floor/plains/flooring_02", 1),
				create("dungeon/floor/plains/flooring_03", 1),
				create("dungeon/floor/plains/flooring_04", 1),
				create("dungeon/floor/plains/flooring_05", 1)
				), StructurePool.Projection.RIGID));
		context.register(CDStructurePools.FLOOR_LIGHT_KEY, new StructurePool(registry, ImmutableList.of(
				create("dungeon/light/plains/desk_lamp", 1),
				create("dungeon/light/plains/lantern", 1),
				create("dungeon/light/plains/tiki_torch", 1)
				), StructurePool.Projection.RIGID));
		context.register(CDStructurePools.TABLE_LIGHT_KEY, new StructurePool(registry, ImmutableList.of(
				create("dungeon/table_light/plains/table_01", 1),
				create("dungeon/table_light/plains/table_02", 1),
				create("dungeon/table_light/plains/table_03", 1),
				create("dungeon/table_light/plains/table_04", 1)
				), StructurePool.Projection.RIGID));
		context.register(CDStructurePools.SEAT_KEY, new StructurePool(registry, ImmutableList.of(
				create("dungeon/seat/plains/seat_01", 1),
				create("dungeon/seat/plains/seat_02", 1),
				create("dungeon/seat/plains/seat_03", 1),
				create("dungeon/seat/plains/seat_04", 1)
				), StructurePool.Projection.RIGID));
		context.register(CDStructurePools.TABLE_KEY, new StructurePool(registry, ImmutableList.of(
				create("dungeon/table/plains/table_01", 1),
				create("dungeon/table/plains/table_02", 1)
				), StructurePool.Projection.RIGID));
	}
	
	private static Pair<Function<Projection, ? extends StructurePoolElement>, Integer> create(String name, int weight)
	{
		return Pair.of(StructurePoolElement.ofSingle(Reference.ModInfo.MOD_ID+":"+name), weight);
	}
}
