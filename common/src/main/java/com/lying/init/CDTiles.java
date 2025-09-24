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
import java.util.stream.Stream;

import com.google.common.collect.Lists;
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
	
	public static final Supplier<Tile> BLANK	= register("blank", Tile.of((a,b)->false, (a,b) -> {}));
	
	public static final Supplier<Tile> AIR		= register("air", Tile.of((a,b)->true, (a,b) -> {}));
	public static final Supplier<Tile> WALL		= register("wall", Tile.of(boundary(List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)), ofBlocks(
			Blocks.DEEPSLATE_BRICKS.getDefaultState(),
			Blocks.CRACKED_DEEPSLATE_BRICKS.getDefaultState(),
			Blocks.DEEPSLATE_TILES.getDefaultState(),
			Blocks.CRACKED_DEEPSLATE_TILES.getDefaultState())));
	public static final Supplier<Tile> PASSAGE	= register("passage", Tile.of(boundary(Direction.Type.HORIZONTAL), ofBlocks(Blocks.IRON_BARS.getDefaultState())));
	public static final Supplier<Tile> FLOOR	= register("floor", Tile.of(boundary(List.of(Direction.DOWN)), ofBlocks(Blocks.SMOOTH_STONE.getDefaultState())));
	public static final Supplier<Tile> LIGHT	= register("lamp", Tile.of(adjacent(List.of(Direction.DOWN), List.of(CDTiles.FLOOR.get())), ofBlocks(Blocks.LANTERN.getDefaultState())));
	
	protected static BiConsumer<BlockPos,ServerWorld> ofBlocks(BlockState... states)
	{
		return (a,b) -> b.setBlockState(a, states[b.random.nextInt(states.length)]);
	}
	
	protected static BiPredicate<BlockPos,TileSet> boundary(Direction.Type faces)
	{
		return (a,b)-> faces.stream().anyMatch(d -> b.isBoundary(a, d));
	}
	
	protected static BiPredicate<BlockPos,TileSet> boundary(List<Direction> faces)
	{
		return (a,b)-> faces.stream().anyMatch(d -> b.isBoundary(a, d));
	}
	
	protected static BiPredicate<BlockPos,TileSet> adjacent(List<Direction> faces, List<Tile> tiles)
	{
		return (a,b) -> faces.stream().anyMatch(d -> tiles.stream().anyMatch(t -> b.get(a.offset(d)).equals(t)));
	}
	
	protected static BiPredicate<BlockPos,TileSet> adjacent(Stream<Direction> faces, Tile... tiles)
	{
		List<Tile> tileSet = Lists.newArrayList();
		for(Tile t : tiles)
			tileSet.add(t);
		return (a,b) -> faces.anyMatch(d -> tileSet.stream().anyMatch(t -> b.get(a.offset(d)).equals(t)));
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
