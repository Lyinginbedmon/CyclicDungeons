package com.lying.worldgen.tileset;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
		ID_TRAP				= prefix("trap"),
		ID_PITFALL_TRAP		= prefix("pitfall_trap");
	
	private static final List<TileSet> ROOM_TILESETS	= List.of(
			new TileSet(ID_DEFAULT)
				.addPlaceableFlooring()
				.addAll(Map.of(
					DefaultTiles.ID_PUDDLE, 1000,
					DefaultTiles.ID_WET_FLOOR, 750,
					DefaultTiles.ID_POOL, 100,
					DefaultTiles.ID_SEAT, 10,
					DefaultTiles.ID_FLOOR_LIGHT, 1,
					DefaultTiles.ID_TABLE, 1,
					DefaultTiles.ID_TABLE_LIGHT, 1,
					DefaultTiles.ID_WORKSTATION, 1)),
			new TileSet(ID_DEFAULT_PASSAGE)
				.add(DefaultTiles.ID_PASSAGE_FLOOR, 10000)
				.add(DefaultTiles.ID_FLOOR_LIGHT, 1),
			new TileSet(ID_START)
				.addPlaceableFlooring(),
			new TileSet(ID_END)
				.addPlaceableFlooring(),
			new TileSet(ID_EMPTY)
				.addGenericFlooring()
				.addAll(Map.of(
					DefaultTiles.ID_SEAT, 10,
					DefaultTiles.ID_FLOOR_LIGHT, 1,
					DefaultTiles.ID_TABLE, 1,
					DefaultTiles.ID_TABLE_LIGHT, 1,
					DefaultTiles.ID_WORKSTATION, 1)),
			new TileSet(ID_TREASURE)
				.addPlaceableFlooring()
				.add(DefaultTiles.ID_TREASURE, 3000),
			new TileSet(ID_BOSS)
				.addAll(Map.of(
					DefaultTiles.ID_PILLAR_BASE, 3000,
					DefaultTiles.ID_PILLAR, 3000,
					DefaultTiles.ID_PILLAR_CAP, 3000,
					DefaultTiles.ID_PRISTINE_FLOOR, 1000,
					DefaultTiles.ID_FLOOR, 1000,
					DefaultTiles.ID_HOT_FLOOR, 750,
					DefaultTiles.ID_LAVA, 750)),
			new TileSet(ID_BATTLE)
				.addAll(Map.of(
					DefaultTiles.ID_PRISTINE_FLOOR, 750,
					DefaultTiles.ID_FLOOR, 1000,
					DefaultTiles.ID_HOT_FLOOR, 1000,
					DefaultTiles.ID_SEAT, 10,
					DefaultTiles.ID_FLOOR_LIGHT, 1,
					DefaultTiles.ID_TABLE, 1,
					DefaultTiles.ID_TABLE_LIGHT, 1,
					DefaultTiles.ID_WORKSTATION, 1)),
			new TileSet(ID_PUZZLE)
				.addPlaceableFlooring(),
			new TileSet(ID_TRAP)
				.addPlaceableFlooring());
	
	private static final List<TileSet> TRAP_TILESETS = List.of(
			new TileSet(ID_PITFALL_TRAP)
			.addAll(Map.of(
				DefaultTiles.ID_PITFALL, 30
					))
			);
	
	private static final List<TileSet> DEFAULTS = new ArrayList<>();
	
	static
	{
		// Adds a low-weight air entry to all default room tile sets, to prevent overcrowding
		ROOM_TILESETS.forEach(set -> set.add(CDTiles.ID_AIR, 10));
		
		DEFAULTS.addAll(ROOM_TILESETS);
		DEFAULTS.addAll(TRAP_TILESETS);
	}
	
	public static List<TileSet> getDefaults()
	{
		return DEFAULTS;
	}
}
