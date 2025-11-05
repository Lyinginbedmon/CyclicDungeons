package com.lying.init;

import java.util.HashMap;
import java.util.Map;

import com.lying.worldgen.Tile;

public class CDRoomTileSets
{
	public static final Map<Tile,Float> EMPTY_ROOM_TILESET	= new HashMap<>();
	public static final Map<Tile,Float> TREASURE_ROOM_TILESET	= new HashMap<>();
	public static final Map<Tile,Float> BOSS_ROOM_TILESET	= new HashMap<>();
	public static final Map<Tile,Float> BATTLE_ROOM_TILESET	= new HashMap<>();
	public static final Map<Tile,Float> PUZZLE_ROOM_TILESET	= new HashMap<>();
	
	public static void init()
	{
		// Empty room tile set
		addClearFlooring(EMPTY_ROOM_TILESET);
		EMPTY_ROOM_TILESET.put(CDTiles.AIR.get(), 10F);
		EMPTY_ROOM_TILESET.put(CDTiles.SEAT.get(), 10F);
		EMPTY_ROOM_TILESET.put(CDTiles.FLOOR_LIGHT.get(), 1F);
		EMPTY_ROOM_TILESET.put(CDTiles.TABLE.get(), 1F);
		EMPTY_ROOM_TILESET.put(CDTiles.TABLE_LIGHT.get(), 1F);
		EMPTY_ROOM_TILESET.put(CDTiles.WORKSTATION.get(), 1F);
		
		// Treasure room tile set
		addClearFlooring(TREASURE_ROOM_TILESET);
		TREASURE_ROOM_TILESET.put(CDTiles.AIR.get(), 10F);
		TREASURE_ROOM_TILESET.put(CDTiles.TREASURE.get(), 3000F);
		
		// Boss room tile set
		BOSS_ROOM_TILESET.put(CDTiles.AIR.get(), 10F);
		BOSS_ROOM_TILESET.put(CDTiles.PILLAR_BASE.get(), 3000F);
		BOSS_ROOM_TILESET.put(CDTiles.PILLAR.get(), 3000F);
		BOSS_ROOM_TILESET.put(CDTiles.PILLAR_CAP.get(), 3000F);
		BOSS_ROOM_TILESET.put(CDTiles.FLOOR_PRISTINE.get(), 1000F);
		BOSS_ROOM_TILESET.put(CDTiles.FLOOR.get(), 1000F);
		BOSS_ROOM_TILESET.put(CDTiles.HOT_FLOOR.get(), 750F);
		BOSS_ROOM_TILESET.put(CDTiles.LAVA.get(), 750F);
		
		// Battle room tile set
		BATTLE_ROOM_TILESET.put(CDTiles.AIR.get(), 10F);
		BATTLE_ROOM_TILESET.put(CDTiles.FLOOR_PRISTINE.get(), 750F);
		BATTLE_ROOM_TILESET.put(CDTiles.FLOOR.get(), 1000F);
		BATTLE_ROOM_TILESET.put(CDTiles.HOT_FLOOR.get(), 1000F);
		BATTLE_ROOM_TILESET.put(CDTiles.LAVA.get(), 500F);
		
		// Puzzle room tile set
		PUZZLE_ROOM_TILESET.put(CDTiles.FLOOR_PRISTINE.get(), 3000F);
		PUZZLE_ROOM_TILESET.put(CDTiles.AIR.get(), 10F);
	}
	
	private static void addClearFlooring(Map<Tile,Float> map)
	{
		map.put(CDTiles.FLOOR_PRISTINE.get(), 3000F);
		map.put(CDTiles.FLOOR.get(), 1000F);
		map.put(CDTiles.PUDDLE.get(), 1000F);
		map.put(CDTiles.WET_FLOOR.get(), 200F);
		map.put(CDTiles.POOL.get(), 10F);
	}
}
