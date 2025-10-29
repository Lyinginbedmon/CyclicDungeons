package com.lying.blueprint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Lists;
import com.lying.grammar.GrammarPhrase;
import com.lying.grammar.GrammarRoom;
import com.lying.grammar.GrammarTerm;
import com.lying.grammar.RoomMetadata;
import com.lying.grid.GridTile;
import com.lying.init.CDLoggers;
import com.lying.init.CDTerms;
import com.lying.utility.AbstractBox2f;
import com.lying.utility.DebugLogger;
import com.lying.worldgen.Tile;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

@SuppressWarnings("serial")
public class Blueprint extends ArrayList<BlueprintRoom>
{
	public static final DebugLogger LOGGER = CDLoggers.WORLDGEN;
	
	public static final int ROOM_TILE_HEIGHT	= 4;
	public static final int ROOM_HEIGHT			= ROOM_TILE_HEIGHT * Tile.TILE_SIZE;
	protected int maxDepth = 0;
	protected Map<Integer, List<BlueprintRoom>> byDepth = new HashMap<>();
	private List<BlueprintRoom> criticalPath = Lists.newArrayList();
	
	public static Blueprint fromGraph(GrammarPhrase graphIn)
	{
		Blueprint graph = new Blueprint();
		graphIn.getStart().ifPresent(r -> addNodeToBlueprint(r, null, graph, graphIn));
		return graph;
	}
	
	private static BlueprintRoom addNodeToBlueprint(GrammarRoom room, @Nullable BlueprintRoom parent, Blueprint graph, GrammarPhrase graphIn)
	{
		BlueprintRoom node = new BlueprintRoom(room.uuid(), room.metadata(), room.getChildLinks(), room.getParentLinks());
		graph.add(node);
		if(room.hasLinks())
			room.getChildRooms(graphIn).forEach(r -> addNodeToBlueprint(r, parent, graph, graphIn));
		return node;
	}
	
	public Blueprint clone()
	{
		Blueprint clone = new Blueprint();
		stream().map(BlueprintRoom::clone).forEach(clone::add);
		return clone;
	}
	
	public Optional<BlueprintRoom> start() { return stream().filter(n -> n.metadata().is(CDTerms.START.get())).findFirst(); }
	
	public Optional<BlueprintRoom> end() { return stream().filter(n -> n.metadata().is(CDTerms.END.get())).findFirst(); }
	
	public boolean add(BlueprintRoom node)
	{
		boolean result = super.add(node);
		if(result)
		{
			// Update the depth range
			maxDepth = 0;
			for(BlueprintRoom n : this)
				if(n.metadata().depth() > maxDepth)
					maxDepth = n.metadata().depth();
			
			// Update the depth map
			byDepth.clear();
			for(int i=0; i<=maxDepth; i++)
			{
				final int depth = i;
				byDepth.put(i, stream().filter(n -> n.metadata().depth() == depth).toList());
			}
		}
		return result;
	}
	
	public Optional<BlueprintRoom> getRoom(UUID id) { return stream().filter(r -> r.uuid().equals(id)).findFirst(); }
	
	/** Returns the deepest level of this dungeon */
	public int maxDepth() { return maxDepth; }
	
	public void updateCriticalPath()
	{
		criticalPath = BlueprintPather.calculateCriticalPath(this);
	}
	
	/** Returns a list of nodes representing the path from the start to the end of this dungeon */
	public List<BlueprintRoom> getCriticalPath() { return this.criticalPath; }
	
	@NotNull
	public List<BlueprintRoom> byDepth(int depth) { return byDepth.getOrDefault(depth, Lists.newArrayList()); }
	
	/** Returns true if this blueprint contains any errors that may interfere with generation */
	public boolean hasErrors() { return hasErrors(this); }
	
	/** Returns true if the set of rooms contains any errors that may interfere with generation */
	public static boolean hasErrors(List<BlueprintRoom> chart)
	{
		return ErrorType.stream().anyMatch(e -> anyErrors(chart, e));
	}
	
	public static boolean anyErrors(List<BlueprintRoom> chart, ErrorType type)
	{
		List<BlueprintPassage> paths = BlueprintOrganiser.getFinalisedPassages(chart);
		switch(type)
		{
			case COLLISION:
				for(BlueprintRoom room : chart)
					if(chart.stream().filter(r -> !r.equals(room)).anyMatch(room::intersects))
						return true;
				break;
			case TUNNEL:
				for(BlueprintRoom room : chart)
				{
					List<GridTile> roomTiles = room.tiles();
					List<BlueprintPassage> passages = paths.stream().filter(p -> !p.isTerminus(room)).toList();
					if(passages.stream().anyMatch(p -> 
					{
						List<GridTile> pathTiles = p.tiles();
						return pathTiles.stream().anyMatch(t -> roomTiles.stream().anyMatch(t::isAdjacentTo));
					}))
						return true;
				}
				break;
			case INTERSECTION:
				for(BlueprintPassage path : paths)
				{
					path.exclude(path.parent().tileBounds());
					path.children().stream().map(BlueprintRoom::tileBounds).forEach(path::exclude);
					
					if(path.intersectsOtherPassages(chart))
						return true;
				}
				break;
		}
		
		return false;
	}
	
	public static int tallyErrors(List<BlueprintRoom> chart, ErrorType type)
	{
		int tally = 0;
		List<BlueprintPassage> paths = BlueprintOrganiser.getFinalisedPassages(chart);
		switch(type)
		{
			case COLLISION:
				for(BlueprintRoom room : chart)
					if(chart.stream().filter(r -> !r.equals(room)).anyMatch(room::intersects))
						++tally;
				return tally;
			case TUNNEL:
				for(BlueprintRoom room : chart)
				{
					List<GridTile> roomTiles = room.tiles();
					List<BlueprintPassage> passages = paths.stream().filter(p -> !p.isTerminus(room)).toList();
					if(passages.stream().anyMatch(p -> 
					{
						List<GridTile> pathTiles = p.tiles();
						return pathTiles.stream().anyMatch(t -> roomTiles.stream().anyMatch(t::isAdjacentTo));
					}))
						tally++;
				}
				return tally;
			case INTERSECTION:
				for(BlueprintPassage path : paths)
				{
					path.exclude(path.parent().tileBounds());
					path.children().stream().map(BlueprintRoom::tileBounds).forEach(path::exclude);
					
					if(path.intersectsOtherPassages(chart))
						++tally;
				}
				return tally;
		}
		
		return 0;
	}
	
	public boolean build(BlockPos position, ServerWorld world)
	{
		if(isEmpty() || hasErrors())
			return false;
		
		long timeMillis = System.currentTimeMillis();
		LOGGER.info(" # Beginning blueprint generation");
		
//		buildExteriorShell(position, world);	FIXME Update exterior shell generation to reflect positioning changes
		
		buildExteriorPaths(position, world);
		
		buildRooms(position, world);
		
		LOGGER.info(" # Blueprint generation completed, {}ms total", System.currentTimeMillis() - timeMillis);
		return true;
	}
	
	public void buildExteriorShell(BlockPos position, ServerWorld world)
	{
		long timeMillis = System.currentTimeMillis();
		LOGGER.info(" # Generating exterior shell");
		
		// Collect all bounding boxes as positions
		List<AbstractBox2f> bounds = BlueprintOrganiser.getBounds(this);
		List<BlockPos> interior = Lists.newArrayList();
		bounds.stream().forEach(b -> 
		{
			BlockPos start = position.add((int)b.minX(), 0, (int)b.minY());
			BlockPos end = position.add((int)b.maxX(), ROOM_HEIGHT, (int)b.maxY());
			BlockPos.Mutable.iterate(start, end.add(-1, -1, -1)).forEach(p -> interior.add(p.toImmutable()));
		});
		
		// Expand the bounding boxes 1 block in all directions and collect the new positions
		// Exclude any positions that are in the interior set
		final Predicate<BlockPos> isExterior = p -> !interior.contains(p);
		List<BlockPos> points = Lists.newArrayList();
		bounds.stream().forEach(b -> 
		{
			BlockPos start = position.add((int)b.minX() - 1, -1, (int)b.minY() - 1);
			BlockPos end = position.add((int)b.maxX(), ROOM_HEIGHT, (int)b.maxY());
			BlockPos.Mutable.iterate(start, end).forEach(p -> 
			{
				if(isExterior.test(p))
					points.add(p.toImmutable());
			});
		});
		
		// Generate wall tile at all remaining positions
		final BlockState[] states = new BlockState[] 
				{
					Blocks.DEEPSLATE_BRICKS.getDefaultState(),
					Blocks.CRACKED_DEEPSLATE_BRICKS.getDefaultState(),
					Blocks.DEEPSLATE_TILES.getDefaultState(),
					Blocks.CRACKED_DEEPSLATE_TILES.getDefaultState()
				};
		points.forEach(p -> Tile.tryPlace(states[world.random.nextInt(states.length)], p, world));
		
		LOGGER.info(" ## Exterior shell completed in {}ms", System.currentTimeMillis() - timeMillis);
	}
	
	public void buildRooms(BlockPos position, ServerWorld world)
	{
		long timeMillis = System.currentTimeMillis();
		LOGGER.info(" # Generating rooms");
		
		final List<BlueprintPassage> passages = BlueprintOrganiser.getFinalisedPassages(this);
		int tally = 0;
		for(BlueprintRoom node : this)
		{
			RoomMetadata meta = node.metadata();
			GrammarTerm type = meta.type();
			LOGGER.info(" # Room {} of {}: {}x{} {}", ++tally, size(), meta.size().x(), meta.size().y(), type.registryName().getPath());
			if(type.generate(position, world, node, passages))
				LOGGER.info(" ## Finished");
			else
				LOGGER.error(" ## Error during room generation");
		};
		
		LOGGER.info(" ## Rooms completed in {}ms", System.currentTimeMillis() - timeMillis);
	}
	
	public void buildExteriorPaths(BlockPos position, ServerWorld world)
	{
		long timeMillis = System.currentTimeMillis();
		LOGGER.info(" # Generating exterior passages");
		
		BlueprintOrganiser.getFinalisedPassages(this)
			.forEach(p -> p.generate(position, world));
		
		LOGGER.info(" ## Passages completed in {}ms", System.currentTimeMillis() - timeMillis);
	}
	
	public static void tryPlaceAt(BlockState state, BlockPos pos, ServerWorld world)
	{
		BlockState stateAt = world.getBlockState(pos);
		if(!stateAt.isIn(BlockTags.WITHER_IMMUNE))
			world.setBlockState(pos, state);
	}
	
	public static List<BlueprintPassage> getPassagesOf(BlueprintRoom room, Collection<BlueprintRoom> chart)
	{
		return BlueprintOrganiser.getPassages(chart).stream().filter(p -> p.isTerminus(room)).toList();
	}
	
	public static enum ErrorType
	{
		/** Rooms that share space with other rooms */
		COLLISION,
		/** Passages that intersect other passages */
		INTERSECTION,
		/** Passages that pass through unrelated rooms */
		TUNNEL;
		
		public static Stream<ErrorType> stream() { return List.of(values()).stream(); }
	}
}
