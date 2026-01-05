package com.lying.data;

import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import com.lying.CyclicDungeons;
import com.lying.init.CDThemes;
import com.lying.init.CDTiles;
import com.lying.reference.Reference;
import com.lying.worldgen.theme.Theme;
import com.lying.worldgen.tile.DefaultTiles;
import com.mojang.datafixers.util.Pair;

import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePool.Projection;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.structure.pool.StructurePools;
import net.minecraft.util.Identifier;

public class CDTemplatePoolProvider
{
	public CDTemplatePoolProvider(Registerable<StructurePool> context)
	{
		CyclicDungeons.LOGGER.info("# Generated structure template pools");
		
		/**
		 * Pools are in the format of [dungeon/theme/tile]
		 * Entries are in the format of [dungeon/tile/theme/filename]
		 */
		
		RegistryEntryLookup<StructurePool> lookup = context.getRegistryLookup(RegistryKeys.TEMPLATE_POOL);
		RegistryEntry.Reference<StructurePool> registry = lookup.getOrThrow(StructurePools.EMPTY);
		context.register(getPool(CDThemes.ID_GENERIC, DefaultTiles.ID_FLOOR), new StructurePool(registry, ImmutableList.of(
				create("dungeon/floor/generic/flooring_01", 1),
				create("dungeon/floor/generic/flooring_02", 1),
				create("dungeon/floor/generic/flooring_03", 1),
				create("dungeon/floor/generic/flooring_04", 1),
				create("dungeon/floor/generic/flooring_05", 1)
				), StructurePool.Projection.RIGID));
		context.register(getPool(CDThemes.ID_GENERIC, DefaultTiles.ID_PRISTINE_FLOOR), new StructurePool(registry, ImmutableList.of(
				create("dungeon/pristine_floor/generic/floor_1", 1)
				), StructurePool.Projection.RIGID));
		context.register(getPool(CDThemes.ID_GENERIC, DefaultTiles.ID_FLOOR_LIGHT), new StructurePool(registry, ImmutableList.of(
				create("dungeon/floor_light/generic/desk_lamp", 1),
				create("dungeon/floor_light/generic/lantern", 1),
				create("dungeon/floor_light/generic/tiki_torch", 1)
				), StructurePool.Projection.RIGID));
		context.register(getPool(CDThemes.ID_GENERIC, DefaultTiles.ID_WET_FLOOR), new StructurePool(registry, ImmutableList.of(
				create("dungeon/wet_floor/generic/floor_01", 1)
				), StructurePool.Projection.RIGID));
		context.register(getPool(CDThemes.ID_GENERIC, DefaultTiles.ID_PILLAR), new StructurePool(registry, ImmutableList.of(
				create("dungeon/pillar/generic/pillar_01", 1)
				), StructurePool.Projection.RIGID));
		context.register(getPool(CDThemes.ID_GENERIC, DefaultTiles.ID_PILLAR_BASE), new StructurePool(registry, ImmutableList.of(
				create("dungeon/pillar_base/generic/pillar_01", 1)
				), StructurePool.Projection.RIGID));
		context.register(getPool(CDThemes.ID_GENERIC, DefaultTiles.ID_PILLAR_CAP), new StructurePool(registry, ImmutableList.of(
				create("dungeon/pillar_cap/generic/pillar_01", 1)
				), StructurePool.Projection.RIGID));
		context.register(getPool(CDThemes.ID_GENERIC, DefaultTiles.ID_HOT_FLOOR), new StructurePool(registry, ImmutableList.of(
				create("dungeon/hot_floor/generic/flooring_01", 1),
				create("dungeon/hot_floor/generic/flooring_02", 1),
				create("dungeon/hot_floor/generic/flooring_03", 1)
				), StructurePool.Projection.RIGID));
		context.register(getPool(CDThemes.ID_GENERIC, DefaultTiles.ID_PUDDLE), new StructurePool(registry, ImmutableList.of(
				create("dungeon/puddle/generic/puddle_01", 1),
				create("dungeon/puddle/generic/puddle_02", 1),
				create("dungeon/puddle/generic/puddle_03", 1)
				), StructurePool.Projection.RIGID));
		context.register(getPool(CDThemes.ID_GENERIC, DefaultTiles.ID_TABLE_LIGHT), new StructurePool(registry, ImmutableList.of(
				create("dungeon/table_light/generic/table_01", 1),
				create("dungeon/table_light/generic/table_02", 1),
				create("dungeon/table_light/generic/table_03", 1),
				create("dungeon/table_light/generic/table_04", 1)
				), StructurePool.Projection.RIGID));
		context.register(getPool(CDThemes.ID_GENERIC, DefaultTiles.ID_SEAT), new StructurePool(registry, ImmutableList.of(
				create("dungeon/seat/generic/seat_01", 1),
				create("dungeon/seat/generic/seat_02", 1),
				create("dungeon/seat/generic/seat_03", 1),
				create("dungeon/seat/generic/seat_04", 1)
				), StructurePool.Projection.RIGID));
		context.register(getPool(CDThemes.ID_GENERIC, DefaultTiles.ID_TABLE), new StructurePool(registry, ImmutableList.of(
				create("dungeon/table/generic/table_01", 1),
				create("dungeon/table/generic/table_02", 1)
				), StructurePool.Projection.RIGID));
		context.register(getPool(CDThemes.ID_GENERIC, DefaultTiles.ID_WORKSTATION), new StructurePool(registry, ImmutableList.of(
				create("dungeon/workstation/generic/workstation_01", 1),
				create("dungeon/workstation/generic/workstation_02", 1),
				create("dungeon/workstation/generic/workstation_03", 1)
				), StructurePool.Projection.RIGID));
		context.register(getPool(CDThemes.ID_GENERIC, DefaultTiles.ID_PASSAGE_FLOOR), new StructurePool(registry, ImmutableList.of(
				create("dungeon/passage_floor/generic/floor_01", 1),
				create("dungeon/passage_floor/generic/floor_02", 1),
				create("dungeon/passage_floor/generic/floor_03", 1),
				create("dungeon/passage_floor/generic/floor_04", 1)
				), StructurePool.Projection.RIGID));
		context.register(getPool(CDThemes.ID_GENERIC, CDTiles.ID_DOORWAY), new StructurePool(registry, ImmutableList.of(
				create("dungeon/doorway/generic/door_01", 1),
				create("dungeon/doorway/generic/door_02", 1),
				create("dungeon/doorway/generic/door_03", 1),
				create("dungeon/doorway/generic/door_04", 1)
				), StructurePool.Projection.RIGID));
		context.register(getPool(CDThemes.ID_GENERIC, CDTiles.ID_DOORWAY_LINTEL), new StructurePool(registry, ImmutableList.of(
				create("dungeon/doorway_lintel/generic/door_01", 1),
				create("dungeon/doorway_lintel/generic/door_02", 1),
				create("dungeon/doorway_lintel/generic/door_03", 1),
				create("dungeon/doorway_lintel/generic/door_04", 1)
				), StructurePool.Projection.RIGID));
		context.register(getPool(CDThemes.ID_GENERIC, DefaultTiles.ID_TREASURE), new StructurePool(registry, ImmutableList.of(
				create("dungeon/treasure/generic/chest_01", 1),
				create("dungeon/treasure/generic/chest_02", 1),
				create("dungeon/treasure/generic/chest_03", 1)
				), StructurePool.Projection.RIGID));
		context.register(getPool(CDThemes.ID_GENERIC, DefaultTiles.ID_HATCH), new StructurePool(registry, ImmutableList.of(
				create("dungeon/pitfall_hatch/generic/hatch_01", 1),
				create("dungeon/pitfall_hatch/generic/hatch_02", 1),
				create("dungeon/pitfall_hatch/generic/hatch_03", 1),
				create("dungeon/pitfall_hatch/generic/hatch_04", 1)
				), StructurePool.Projection.RIGID));
	}
	
	public static RegistryKey<StructurePool> getPool(Identifier theme, Identifier tile)
	{
		return Theme.getTilePool(theme, tile);
	}
	
	private static Pair<Function<Projection, ? extends StructurePoolElement>, Integer> create(String name, int weight)
	{
		return Pair.of(StructurePoolElement.ofSingle(Reference.ModInfo.MOD_ID+":"+name), weight);
	}
}
