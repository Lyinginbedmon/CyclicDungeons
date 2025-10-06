package com.lying.init;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

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
	
	public static final Supplier<Tile> BLANK	= register("blank", Tile.Builder
			.of(TilePredicate.Builder.create().never().build())
			.asFlag().build());
	
	public static final Supplier<Tile> AIR		= register("air", Tile.Builder
			.of(TilePredicate.Builder.create().always().build())
			.asFlag().build());
	public static final Supplier<Tile> PASSAGE	= register("passage", Tile.Builder
			.of(TilePredicate.Builder.create().boundary(Direction.Type.HORIZONTAL).build())
			.asBlock(Blocks.IRON_BARS.getDefaultState()).build());
	
	public static final Supplier<Tile> FLOOR	= register("floor", Tile.Builder
			.of(TilePredicate.Builder.create()
				.boundary(List.of(Direction.DOWN))
				.build())
			.asStructure(CDStructurePools.FLOOR_KEY)
			.freeRotation().build());
	public static final Supplier<Tile> TABLE	= register("table", Tile.Builder
			.of(TilePredicate.Builder.create()
				.onFloor()
				.boundary(HORIZONTAL_FACES)
				.nonConsecutive()
				.build())
			.asStructure(CDStructurePools.TABLE_KEY)
			.freeRotation().build());
	public static final Supplier<Tile> TABLE_LIGHT	= register("table_light", Tile.Builder
			.of(TilePredicate.Builder.create()
				.onFloor()
				.boundary(HORIZONTAL_FACES)
				.nonConsecutive()
				.nonAdjacent(List.of(CDTiles.TABLE))
				.build())
			.asStructure(CDStructurePools.TABLE_LIGHT_KEY)
			.freeRotation().build());
	public static final Supplier<Tile> SEAT		= register("seat", Tile.Builder
			.of(TilePredicate.Builder.create()
				.onFloor()
				.adjacent(HORIZONTAL_FACES, List.of(CDTiles.TABLE, CDTiles.TABLE_LIGHT))
				.build())
			.asStructure(CDStructurePools.SEAT_KEY)
			.withRotation(RotationSupplier.toFaceAdjacent(List.of(CDTiles.TABLE, CDTiles.TABLE_LIGHT))).build());
	public static final Supplier<Tile> FLOOR_LIGHT	= register("floor_light", Tile.Builder
			.of(TilePredicate.Builder.create()
				.boundary(HORIZONTAL_FACES)
				.onFloor()
				.nonConsecutive()
				.avoid(Box.enclosing(new BlockPos(-2,0,-2), new BlockPos(2,0,2)), List.of(CDTiles.TABLE, CDTiles.TABLE_LIGHT, CDTiles.SEAT))
				.build())
			.asStructure(CDStructurePools.FLOOR_LIGHT_KEY)
			.freeRotation().build());
	
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
