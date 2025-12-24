package com.lying.init;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.base.Predicates;
import com.lying.CyclicDungeons;
import com.lying.data.CDStructurePools;
import com.lying.worldgen.Tile;
import com.lying.worldgen.Tile.RotationSupplier;
import com.lying.worldgen.TileConditions;
import com.lying.worldgen.TilePredicate;

import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

public class CDTiles
{
	private static final Map<Identifier, Supplier<Tile>> TILES = new HashMap<>();
	private static int tally = 0;
	
	public static final Identifier 
		ID_BLANK			= prefix("blank"), 
		ID_AIR				= prefix("air"), 
		ID_PASSAGE			= prefix("passage_flag"), 
		ID_DOORWAY			= prefix("doorway"),
		ID_DOORWAY_LINTEL	= prefix("doorway_lintel"),
		ID_FLOOR_PRISTINE	= prefix("floor_room_pristine"),
		ID_FLOOR_ROOM		= prefix("floor_room"),
		ID_FLOOR_PASSAGE	= prefix("floor_passage"),
		ID_BOUNDARY_PASSAGE	= prefix("boundary_passage"),
		ID_PUDDLE			= prefix("puddle"),
		ID_WET_FLOOR		= prefix("wet_floor"),
		ID_POOL				= prefix("pool"),
		ID_TABLE			= prefix("table"),
		ID_SEAT				= prefix("seat"),
		ID_LIGHT_FLOOR		= prefix("light_floor"),
		ID_LIGHT_TABLE		= prefix("light_table"),
		ID_WORKSTATION		= prefix("workstation"),
		ID_PILLAR_BASE		= prefix("pillar_base"),
		ID_PILLAR			= prefix("pillar"),
		ID_PILLAR_CAP		= prefix("pillar_cap"),
		ID_LAVA				= prefix("lava"),
		ID_HOT_FLOOR		= prefix("hot_floor"),
		ID_TREASURE			= prefix("treasure"),
		ID_HATCH			= prefix("pitfall_hatch"),
		ID_LAVA_RIVER		= prefix("lava_river");
	
	// Blank tile, used during generation
	public static final Supplier<Tile> BLANK	= register(ID_BLANK, Tile.Builder
			.of(TilePredicate.fromCondition(TileConditions.never()))
			.asFlag().build());
	
	// Flag tiles, usually empty air
	public static final Supplier<Tile> AIR		= register(ID_AIR, Tile.Builder
			.of(TilePredicate.fromCondition(TileConditions.not(TileConditions.onBottomLayer())))
			.asAir().build());
	public static final Supplier<Tile> PASSAGE	= register(ID_PASSAGE, Tile.Builder
			.of(TilePredicate.fromCondition(TileConditions.boundary(Direction.Type.HORIZONTAL)))
			.asAir().build());
	public static final Supplier<Tile> DOORWAY	= register(ID_DOORWAY, Tile.Builder
			.of(TilePredicate.fromCondition(TileConditions.boundary(Direction.Type.HORIZONTAL)))
			.asStructure(CDStructurePools.DOORWAY_KEY)
			.build());
	public static final Supplier<Tile> DOORWAY_LINTEL	= register(ID_DOORWAY_LINTEL, Tile.Builder
			.of(TilePredicate.fromCondition(TileConditions.boundary(Direction.Type.HORIZONTAL)))
			.asStructure(CDStructurePools.DOORWAY_LINTEL_KEY)
			.build());
	
	// Flooring tiles
	public static final Supplier<Tile> FLOOR_PRISTINE	= register(ID_FLOOR_PRISTINE, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(TileConditions.onBottomLayer())
				.condition(TileConditions.nonAdjacent(CDTileTags.DAMP::contains))
				.build())
			.asStructure(CDStructurePools.PRISTINE_FLOOR_KEY)
			.freeRotation().build());
	public static final Supplier<Tile> FLOOR	= register(ID_FLOOR_ROOM, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(TileConditions.onBottomLayer())
				.condition(TileConditions.nonAdjacent(CDTileTags.WET::contains))
				.build())
			.asStructure(CDStructurePools.FLOOR_KEY)
			.freeRotation().build());
	public static final Supplier<Tile> PUDDLE	= register(ID_PUDDLE, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(TileConditions.onBottomLayer())
				.condition(TileConditions.nonAdjacent(t -> t.registryName().equals(ID_POOL)))
				.condition(TileConditions.nonPassage())
				.build())
			.asStructure(CDStructurePools.PUDDLE_KEY)
			.freeRotation().build());
	public static final Supplier<Tile> WET_FLOOR	= register(ID_WET_FLOOR, Tile.Builder.
			of(TilePredicate.Builder.create()
				.condition(TileConditions.onBottomLayer())
				.condition(TileConditions.adjacent(CDTileTags.DAMP::contains))
				.condition(TileConditions.nonAdjacent(CDTileTags.SOLID_FLOORING::contains))
				.condition(TileConditions.nonPassage())
				.build())
			.asStructure(CDStructurePools.WET_FLOOR_KEY)
			.freeRotation().build());
	public static final Supplier<Tile> POOL		= register(ID_POOL, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(TileConditions.onBottomLayer())
				.condition(TileConditions.adjacent(CDTileTags.WET::contains))
				.condition(TileConditions.nonAdjacent(CDTileTags.SOLID_FLOORING::contains))
				.condition(TileConditions.nonPassage())
				.build())
			.asBlock(Blocks.WATER.getDefaultState())
			.build());
	
	// Passage tiles
	public static final Supplier<Tile> PASSAGE_FLOOR	= register(ID_FLOOR_PASSAGE, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(TileConditions.onBottomLayer())
				.build())
			.asStructure(CDStructurePools.PASSAGE_FLOOR_KEY)
			.freeRotation().build());
	public static final Supplier<Tile> PASSAGE_BOUNDARY	= register(ID_BOUNDARY_PASSAGE, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(TileConditions.always())
				.build())
			.asStructure(CDStructurePools.PASSAGE_BOUNDARY_KEY)
			.freeRotation().build());
	
	// Decoration & content tiles
	public static final Supplier<Tile> TABLE	= register(ID_TABLE, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(TileConditions.onFloor())
				.condition(TileConditions.boundary(Direction.Type.HORIZONTAL))
				.condition(TileConditions.avoid(Box.enclosing(new BlockPos(-1,0,-1), new BlockPos(1,0,1)), CDTileTags.TABLES::contains))
				.build())
			.asStructure(CDStructurePools.TABLE_KEY)
			.freeRotation().build());
	public static final Supplier<Tile> TABLE_LIGHT	= register(ID_LIGHT_TABLE, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(TileConditions.onFloor())
				.condition(TileConditions.boundary(Direction.Type.HORIZONTAL))
				.condition(TileConditions.nonConsecutive())
				.condition(TileConditions.avoid(Box.enclosing(new BlockPos(-1,0,-1), new BlockPos(1,0,1)), Predicates.or(CDTileTags.TABLES::contains, CDTileTags.LIGHTING::contains)))
				.build())
			.asStructure(CDStructurePools.TABLE_LIGHT_KEY)
			.freeRotation().build());
	public static final Supplier<Tile> SEAT		= register(ID_SEAT, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(TileConditions.onFloor())
				.condition(TileConditions.adjacent(Direction.Type.HORIZONTAL, CDTileTags.TABLES::contains))
				.build())
			.asStructure(CDStructurePools.SEAT_KEY)
			.withRotation(RotationSupplier.toFaceAdjacent(CDTileTags.TABLES::contains)).build());
	public static final Supplier<Tile> FLOOR_LIGHT	= register(ID_LIGHT_FLOOR, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(TileConditions.boundary(Direction.Type.HORIZONTAL))
				.condition(TileConditions.onFloor())
				.condition(TileConditions.nonConsecutive())
				.condition(TileConditions.avoid(Box.enclosing(new BlockPos(-2,0,-2), new BlockPos(2,0,2)), CDTileTags.LIGHTING::contains))
				.condition(TileConditions.nonAdjacent(CDTileTags.TABLES::contains))
				.build())
			.asStructure(CDStructurePools.FLOOR_LIGHT_KEY)
			.freeRotation().build());
	public static final Supplier<Tile> WORKSTATION	= register(ID_WORKSTATION, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(TileConditions.boundary(Direction.Type.HORIZONTAL))
				.condition(TileConditions.onFloor())
				.condition(TileConditions.nonConsecutive())
				.condition(TileConditions.avoid(Box.enclosing(new BlockPos(-2,0,-2), new BlockPos(2,0,2)), CDTileTags.DECOR::contains))
				.build())
			.asStructure(CDStructurePools.WORKSTATION_KEY)
			.withRotation(RotationSupplier.againstBoundary(RotationSupplier.random()))
			.build());
	public static final Supplier<Tile> PILLAR_BASE	= register(ID_PILLAR_BASE, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(TileConditions.onFloor())
				.condition(TileConditions.nonBoundary())
				.condition((tile,pos,set) -> Direction.Type.HORIZONTAL.stream().filter(d -> set.isBoundary(pos.offset(d),d)).count() >= 2)
				.condition(TileConditions.nonConsecutive())
				.condition(TileConditions.maxOf(4))
				.build())
			.asStructure(CDStructurePools.PILLAR_BASE_KEY)
			.freeRotation()
			.build());
	public static final Supplier<Tile> PILLAR		= register(ID_PILLAR, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(TileConditions.nonBoundary())
				.condition(TileConditions.adjacent(List.of(Direction.DOWN), TileConditions.isAnyOf(ID_PILLAR_BASE, ID_PILLAR)))
				.build())
			.asStructure(CDStructurePools.PILLAR_KEY)
			.freeRotation()
			.build());
	public static final Supplier<Tile> PILLAR_CAP	= register(ID_PILLAR_CAP, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(TileConditions.onTopLayer())
				.condition(TileConditions.adjacent(List.of(Direction.DOWN), TileConditions.isAnyOf(ID_PILLAR_BASE, ID_PILLAR)))
				.build())
			.asStructure(CDStructurePools.PILLAR_CAP_KEY)
			.freeRotation()
			.build());
	
	// Loot
	public static final Supplier<Tile> TREASURE	= register(ID_TREASURE, Tile.Builder
			.of(
				TilePredicate.Builder.create()
				.condition(TileConditions.nonBoundary())
				.condition(TileConditions.onFloor())
				.build())
			.asStructure(CDStructurePools.TREASURE_KEY)
			.freeRotation()
			.build());
	
	// Hazards
	public static final Supplier<Tile> LAVA		= register(ID_LAVA, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(TileConditions.onBottomLayer())
				.condition(TileConditions.nonAdjacent(CDTileTags.DAMP::contains))
				.condition(TileConditions.adjacent(CDTileTags.HOT::contains))
				.condition(TileConditions.nonPassage())
				.build())
			.asBlock(Blocks.LAVA.getDefaultState())
			.build());
	public static final Supplier<Tile> LAVA_RIVER	= register(ID_LAVA_RIVER, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(TileConditions.onBottomLayer())
				.condition(TileConditions.nonPassage())
				.condition(TileConditions.maxAdjacent(Direction.Type.HORIZONTAL.stream().toList(), TileConditions.isAnyOf(CDTiles.ID_LAVA_RIVER), 2))
				.build())
			.asBlock(Blocks.LAVA.getDefaultState())
			.build());
	public static final Supplier<Tile> HOT_FLOOR	= register(ID_HOT_FLOOR, Tile.Builder.
			of(TilePredicate.Builder.create()
				.condition(TileConditions.onBottomLayer())
				.condition(TileConditions.nonAdjacent(CDTileTags.DAMP::contains))
				.condition(TileConditions.adjacent(CDTileTags.SOLID_FLOORING::contains))
				.condition(TileConditions.nonPassage())
				.build())
			.asStructure(CDStructurePools.HOT_FLOOR_KEY)
			.freeRotation().build());
	public static final Supplier<Tile> HATCH	= register(ID_HATCH, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(TileConditions.onBottomLayer())
				.condition(TileConditions.nonPassage())
				.build())
			.asStructure(CDStructurePools.HATCH_KEY)
			.freeRotation()
			.build());
	
	@SuppressWarnings("unused")
	private static Supplier<Tile> register(String name, Function<Identifier,Tile> funcIn)
	{
		return register(prefix(name), funcIn);
	}
	
	private static Supplier<Tile> register(final Identifier id, Function<Identifier,Tile> funcIn)
	{
		Supplier<Tile> sup = () -> funcIn.apply(id);
		TILES.put(id, sup);
		tally++;
		return sup;
	}
	
	public static Optional<Tile> get(String name) { return get(name.contains(":") ? Identifier.of(name) : prefix(name)); }
	
	public static Optional<Tile> get(Identifier id) { return TILES.containsKey(id) ? Optional.of(TILES.get(id).get()) : Optional.empty(); }
	
	public static void init()
	{
		CyclicDungeons.LOGGER.info("# Initialised {} generator tiles", tally);
	}
}
