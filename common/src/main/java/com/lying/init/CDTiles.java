package com.lying.init;

import static com.lying.reference.Reference.ModInfo.prefix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

import com.lying.CyclicDungeons;
import com.lying.worldgen.Tile;
import com.lying.worldgen.TileSet;

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
	
	public static final Supplier<Tile> BLANK	= register("blank", Tile.of(never(), noOp()));
	
	public static final Supplier<Tile> AIR		= register("air", Tile.of(always(), ofBlocks(Blocks.AIR.getDefaultState())));
	public static final Supplier<Tile> PASSAGE	= register("passage", Tile.of(boundary(Direction.Type.HORIZONTAL), ofBlocks(Blocks.IRON_BARS.getDefaultState())));
	
	public static final Supplier<Tile> FLOOR	= register("floor", Tile.of(boundary(List.of(Direction.DOWN)), ofBlocks(Blocks.SMOOTH_STONE.getDefaultState())));
	public static final Supplier<Tile> LIGHT	= register("lamp", Tile.of(onFloor(), ofBlocks(Blocks.LANTERN.getDefaultState())));
	public static final Supplier<Tile> TABLE	= register("table", Tile.of(onFloor(), ofBlocks(Blocks.OAK_STAIRS.getDefaultState())));
	public static final Supplier<Tile> SEAT		= register("seat", Tile.of(onFloor(), ofBlocks(Blocks.OAK_SLAB.getDefaultState())));
	
	protected static BiPredicate<BlockPos,TileSet> always() { return (a,b) -> true; }
	protected static BiPredicate<BlockPos,TileSet> never() { return (a,b) -> false; }
	
	protected static BiPredicate<BlockPos,TileSet> nonBoundary() { return (a,b) -> Direction.stream().noneMatch(d -> b.isBoundary(a, d)); }
	
	protected static BiPredicate<BlockPos,TileSet> boundary(Direction.Type faces)
	{
		return (a,b)-> faces.stream().anyMatch(d -> b.isBoundary(a, d));
	}
	
	protected static BiPredicate<BlockPos,TileSet> boundary(List<Direction> faces)
	{
		return (a,b)-> faces.stream().anyMatch(d -> b.isBoundary(a, d));
	}
	
	protected static BiPredicate<BlockPos,TileSet> adjacent(List<Direction> faces, List<Supplier<Tile>> tiles)
	{
		return (a,b) -> faces.stream().anyMatch(d -> b.contains(a.offset(d)) && tiles.stream().map(Supplier::get).anyMatch(t -> b.get(a.offset(d)).equals(t)));
	}
	
	protected static BiPredicate<BlockPos,TileSet> onFloor()
	{
		return adjacent(List.of(Direction.DOWN), List.of(CDTiles.FLOOR));
	}
	
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
	
	public static List<Tile> getUseableTiles() { return TERMS.values().stream().map(Supplier::get).filter(t -> !t.isBlank()).toList(); }
	
	public static void init()
	{
		CyclicDungeons.LOGGER.info("# Initialised {} grammar terms ({} placeable)", TERMS.size(), tally);
	}
}
