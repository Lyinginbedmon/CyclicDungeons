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
		context.register(CDStructurePools.PRISTINE_FLOOR_KEY, new StructurePool(registry, ImmutableList.of(
				create("dungeon/pristine_floor/plains/floor_1", 1)
				), StructurePool.Projection.RIGID));
		context.register(CDStructurePools.FLOOR_LIGHT_KEY, new StructurePool(registry, ImmutableList.of(
				create("dungeon/floor_light/plains/desk_lamp", 1),
				create("dungeon/floor_light/plains/lantern", 1),
				create("dungeon/floor_light/plains/tiki_torch", 1)
				), StructurePool.Projection.RIGID));
		context.register(CDStructurePools.WET_FLOOR_KEY, new StructurePool(registry, ImmutableList.of(
				create("dungeon/floor_wet/plains/floor_01", 1)
				), StructurePool.Projection.RIGID));
		context.register(CDStructurePools.PILLAR_KEY, new StructurePool(registry, ImmutableList.of(
				create("dungeon/pillar/plains/pillar_01", 1)
				), StructurePool.Projection.RIGID));
		context.register(CDStructurePools.PILLAR_BASE_KEY, new StructurePool(registry, ImmutableList.of(
				create("dungeon/pillar_base/plains/pillar_01", 1)
				), StructurePool.Projection.RIGID));
		context.register(CDStructurePools.PILLAR_CAP_KEY, new StructurePool(registry, ImmutableList.of(
				create("dungeon/pillar_cap/plains/pillar_01", 1)
				), StructurePool.Projection.RIGID));
		context.register(CDStructurePools.HOT_FLOOR_KEY, new StructurePool(registry, ImmutableList.of(
				create("dungeon/floor_hot/plains/flooring_01", 1),
				create("dungeon/floor_hot/plains/flooring_02", 1),
				create("dungeon/floor_hot/plains/flooring_03", 1)
				), StructurePool.Projection.RIGID));
		context.register(CDStructurePools.PUDDLE_KEY, new StructurePool(registry, ImmutableList.of(
				create("dungeon/puddle/plains/puddle_01", 1),
				create("dungeon/puddle/plains/puddle_02", 1),
				create("dungeon/puddle/plains/puddle_03", 1)
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
		context.register(CDStructurePools.WORKSTATION_KEY, new StructurePool(registry, ImmutableList.of(
				create("dungeon/workstation/plains/workstation_01", 1),
				create("dungeon/workstation/plains/workstation_02", 1),
				create("dungeon/workstation/plains/workstation_03", 1)
				), StructurePool.Projection.RIGID));
		context.register(CDStructurePools.PASSAGE_FLOOR_KEY, new StructurePool(registry, ImmutableList.of(
				create("dungeon/passage_floor/plains/floor_01", 1),
				create("dungeon/passage_floor/plains/floor_02", 1),
				create("dungeon/passage_floor/plains/floor_03", 1),
				create("dungeon/passage_floor/plains/floor_04", 1)
				), StructurePool.Projection.RIGID));
		context.register(CDStructurePools.DOORWAY_KEY, new StructurePool(registry, ImmutableList.of(
				create("dungeon/doorway/plains/door_01", 1),
				create("dungeon/doorway/plains/door_02", 1),
				create("dungeon/doorway/plains/door_03", 1),
				create("dungeon/doorway/plains/door_04", 1)
				), StructurePool.Projection.RIGID));
		context.register(CDStructurePools.DOORWAY_LINTEL_KEY, new StructurePool(registry, ImmutableList.of(
				create("dungeon/doorway_lintel/plains/door_01", 1),
				create("dungeon/doorway_lintel/plains/door_02", 1),
				create("dungeon/doorway_lintel/plains/door_03", 1),
				create("dungeon/doorway_lintel/plains/door_04", 1)
				), StructurePool.Projection.RIGID));
		context.register(CDStructurePools.TREASURE_KEY, new StructurePool(registry, ImmutableList.of(
				create("dungeon/treasure/plains/chest_01", 1),
				create("dungeon/treasure/plains/chest_02", 1),
				create("dungeon/treasure/plains/chest_03", 1)
				), StructurePool.Projection.RIGID));
		context.register(CDStructurePools.HATCH_KEY, new StructurePool(registry, ImmutableList.of(
				create("dungeon/pitfall_hatch/plains/hatch_01", 1),
				create("dungeon/pitfall_hatch/plains/hatch_02", 1),
				create("dungeon/pitfall_hatch/plains/hatch_03", 1),
				create("dungeon/pitfall_hatch/plains/hatch_04", 1)
				), StructurePool.Projection.RIGID));
	}
	
	private static Pair<Function<Projection, ? extends StructurePoolElement>, Integer> create(String name, int weight)
	{
		return Pair.of(StructurePoolElement.ofSingle(Reference.ModInfo.MOD_ID+":"+name), weight);
	}
}
