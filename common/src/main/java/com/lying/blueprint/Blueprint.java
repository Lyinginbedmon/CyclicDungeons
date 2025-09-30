package com.lying.blueprint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.lying.grammar.GrammarPhrase;
import com.lying.grammar.GrammarRoom;
import com.lying.grammar.GrammarTerm;
import com.lying.grammar.RoomMetadata;
import com.lying.init.CDTerms;
import com.lying.reference.Reference;
import com.lying.utility.AbstractBox2f;
import com.lying.worldgen.Tile;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

@SuppressWarnings("serial")
public class Blueprint extends ArrayList<BlueprintRoom>
{
	public static final Logger LOGGER = LoggerFactory.getLogger(Reference.ModInfo.MOD_ID+"_worldgen");
	private static final int ROOM_HEIGHT = Tile.TILE_SIZE * 4;
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
		for(ErrorType type : ErrorType.values())
			if(tallyErrors(chart, type) > 0)
				return true;
		return false;
	}
	
	public static int tallyErrors(List<BlueprintRoom> chart, ErrorType type)
	{
		int tally = 0;
		List<BlueprintPassage> paths = getPassages(chart);
		
		switch(type)
		{
			case COLLISION:
				LOGGER.warn("# Checking for room collision errors");
				for(BlueprintRoom room : chart)
				{
					AbstractBox2f bounds = room.bounds();
					LOGGER.info(" # Comparing {}", bounds.toString());
					if(chart.stream().filter(r -> !r.equals(room)).map(b -> b.bounds()).anyMatch(bounds::intersects))
						++tally;
				}
				return tally;
			case TUNNEL:
				for(BlueprintPassage path : paths)
					if(path.hasTunnels(chart))
						++tally;
				return tally;
			case INTERSECTION:
				for(BlueprintPassage path : paths)
					if(path.hasIntersections(chart))
						++tally;
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
		
		buildExteriorShell(position, world);
		
//		buildRooms(position, world);
		
		buildExteriorPaths(position, world);
		LOGGER.info(" # Blueprint generation completed, {}ms total", System.currentTimeMillis() - timeMillis);
		return true;
	}
	
	public void buildExteriorShell(BlockPos position, ServerWorld world)
	{
		long timeMillis = System.currentTimeMillis();
		LOGGER.info(" # Generating exterior shell");
		
		// Collect all bounding boxes as positions
		List<AbstractBox2f> bounds = stream().map(BlueprintRoom::bounds).toList();
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
		final List<BlueprintPassage> passages = getPassages(this);
		int tally = 0;
		for(BlueprintRoom node : this)
		{
			Vector2i nodePos = node.position();
			BlockPos pos = position.add(nodePos.x, 0, nodePos.y);
			Vector2i scale = node.metadata().size();
			
			BlockPos min = pos.add(-scale.x / 2, 0, -scale.y / 2);
			BlockPos max = min.add(scale.x, ROOM_HEIGHT, scale.y);
			
			RoomMetadata meta = node.metadata();
			GrammarTerm type = meta.type();
			LOGGER.info(" # Room {}: {}x{} {}", tally++, meta.size().x(), meta.size().y(), type.registryName().getPath());
			type.generate(min, max, world, node, this, passages);
		};
	}
	
	public void buildExteriorPaths(BlockPos position, ServerWorld world)
	{
		long timeMillis = System.currentTimeMillis();
		LOGGER.info(" # Generating exterior passages");
		List<AbstractBox2f> bounds = stream().map(BlueprintRoom::bounds).toList();
		getPassages(this).forEach(p -> p.build(position, world, bounds));
		LOGGER.info(" ## Passages completed in {}ms", System.currentTimeMillis() - timeMillis);
	}
	
	public static void tryPlaceAt(BlockState state, BlockPos pos, ServerWorld world)
	{
		BlockState stateAt = world.getBlockState(pos);
		if(!stateAt.isIn(BlockTags.WITHER_IMMUNE))
			world.setBlockState(pos, state);
	}
	
	/** Returns a list of all paths between the given nodes */
	public static List<BlueprintPassage> getPassages(Collection<BlueprintRoom> chart)
	{
		List<BlueprintPassage> paths = Lists.newArrayList();
		for(BlueprintRoom n : chart)
			n.getChildren(chart).stream().map(c -> new BlueprintPassage(n, c)).forEach(paths::add);
		return paths;
	}
	
	public static List<BlueprintPassage> getPassagesOf(BlueprintRoom room, Collection<BlueprintRoom> chart)
	{
		return getPassages(chart).stream().filter(p -> p.isTerminus(room)).toList();
	}
	
	public static enum ErrorType
	{
		/** Rooms that share space with other rooms */
		COLLISION,
		/** Passages that intersect other passages */
		INTERSECTION,
		/** Passages that pass through unrelated rooms */
		TUNNEL;
	}
}
