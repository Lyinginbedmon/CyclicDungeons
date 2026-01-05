package com.lying.worldgen.tile;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import com.lying.init.CDTileConditions;
import com.lying.init.CDTileTags;
import com.lying.worldgen.tile.condition.Adjacent;
import com.lying.worldgen.tile.condition.Boundary;
import com.lying.worldgen.tile.condition.IsAnyOf;
import com.lying.worldgen.tile.condition.MaxAdjacentBoundaries;
import com.lying.worldgen.tile.condition.MaxConsecutive;
import com.lying.worldgen.tile.condition.MaxPerRoom;
import com.lying.worldgen.tile.condition.NearBox;

import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

public class DefaultTiles
{
	private static final Map<Identifier, Supplier<Tile>> TILES = new HashMap<>();
	
	public static final Identifier 
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
	
	// Flooring tiles
	public static final Supplier<Tile> FLOOR_PRISTINE	= register(ID_PRISTINE_FLOOR, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(CDTileConditions.ON_BOTTOM.get())
				.condition(Adjacent.Inverse.of(IsAnyOf.HasTag.of(CDTileTags.ID_DAMP)))
				.build())
			.asStructure()
			.tags(CDTileTags.ID_SOLID_FLOORING)
			.freeRotation().build());
	public static final Supplier<Tile> FLOOR	= register(ID_FLOOR, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(CDTileConditions.ON_BOTTOM.get())
				.condition(Adjacent.Inverse.of(IsAnyOf.HasTag.of(CDTileTags.ID_WET)))
				.build())
			.asStructure()
			.tags(CDTileTags.ID_SOLID_FLOORING)
			.freeRotation().build());
	public static final Supplier<Tile> PUDDLE	= register(ID_PUDDLE, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(CDTileConditions.ON_BOTTOM.get())
				.condition(Adjacent.Inverse.of(IsAnyOf.of(ID_POOL)))
				.condition(CDTileConditions.NOT_BY_PASSAGE.get())
				.build())
			.asStructure()
			.tags(CDTileTags.ID_DAMP)
			.freeRotation().build());
	public static final Supplier<Tile> WET_FLOOR	= register(ID_WET_FLOOR, Tile.Builder.
			of(TilePredicate.Builder.create()
				.condition(CDTileConditions.ON_BOTTOM.get())
				.condition(Adjacent.of(IsAnyOf.HasTag.of(CDTileTags.ID_DAMP)))
				.condition(Adjacent.Inverse.of(IsAnyOf.HasTag.of(CDTileTags.ID_SOLID_FLOORING)))
				.condition(CDTileConditions.NOT_BY_PASSAGE.get())
				.build())
			.asStructure()
			.tags(CDTileTags.ID_WET, CDTileTags.ID_DAMP)
			.freeRotation().build());
	public static final Supplier<Tile> POOL		= register(ID_POOL, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(CDTileConditions.ON_BOTTOM.get())
				.condition(Adjacent.of(IsAnyOf.HasTag.of(CDTileTags.ID_WET)))
				.condition(Adjacent.Inverse.of(IsAnyOf.HasTag.of(CDTileTags.ID_SOLID_FLOORING)))
				.condition(CDTileConditions.NOT_BY_PASSAGE.get())
				.build())
			.asBlock(Blocks.WATER.getDefaultState())
			.tags(CDTileTags.ID_WET, CDTileTags.ID_DAMP)
			.build());
	
	// Passage tiles
	public static final Supplier<Tile> PASSAGE_FLOOR	= register(ID_PASSAGE_FLOOR, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(CDTileConditions.ON_BOTTOM.get())
				.build())
			.asStructure()
			.tags(CDTileTags.ID_SOLID_FLOORING)
			.freeRotation().build());
	public static final Supplier<Tile> PASSAGE_BOUNDARY	= register(ID_PASSAGE_BOUNDARY, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(CDTileConditions.ALWAYS.get())
				.build())
			.asStructure()
			.freeRotation().build());
	
	// Decoration & content tiles
	public static final Supplier<Tile> TABLE	= register(ID_TABLE, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(CDTileConditions.ON_FLOOR.get())
				.condition(Boundary.of(Direction.Type.HORIZONTAL))
				.condition(NearBox.Inverse.of(Box.enclosing(new BlockPos(-1,0,-1), new BlockPos(1,0,1)), IsAnyOf.HasTag.of(CDTileTags.ID_TABLES)))
				.build())
			.asStructure()
			.tags(CDTileTags.ID_TABLES, CDTileTags.ID_OBTRUSIVE, CDTileTags.ID_DECOR)
			.freeRotation().build());
	public static final Supplier<Tile> TABLE_LIGHT	= register(ID_TABLE_LIGHT, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(CDTileConditions.ON_FLOOR.get())
				.condition(Boundary.of(Direction.Type.HORIZONTAL))
				.condition(CDTileConditions.NON_CONSECUTIVE.get())
				.condition(NearBox.Inverse.of(Box.enclosing(new BlockPos(-1,0,-1), new BlockPos(1,0,1)), IsAnyOf.HasTag.of(CDTileTags.ID_TABLES, CDTileTags.ID_LIGHTING)))
				.build())
			.asStructure()
			.tags(CDTileTags.ID_TABLES, CDTileTags.ID_LIGHTING, CDTileTags.ID_OBTRUSIVE, CDTileTags.ID_DECOR)
			.freeRotation().build());
	public static final Supplier<Tile> SEAT		= register(ID_SEAT, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(CDTileConditions.ON_FLOOR.get())
				.condition(Adjacent.of(Direction.Type.HORIZONTAL, IsAnyOf.HasTag.of(CDTileTags.ID_TABLES)))
				.build())
			.asStructure()
			.withRotation(RotationSupplier.FaceAdjacent.of(IsAnyOf.HasTag.of(CDTileTags.ID_TABLES)))
			.tags(CDTileTags.ID_OBTRUSIVE, CDTileTags.ID_DECOR).build());
	public static final Supplier<Tile> FLOOR_LIGHT	= register(ID_FLOOR_LIGHT, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(Boundary.of(Direction.Type.HORIZONTAL))
				.condition(CDTileConditions.ON_FLOOR.get())
				.condition(CDTileConditions.NON_CONSECUTIVE.get())
				.condition(NearBox.Inverse.of(Box.enclosing(new BlockPos(-2,0,-2), new BlockPos(2,0,2)), IsAnyOf.HasTag.of(CDTileTags.ID_LIGHTING)))
				.condition(Adjacent.Inverse.of(IsAnyOf.HasTag.of(CDTileTags.ID_TABLES)))
				.build())
			.asStructure()
			.tags(CDTileTags.ID_LIGHTING, CDTileTags.ID_DECOR)
			.freeRotation().build());
	public static final Supplier<Tile> WORKSTATION	= register(ID_WORKSTATION, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(Boundary.of(Direction.Type.HORIZONTAL))
				.condition(CDTileConditions.ON_FLOOR.get())
				.condition(CDTileConditions.NON_CONSECUTIVE.get())
				.condition(NearBox.Inverse.of(Box.enclosing(new BlockPos(-2,0,-2), new BlockPos(2,0,2)), IsAnyOf.HasTag.of(CDTileTags.ID_DECOR)))
				.build())
			.asStructure()
			.withRotation(RotationSupplier.FaceBoundary.of(RotationSupplier.RANDOM.get()))
			.tags(CDTileTags.ID_DECOR)
			.build());
	public static final Supplier<Tile> PILLAR_BASE	= register(ID_PILLAR_BASE, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(CDTileConditions.ON_FLOOR.get())
				.condition(CDTileConditions.NON_BOUNDARY.get())
				.condition(MaxAdjacentBoundaries.Inverse.of(Direction.Type.HORIZONTAL.stream().toList(), 2))
				.condition(CDTileConditions.NON_CONSECUTIVE.get())
				.condition(MaxPerRoom.of(4))
				.build())
			.asStructure()
			.freeRotation()
			.tags(CDTileTags.ID_OBTRUSIVE, CDTileTags.ID_DECOR)
			.build());
	public static final Supplier<Tile> PILLAR		= register(ID_PILLAR, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(CDTileConditions.NON_BOUNDARY.get())
				.condition(Adjacent.of(List.of(Direction.DOWN), IsAnyOf.of(ID_PILLAR_BASE, ID_PILLAR)))
				.build())
			.asStructure()
			.freeRotation()
			.build());
	public static final Supplier<Tile> PILLAR_CAP	= register(ID_PILLAR_CAP, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(CDTileConditions.ON_TOP.get())
				.condition(Adjacent.of(List.of(Direction.DOWN), IsAnyOf.of(ID_PILLAR_BASE, ID_PILLAR)))
				.build())
			.asStructure()
			.freeRotation()
			.tags(CDTileTags.ID_SOLID_FLOORING, CDTileTags.ID_CEILING)
			.build());
	
	// Loot
	public static final Supplier<Tile> TREASURE	= register(ID_TREASURE, Tile.Builder
			.of(
				TilePredicate.Builder.create()
				.condition(CDTileConditions.NON_BOUNDARY.get())
				.condition(CDTileConditions.ON_FLOOR.get())
				.build())
			.asStructure()
			.freeRotation()
			.tags(CDTileTags.ID_OBTRUSIVE, CDTileTags.ID_DECOR)
			.build());
	
	// Hazards
	public static final Supplier<Tile> LAVA		= register(ID_LAVA, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(CDTileConditions.ON_BOTTOM.get())
				.condition(Adjacent.Inverse.of(IsAnyOf.HasTag.of(CDTileTags.ID_DAMP)))
				.condition(Adjacent.of(IsAnyOf.HasTag.of(CDTileTags.ID_HOT)))
				.condition(CDTileConditions.NOT_BY_PASSAGE.get())
				.build())
			.asBlock(Blocks.LAVA.getDefaultState())
			.tags(CDTileTags.ID_HOT)
			.build());
	public static final Supplier<Tile> LAVA_RIVER	= register(ID_LAVA_RIVER, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(CDTileConditions.ON_BOTTOM.get())
				.condition(CDTileConditions.NOT_BY_PASSAGE.get())
				.condition(MaxConsecutive.of(Direction.Type.HORIZONTAL.stream().toList(), 2))
				.build())
			.asBlock(Blocks.LAVA.getDefaultState())
			.build());
	public static final Supplier<Tile> HOT_FLOOR	= register(ID_HOT_FLOOR, Tile.Builder.
			of(TilePredicate.Builder.create()
				.condition(CDTileConditions.ON_BOTTOM.get())
				.condition(Adjacent.Inverse.of(IsAnyOf.HasTag.of(CDTileTags.ID_DAMP)))
				.condition(Adjacent.of(IsAnyOf.HasTag.of(CDTileTags.ID_SOLID_FLOORING)))
				.condition(CDTileConditions.NOT_BY_PASSAGE.get())
				.build())
			.asStructure()
			.tags(CDTileTags.ID_SOLID_FLOORING, CDTileTags.ID_HOT)
			.freeRotation().build());
	public static final Supplier<Tile> HATCH	= register(ID_HATCH, Tile.Builder
			.of(TilePredicate.Builder.create()
				.condition(CDTileConditions.NOT_BY_PASSAGE.get())
				.condition(CDTileConditions.ON_BOTTOM.get())
				.build())
			.asStructure()
			.freeRotation()
			.tags(CDTileTags.ID_TRAPS)
			.build());
	
	private static Supplier<Tile> register(final Identifier id, Function<Identifier,Tile> funcIn)
	{
		Supplier<Tile> sup = () -> funcIn.apply(id);
		TILES.put(id, sup);
		return sup;
	}
	
	public static List<Tile> getAll()
	{
		return TILES.values().stream().map(Supplier::get).toList();
	}
}
