package com.lying.data;

import com.lying.reference.Reference;

import net.minecraft.registry.RegistryKey;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePools;

public class CDStructurePools
{
	public static final RegistryKey<StructurePool>	FLOOR_KEY		= of("dungeon/floor/plains");
	public static final RegistryKey<StructurePool>	FLOOR_LIGHT_KEY	= of("dungeon/floor_light/plains");
	public static final RegistryKey<StructurePool>	PUDDLE_KEY		= of("dungeon/puddle/plains");
	public static final RegistryKey<StructurePool>	TABLE_KEY		= of("dungeon/table/plains");
	public static final RegistryKey<StructurePool>	TABLE_LIGHT_KEY	= of("dungeon/table_light/plains");
	public static final RegistryKey<StructurePool>	SEAT_KEY		= of("dungeon/seat/plains");
	public static final RegistryKey<StructurePool>	WORKSTATION_KEY	= of("dungeon/workstation/plains");
	
	public static final RegistryKey<StructurePool>	PASSAGE_FLOOR_KEY		= of("dungeon/passage_floor/plains");
	
	public static RegistryKey<StructurePool> of(String path)
	{
		return StructurePools.of(Reference.ModInfo.MOD_ID+":"+path);
	}
}
