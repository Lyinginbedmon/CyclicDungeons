package com.lying.worldgen.tileset;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.lying.init.CDTiles;
import com.lying.worldgen.tile.DefaultTiles;

import net.minecraft.util.Identifier;

public class DefaultTileSets
{
	public static final Identifier
		ID_DEFAULT			= prefix("default"),
		ID_DEFAULT_PASSAGE	= prefix("passage"),
		ID_START			= prefix("start"),
		ID_END				= prefix("end"),
		ID_EMPTY			= prefix("empty"),
		ID_TREASURE			= prefix("treasure"),
		ID_BOSS				= prefix("boss"),
		ID_BATTLE			= prefix("battle"),
		ID_PUZZLE			= prefix("puzzle"),
		ID_TRAP				= prefix("trap");
	
	private static final List<TileSet> DEFAULTS	= Lists.newArrayList(
			new TileSet(ID_DEFAULT)
				.addPlaceableFlooring()
				.addAll(Map.of(
					DefaultTiles.ID_PUDDLE, 1000F,
					DefaultTiles.ID_WET_FLOOR, 750F,
					DefaultTiles.ID_POOL, 100F,
					DefaultTiles.ID_SEAT, 10F,
					DefaultTiles.ID_FLOOR_LIGHT, 1F,
					DefaultTiles.ID_TABLE, 1F,
					DefaultTiles.ID_TABLE_LIGHT, 1F,
					DefaultTiles.ID_WORKSTATION, 1F)),
			new TileSet(ID_DEFAULT_PASSAGE)
				.add(DefaultTiles.ID_PASSAGE_FLOOR, 10000F)
				.add(DefaultTiles.ID_FLOOR_LIGHT, 1F),
			new TileSet(ID_START)
				.addPlaceableFlooring(),
			new TileSet(ID_END)
				.addPlaceableFlooring(),
			new TileSet(ID_EMPTY)
				.addGenericFlooring()
				.addAll(Map.of(
					DefaultTiles.ID_SEAT, 10F,
					DefaultTiles.ID_FLOOR_LIGHT, 1F,
					DefaultTiles.ID_TABLE, 1F,
					DefaultTiles.ID_TABLE_LIGHT, 1F,
					DefaultTiles.ID_WORKSTATION, 1F)),
			new TileSet(ID_TREASURE)
				.addPlaceableFlooring()
				.add(DefaultTiles.ID_TREASURE, 3000F),
			new TileSet(ID_BOSS)
				.addAll(Map.of(
					DefaultTiles.ID_PILLAR_BASE, 3000F,
					DefaultTiles.ID_PILLAR, 3000F,
					DefaultTiles.ID_PILLAR_CAP, 3000F,
					DefaultTiles.ID_PRISTINE_FLOOR, 1000F,
					DefaultTiles.ID_FLOOR, 1000F,
					DefaultTiles.ID_HOT_FLOOR, 750F,
					DefaultTiles.ID_LAVA, 750F)),
			new TileSet(ID_BATTLE)
				.addAll(Map.of(
					DefaultTiles.ID_PRISTINE_FLOOR, 750F,
					DefaultTiles.ID_FLOOR, 1000F,
					DefaultTiles.ID_HOT_FLOOR, 1000F,
					DefaultTiles.ID_SEAT, 10F,
					DefaultTiles.ID_FLOOR_LIGHT, 1F,
					DefaultTiles.ID_TABLE, 1F,
					DefaultTiles.ID_TABLE_LIGHT, 1F,
					DefaultTiles.ID_WORKSTATION, 1F)),
			new TileSet(ID_PUZZLE)
				.addPlaceableFlooring(),
			new TileSet(ID_TRAP)
				.addPlaceableFlooring()
			);
	
	static
	{
		// Adds a low-weight air entry to all default tile sets, to prevent overcrowding
		DEFAULTS.forEach(set -> set.add(CDTiles.ID_AIR, 10F));
	}
	
	public static List<TileSet> getDefaults()
	{
		return DEFAULTS;
	}
}
