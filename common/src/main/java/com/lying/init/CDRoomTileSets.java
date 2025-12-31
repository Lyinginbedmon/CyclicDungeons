package com.lying.init;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.lying.worldgen.Tile;

public class CDRoomTileSets
{
	public static final TileSet DEFAULT_TILESET			= new TileSet();
	
	public static final TileSet START_ROOM_TILESET		= new TileSet();
	public static final TileSet END_ROOM_TILESET		= new TileSet();
	public static final TileSet EMPTY_ROOM_TILESET		= new TileSet();
	public static final TileSet TREASURE_ROOM_TILESET	= new TileSet();
	public static final TileSet BOSS_ROOM_TILESET		= new TileSet();
	public static final TileSet BATTLE_ROOM_TILESET		= new TileSet();
	public static final TileSet PUZZLE_ROOM_TILESET		= new TileSet();
	public static final TileSet TRAP_ROOM_TILESET		= new TileSet();
	
	public static void init()
	{
		// Basic default tile set
		DEFAULT_TILESET
			.addPlaceableFlooring()
			.addAll(Map.of(
				CDTiles.PUDDLE, 1000F,
				CDTiles.WET_FLOOR, 750F,
				CDTiles.POOL, 100F,
				CDTiles.SEAT, 10F,
				CDTiles.FLOOR_LIGHT, 1F,
				CDTiles.TABLE, 1F,
				CDTiles.TABLE_LIGHT, 1F,
				CDTiles.WORKSTATION, 1F));
		
		// Start room tile set
		START_ROOM_TILESET
			.addPlaceableFlooring();
		
		// End room tile set
		END_ROOM_TILESET
			.addPlaceableFlooring();
		
		// Empty room tile set
		EMPTY_ROOM_TILESET
			.addGenericFlooring()
			.addAll(Map.of(
				CDTiles.SEAT, 10F,
				CDTiles.FLOOR_LIGHT, 1F,
				CDTiles.TABLE, 1F,
				CDTiles.TABLE_LIGHT, 1F,
				CDTiles.WORKSTATION, 1F));
		
		// Treasure room tile set
		TREASURE_ROOM_TILESET
			.addPlaceableFlooring()
			.add(CDTiles.TREASURE.get(), 3000F);
		
		// Boss room tile set
		BOSS_ROOM_TILESET.addAll(Map.of(
			CDTiles.PILLAR_BASE, 3000F,
			CDTiles.PILLAR, 3000F,
			CDTiles.PILLAR_CAP, 3000F,
			CDTiles.FLOOR_PRISTINE, 1000F,
			CDTiles.FLOOR, 1000F,
			CDTiles.HOT_FLOOR, 750F,
			CDTiles.LAVA, 750F));
		
		// Battle room tile set
		BATTLE_ROOM_TILESET.addAll(Map.of(
			CDTiles.FLOOR_PRISTINE, 750F,
			CDTiles.FLOOR, 1000F,
			CDTiles.HOT_FLOOR, 1000F,
			CDTiles.SEAT, 10F,
			CDTiles.FLOOR_LIGHT, 1F,
			CDTiles.TABLE, 1F,
			CDTiles.TABLE_LIGHT, 1F,
			CDTiles.WORKSTATION, 1F));
		
		// Puzzle room tile set
		PUZZLE_ROOM_TILESET
			.addPlaceableFlooring();
		
		// Trap room tile set
		TRAP_ROOM_TILESET
			.addPlaceableFlooring();
	}
	
	public static class TileSet extends HashMap<Tile,Float>
	{
		private static final long serialVersionUID = 1L;
		
		public TileSet()
		{
			// Adds a low-weight air entry to all tile sets, to prevent overcrowding
			add(CDTiles.AIR.get(), 10F);
		}
		
		public TileSet add(Tile tileIn, Float weightIn)
		{
			super.put(tileIn, weightIn);
			return this;
		}
		
		public TileSet addAll(Map<Supplier<Tile>,Float> mapIn)
		{
			mapIn.entrySet().forEach(e -> add(e.getKey().get(), e.getValue()));
			return this;
		}
		
		/** Adds pristine and regular flooring, things most decor can be placed on */
		public TileSet addPlaceableFlooring()
		{
			put(CDTiles.FLOOR_PRISTINE.get(), 3000F);
			put(CDTiles.FLOOR.get(), 1000F);
			return this;
		}
		
		/** Adds the flooring tiles used in the default tileset */
		public TileSet addGenericFlooring()
		{
			addPlaceableFlooring();
			put(CDTiles.PUDDLE.get(), 1000F);
			put(CDTiles.WET_FLOOR.get(), 200F);
			put(CDTiles.POOL.get(), 10F);
			return this;
		}
	}
}
