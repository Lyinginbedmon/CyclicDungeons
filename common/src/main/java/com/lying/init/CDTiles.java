package com.lying.init;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.lying.CyclicDungeons;
import com.lying.data.CDStructurePools;
import com.lying.worldgen.Tile;
import com.lying.worldgen.Tile.StructureTile;
import com.lying.worldgen.TilePredicate;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class CDTiles
{
	private static final Map<Identifier, Supplier<Tile>> TERMS = new HashMap<>();
	private static int tally = 0;
	
	public static final Supplier<Tile> BLANK	= register("blank", Tile.of(TilePredicate.Builder.create().never().build(), noOp()));
	
	public static final Supplier<Tile> AIR		= register("air", Tile.of(TilePredicate.Builder.create().always().build(), ofBlocks(Blocks.AIR.getDefaultState())));
	public static final Supplier<Tile> PASSAGE	= register("passage", Tile.of(TilePredicate.Builder.create().boundary(Direction.Type.HORIZONTAL).build(), ofBlocks(Blocks.IRON_BARS.getDefaultState())));
	
	public static final Supplier<Tile> FLOOR	= register("floor", StructureTile.of(TilePredicate.Builder.create()
			.boundary(List.of(Direction.DOWN))
			.build(), CDStructurePools.FLOOR_KEY, true));
	public static final Supplier<Tile> TABLE	= register("table", StructureTile.of(TilePredicate.Builder.create()
			.onFloor()
			.nonConsecutive(Direction.Type.HORIZONTAL.stream().toList())
			.build(), CDStructurePools.TABLE_KEY, true));
	public static final Supplier<Tile> FLOOR_LIGHT	= register("floor_light", StructureTile.of(TilePredicate.Builder.create()
			.onFloor()
			.nonConsecutive(Direction.Type.HORIZONTAL.stream().toList())
			.build(), CDStructurePools.FLOOR_LIGHT_KEY, true));
	public static final Supplier<Tile> TABLE_LIGHT	= register("table_light", StructureTile.of(TilePredicate.Builder.create()
			.onFloor()
			.nonConsecutive(Direction.Type.HORIZONTAL.stream().toList())
			.build(), CDStructurePools.TABLE_LIGHT_KEY, true));
	public static final Supplier<Tile> SEAT		= register("seat", StructureTile.of(TilePredicate.Builder.create()
			.onFloor()
			.adjacent(Direction.Type.HORIZONTAL.stream().toList(), List.of(CDTiles.TABLE, CDTiles.TABLE_LIGHT))
			.build(), CDStructurePools.SEAT_KEY));
	
	protected static BiConsumer<BlockPos,ServerWorld> noOp() { return (a,b) -> {}; }
	
	protected static BiConsumer<BlockPos,ServerWorld> ofBlocks(BlockState... states)
	{
		final int sc = Tile.TILE_SIZE - 1;
		return (a,b) -> 
			BlockPos.Mutable.iterate(a, a.add(sc, sc, sc)).forEach(p -> Tile.tryPlace(states[b.random.nextInt(states.length)], p, b));
	}
	
	private static Supplier<Tile> register(String name, Function<Identifier,Tile> funcIn)
	{
		final Identifier id = prefix(name);
		Supplier<Tile> sup = () -> funcIn.apply(id);
		TERMS.put(id, sup);
		tally++;
		return sup;
	}
	
	public static Optional<Tile> get(String name) { return get(name.contains(":") ? Identifier.of(name) : prefix(name)); }
	
	public static Optional<Tile> get(Identifier id) { return TERMS.containsKey(id) ? Optional.of(TERMS.get(id).get()) : Optional.empty(); }
	
	public static void init()
	{
		CyclicDungeons.LOGGER.info("# Initialised {} grammar terms ({} placeable)", TERMS.size(), tally);
	}
}
