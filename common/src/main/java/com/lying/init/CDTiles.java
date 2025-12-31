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
		ID_FLOOR			= prefix("floor"),
		ID_FLOOR_LIGHT		= prefix("floor_light"),
		ID_HOT_FLOOR		= prefix("hot_floor"),
		ID_PASSAGE_BOUNDARY	= prefix("passage_boundary"),
		ID_PASSAGE_FLOOR	= prefix("passage_floor"),
		ID_PILLAR_BASE		= prefix("pillar_base"),
		ID_PILLAR_CAP		= prefix("pillar_cap"),
		ID_PILLAR			= prefix("pillar"),
		ID_HATCH			= prefix("pitfall_hatch"),
		ID_PRISTINE_FLOOR	= prefix("pristine_floor"),
		ID_PUDDLE			= prefix("puddle"),
		ID_SEAT				= prefix("seat"),
		ID_TABLE_LIGHT		= prefix("table_light"),
		ID_TABLE			= prefix("table"),
		ID_TREASURE			= prefix("treasure"),
		ID_WET_FLOOR		= prefix("wet_floor"),
		ID_WORKSTATION		= prefix("workstation"),
		ID_LAVA				= prefix("lava"),
		ID_LAVA_RIVER		= prefix("lava_river"),
		ID_POOL				= prefix("pool");
	
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
			.asStructure()
			.build());
	public static final Supplier<Tile> DOORWAY_LINTEL	= register(ID_DOORWAY_LINTEL, Tile.Builder
			.of(TilePredicate.fromCondition(TileConditions.boundary(Direction.Type.HORIZONTAL)))
			.asStructure()
			.build());
	
	// Flooring tiles
	public static final Supplier<Tile> FLOOR_PRISTINE	= register(ID_PRISTINE_FLOOR, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(TileConditions.onBottomLayer())
				.condition(TileConditions.nonAdjacent(CDTileTags.DAMP::contains))
				.build())
			.asStructure()
			.tags(CDTileTags.ID_SOLID_FLOORING)
			.freeRotation().build());
	public static final Supplier<Tile> FLOOR	= register(ID_FLOOR, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(TileConditions.onBottomLayer())
				.condition(TileConditions.nonAdjacent(CDTileTags.WET::contains))
				.build())
			.asStructure()
			.tags(CDTileTags.ID_SOLID_FLOORING)
			.freeRotation().build());
	public static final Supplier<Tile> PUDDLE	= register(ID_PUDDLE, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(TileConditions.onBottomLayer())
				.condition(TileConditions.nonAdjacent(t -> t.registryName().equals(ID_POOL)))
				.condition(TileConditions.nonPassage())
				.build())
			.asStructure()
			.tags(CDTileTags.ID_DAMP)
			.freeRotation().build());
	public static final Supplier<Tile> WET_FLOOR	= register(ID_WET_FLOOR, Tile.Builder.
			of(TilePredicate.Builder.create()
				.condition(TileConditions.onBottomLayer())
				.condition(TileConditions.adjacent(CDTileTags.DAMP::contains))
				.condition(TileConditions.nonAdjacent(CDTileTags.SOLID_FLOORING::contains))
				.condition(TileConditions.nonPassage())
				.build())
			.asStructure()
			.tags(CDTileTags.ID_WET, CDTileTags.ID_DAMP)
			.freeRotation().build());
	public static final Supplier<Tile> POOL		= register(ID_POOL, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(TileConditions.onBottomLayer())
				.condition(TileConditions.adjacent(CDTileTags.WET::contains))
				.condition(TileConditions.nonAdjacent(CDTileTags.SOLID_FLOORING::contains))
				.condition(TileConditions.nonPassage())
				.build())
			.asBlock(Blocks.WATER.getDefaultState())
			.tags(CDTileTags.ID_WET, CDTileTags.ID_DAMP)
			.build());
	
	// Passage tiles
	public static final Supplier<Tile> PASSAGE_FLOOR	= register(ID_PASSAGE_FLOOR, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(TileConditions.onBottomLayer())
				.build())
			.asStructure()
			.tags(CDTileTags.ID_SOLID_FLOORING)
			.freeRotation().build());
	public static final Supplier<Tile> PASSAGE_BOUNDARY	= register(ID_PASSAGE_BOUNDARY, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(TileConditions.always())
				.build())
			.asStructure()
			.freeRotation().build());
	
	// Decoration & content tiles
	public static final Supplier<Tile> TABLE	= register(ID_TABLE, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(TileConditions.onFloor())
				.condition(TileConditions.boundary(Direction.Type.HORIZONTAL))
				.condition(TileConditions.avoid(Box.enclosing(new BlockPos(-1,0,-1), new BlockPos(1,0,1)), CDTileTags.TABLES::contains))
				.build())
			.asStructure()
			.tags(CDTileTags.ID_TABLES, CDTileTags.ID_OBTRUSIVE, CDTileTags.ID_DECOR)
			.freeRotation().build());
	public static final Supplier<Tile> TABLE_LIGHT	= register(ID_TABLE_LIGHT, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(TileConditions.onFloor())
				.condition(TileConditions.boundary(Direction.Type.HORIZONTAL))
				.condition(TileConditions.nonConsecutive())
				.condition(TileConditions.avoid(Box.enclosing(new BlockPos(-1,0,-1), new BlockPos(1,0,1)), Predicates.or(CDTileTags.TABLES::contains, CDTileTags.LIGHTING::contains)))
				.build())
			.asStructure()
			.tags(CDTileTags.ID_TABLES, CDTileTags.ID_LIGHTING, CDTileTags.ID_OBTRUSIVE, CDTileTags.ID_DECOR)
			.freeRotation().build());
	public static final Supplier<Tile> SEAT		= register(ID_SEAT, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(TileConditions.onFloor())
				.condition(TileConditions.adjacent(Direction.Type.HORIZONTAL, CDTileTags.TABLES::contains))
				.build())
			.asStructure()
			.withRotation(RotationSupplier.toFaceAdjacent(CDTileTags.TABLES::contains))
			.tags(CDTileTags.ID_OBTRUSIVE, CDTileTags.ID_DECOR).build());
	public static final Supplier<Tile> FLOOR_LIGHT	= register(ID_FLOOR_LIGHT, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(TileConditions.boundary(Direction.Type.HORIZONTAL))
				.condition(TileConditions.onFloor())
				.condition(TileConditions.nonConsecutive())
				.condition(TileConditions.avoid(Box.enclosing(new BlockPos(-2,0,-2), new BlockPos(2,0,2)), CDTileTags.LIGHTING::contains))
				.condition(TileConditions.nonAdjacent(CDTileTags.TABLES::contains))
				.build())
			.asStructure()
			.tags(CDTileTags.ID_LIGHTING, CDTileTags.ID_DECOR)
			.freeRotation().build());
	public static final Supplier<Tile> WORKSTATION	= register(ID_WORKSTATION, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(TileConditions.boundary(Direction.Type.HORIZONTAL))
				.condition(TileConditions.onFloor())
				.condition(TileConditions.nonConsecutive())
				.condition(TileConditions.avoid(Box.enclosing(new BlockPos(-2,0,-2), new BlockPos(2,0,2)), CDTileTags.DECOR::contains))
				.build())
			.asStructure()
			.withRotation(RotationSupplier.againstBoundary(RotationSupplier.random()))
			.tags(CDTileTags.ID_DECOR)
			.build());
	public static final Supplier<Tile> PILLAR_BASE	= register(ID_PILLAR_BASE, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(TileConditions.onFloor())
				.condition(TileConditions.nonBoundary())
				.condition((tile,pos,set) -> Direction.Type.HORIZONTAL.stream().filter(d -> set.isBoundary(pos.offset(d),d)).count() >= 2)
				.condition(TileConditions.nonConsecutive())
				.condition(TileConditions.maxOf(4))
				.build())
			.asStructure()
			.freeRotation()
			.tags(CDTileTags.ID_OBTRUSIVE, CDTileTags.ID_DECOR)
			.build());
	public static final Supplier<Tile> PILLAR		= register(ID_PILLAR, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(TileConditions.nonBoundary())
				.condition(TileConditions.adjacent(List.of(Direction.DOWN), TileConditions.isAnyOf(ID_PILLAR_BASE, ID_PILLAR)))
				.build())
			.asStructure()
			.freeRotation()
			.build());
	public static final Supplier<Tile> PILLAR_CAP	= register(ID_PILLAR_CAP, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(TileConditions.onTopLayer())
				.condition(TileConditions.adjacent(List.of(Direction.DOWN), TileConditions.isAnyOf(ID_PILLAR_BASE, ID_PILLAR)))
				.build())
			.asStructure()
			.freeRotation()
			.tags(CDTileTags.ID_SOLID_FLOORING, CDTileTags.ID_CEILING)
			.build());
	
	// Loot
	public static final Supplier<Tile> TREASURE	= register(ID_TREASURE, Tile.Builder
			.of(
				TilePredicate.Builder.create()
				.condition(TileConditions.nonBoundary())
				.condition(TileConditions.onFloor())
				.build())
			.asStructure()
			.freeRotation()
			.tags(CDTileTags.ID_OBTRUSIVE, CDTileTags.ID_DECOR)
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
			.tags(CDTileTags.ID_HOT)
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
			.asStructure()
			.tags(CDTileTags.ID_SOLID_FLOORING, CDTileTags.ID_HOT)
			.freeRotation().build());
	public static final Supplier<Tile> HATCH	= register(ID_HATCH, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(TileConditions.onBottomLayer())
				.condition(TileConditions.nonPassage())
				.build())
			.asStructure()
			.freeRotation()
			.tags(CDTileTags.ID_TRAPS)
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
	
	public static List<Tile> getAllTiles()
	{
		return TILES.values().stream().map(Supplier::get).toList();
	}
	
	public static void init()
	{
		CyclicDungeons.LOGGER.info("# Initialised {} generator tiles", tally);
	}
}
