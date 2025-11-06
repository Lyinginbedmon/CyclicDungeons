package com.lying.init;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.lying.worldgen.Tile;

public class CDRoomTileSets
{
	public static final Map<Tile,Float> DEFAULT_TILESET			= new HashMap<>();
	
	public static final Map<Tile,Float> START_ROOM_TILESET		= new HashMap<>();
	public static final Map<Tile,Float> END_ROOM_TILESET		= new HashMap<>();
	public static final Map<Tile,Float> EMPTY_ROOM_TILESET		= new HashMap<>();
	public static final Map<Tile,Float> TREASURE_ROOM_TILESET	= new HashMap<>();
	public static final Map<Tile,Float> BOSS_ROOM_TILESET		= new HashMap<>();
	public static final Map<Tile,Float> BATTLE_ROOM_TILESET		= new HashMap<>();
	public static final Map<Tile,Float> PUZZLE_ROOM_TILESET		= new HashMap<>();
	public static final Map<Tile,Float> TRAP_ROOM_TILESET		= new HashMap<>();
	
	private static final List<Map<Tile,Float>> TILESETS	= List.of(
			DEFAULT_TILESET,
			START_ROOM_TILESET,
			END_ROOM_TILESET,
			EMPTY_ROOM_TILESET, 
			TREASURE_ROOM_TILESET, 
			BOSS_ROOM_TILESET, 
			BATTLE_ROOM_TILESET, 
			PUZZLE_ROOM_TILESET,
			TRAP_ROOM_TILESET);
	
	public static void init()
	{
		// Adds a low-weight air entry to all tile sets, to prevent overcrowding
		TILESETS.forEach(set -> buildTileSet(set, CDTiles.AIR, 10F));
		
		// Basic default tile set
		buildTileSet(DEFAULT_TILESET, Map.of(
			CDTiles.FLOOR_PRISTINE, 3000F,
			CDTiles.FLOOR, 1000F,
			CDTiles.PUDDLE, 1000F,
			CDTiles.WET_FLOOR, 750F,
			CDTiles.POOL, 100F,
			CDTiles.SEAT, 10F,
			CDTiles.FLOOR_LIGHT, 1F,
			CDTiles.TABLE, 1F,
			CDTiles.TABLE_LIGHT, 1F,
			CDTiles.WORKSTATION, 1F));
		
		// Start room tile set
		addPlaceableFlooring(START_ROOM_TILESET);
		
		// End room tile set
		addPlaceableFlooring(END_ROOM_TILESET);
		
		// Empty room tile set
		addGenericFlooring(EMPTY_ROOM_TILESET);
		buildTileSet(EMPTY_ROOM_TILESET, Map.of(
			CDTiles.SEAT, 10F,
			CDTiles.FLOOR_LIGHT, 1F,
			CDTiles.TABLE, 1F,
			CDTiles.TABLE_LIGHT, 1F,
			CDTiles.WORKSTATION, 1F));
		
		// Treasure room tile set
		addPlaceableFlooring(TREASURE_ROOM_TILESET);
		buildTileSet(TREASURE_ROOM_TILESET, CDTiles.TREASURE, 3000F);
		
		// Boss room tile set
		buildTileSet(BOSS_ROOM_TILESET, Map.of(
			CDTiles.PILLAR_BASE, 3000F,
			CDTiles.PILLAR, 3000F,
			CDTiles.PILLAR_CAP, 3000F,
			CDTiles.FLOOR_PRISTINE, 1000F,
			CDTiles.FLOOR, 1000F,
			CDTiles.HOT_FLOOR, 750F,
			CDTiles.LAVA, 750F));
		
		// Battle room tile set
		buildTileSet(BATTLE_ROOM_TILESET, Map.of(
			CDTiles.FLOOR_PRISTINE, 750F,
			CDTiles.FLOOR, 1000F,
			CDTiles.HOT_FLOOR, 1000F,
			CDTiles.LAVA, 500F));
		
		// Puzzle room tile set
		addPlaceableFlooring(PUZZLE_ROOM_TILESET);
		
		// Trap room tile set
		addPlaceableFlooring(TRAP_ROOM_TILESET);
		
	}
	
	public static void buildTileSet(Map<Tile,Float> tileSet, Map<Supplier<Tile>,Float> tilesIn)
	{
		tilesIn.entrySet().forEach(e -> buildTileSet(tileSet, e.getKey(), e.getValue()));
	}
	
	public static void buildTileSet(Map<Tile,Float> tileSet, Supplier<Tile> tile, Float weight)
	{
		tileSet.put(tile.get(), weight);
	}
	
	/** Adds pristine and regular flooring, things most decor can be placed on */
	public static void addPlaceableFlooring(Map<Tile,Float> map)
	{
		map.put(CDTiles.FLOOR_PRISTINE.get(), 3000F);
		map.put(CDTiles.FLOOR.get(), 1000F);
	}
	
	/** Adds the flooring tiles used in the default tileset */
	public static void addGenericFlooring(Map<Tile,Float> map)
	{
		addPlaceableFlooring(map);
		map.put(CDTiles.PUDDLE.get(), 1000F);
		map.put(CDTiles.WET_FLOOR.get(), 200F);
		map.put(CDTiles.POOL.get(), 10F);
	}
}
