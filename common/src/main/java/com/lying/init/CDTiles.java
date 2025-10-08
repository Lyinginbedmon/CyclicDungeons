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
	
	private static final List<Direction> HORIZONTAL_FACES = Direction.Type.HORIZONTAL.stream().toList();
	
	// Blank tile, used during generation
	public static final Supplier<Tile> BLANK	= register("blank", Tile.Builder
			.of(TilePredicate.Builder.create().never().build())
			.asFlag().build());
	
	// Flag tiles, usually empty
	public static final Supplier<Tile> AIR		= register("air", Tile.Builder
			.of(TilePredicate.Builder.create().always().build())
			.asFlag().build());
	public static final Supplier<Tile> PASSAGE	= register("passage", Tile.Builder
			.of(TilePredicate.Builder.create().boundary(Direction.Type.HORIZONTAL).build())
			.asBlock(Blocks.ORANGE_STAINED_GLASS.getDefaultState()).build());
	
	// Flooring tiles
	public static final Supplier<Tile> FLOOR	= register("floor", Tile.Builder
			.of(TilePredicate.Builder.create()
				.boundary(List.of(Direction.DOWN))
				.build())
			.asStructure(CDStructurePools.FLOOR_KEY)
			.freeRotation().build());
	public static final Supplier<Tile> PUDDLE	= register("puddle", Tile.Builder
			.of(TilePredicate.Builder.create()
				.boundary(List.of(Direction.DOWN))
				.build())
			.asStructure(CDStructurePools.PUDDLE_KEY)
			.freeRotation().build());
	public static final Supplier<Tile> POOL		= register("pool", Tile.Builder
			.of(TilePredicate.Builder.create()
				.boundary(List.of(Direction.DOWN))
				.nonAdjacent(t -> t.is(CDTiles.PASSAGE.get()))
				.build())
			.asBlock(Blocks.WATER.getDefaultState())
			.build());
	
	// Decoration & content tiles
	public static final Supplier<Tile> TABLE	= register("table", Tile.Builder
			.of(TilePredicate.Builder.create()
				.onFloor()
				.boundary(HORIZONTAL_FACES)
				.avoid(Box.enclosing(new BlockPos(-1,0,-1), new BlockPos(1,0,1)), CDTileTags.TABLES::contains)
				.build())
			.asStructure(CDStructurePools.TABLE_KEY)
			.freeRotation().build());
	public static final Supplier<Tile> TABLE_LIGHT	= register("table_light", Tile.Builder
			.of(TilePredicate.Builder.create()
				.onFloor()
				.boundary(HORIZONTAL_FACES)
				.nonConsecutive()
				.avoid(Box.enclosing(new BlockPos(-1,0,-1), new BlockPos(1,0,1)), Predicates.or(CDTileTags.TABLES::contains, CDTileTags.LIGHTING::contains))
				.build())
			.asStructure(CDStructurePools.TABLE_LIGHT_KEY)
			.freeRotation().build());
	public static final Supplier<Tile> SEAT		= register("seat", Tile.Builder
			.of(TilePredicate.Builder.create()
				.onFloor()
				.adjacent(HORIZONTAL_FACES, CDTileTags.TABLES::contains)
				.build())
			.asStructure(CDStructurePools.SEAT_KEY)
			.withRotation(RotationSupplier.toFaceAdjacent(CDTileTags.TABLES::contains)).build());
	public static final Supplier<Tile> FLOOR_LIGHT	= register("floor_light", Tile.Builder
			.of(TilePredicate.Builder.create()
				.boundary(HORIZONTAL_FACES)
				.onFloor()
				.nonConsecutive()
				.avoid(Box.enclosing(new BlockPos(-2,0,-2), new BlockPos(2,0,2)), CDTileTags.LIGHTING::contains)
				.nonAdjacent(CDTileTags.TABLES::contains)
				.build())
			.asStructure(CDStructurePools.FLOOR_LIGHT_KEY)
			.freeRotation().build());
	public static final Supplier<Tile> WORKSTATION	= register("workstation", Tile.Builder
			.of(TilePredicate.Builder.create()
				.boundary(HORIZONTAL_FACES)
				.onFloor()
				.nonConsecutive()
				.avoid(Box.enclosing(new BlockPos(-2,0,-2), new BlockPos(2,0,2)), CDTileTags.DECOR::contains)
				.build())
			.asStructure(CDStructurePools.WORKSTATION_KEY)
			.withRotation(RotationSupplier.againstBoundary(RotationSupplier.random()))
			.build());
	
	private static Supplier<Tile> register(String name, Function<Identifier,Tile> funcIn)
	{
		final Identifier id = prefix(name);
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
